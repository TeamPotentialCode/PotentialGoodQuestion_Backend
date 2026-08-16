package com.potential.goodquestion.domain.parent.service;

import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.domain.activity.repository.ActivityRepository;
import com.potential.goodquestion.domain.auth.entity.Auth;
import com.potential.goodquestion.domain.auth.repository.AuthRepository;
import com.potential.goodquestion.domain.child.repository.ChildConsentRepository;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.message.repository.MessageRepository;
import com.potential.goodquestion.domain.parent.dto.ParentRequestDto;
import com.potential.goodquestion.domain.parent.dto.ParentResponseDto;
import com.potential.goodquestion.domain.parent.entity.Parent;
import com.potential.goodquestion.domain.parent.enums.OAuthProvider;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import com.potential.goodquestion.domain.storysession.repository.StorySessionRepository;
import com.potential.goodquestion.domain.utterance.repository.UtteranceAnalysisRepository;
import com.potential.goodquestion.domain.word.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 보호자 서비스
 *
 * 담당 API:
 * - GET    /api/parent/me : 내 정보 조회
 * - PATCH  /api/parent/me : 내 정보 수정
 * - DELETE /api/parent/me : 회원 탈퇴 (계정 및 연관 데이터 삭제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParentService {

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    // TODO: 카카오 키 발급 후 아래 주석 해제 + application.yaml 카카오 설정 주석 해제
    // @Value("${kakao.admin-key}")
    // private String kakaoAdminKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ParentRepository parentRepository;
    private final AuthRepository authRepository;
    private final ChildRepository childRepository;
    private final ChildConsentRepository childConsentRepository;
    private final StorySessionRepository storySessionRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository utteranceAnalysisRepository;
    private final ActivityRepository activityRepository;
    private final WordRepository wordRepository;

    public ParentResponseDto.Me getMe(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        return ParentResponseDto.Me.from(parent);
    }

    @Transactional
    public ParentResponseDto.Me updateMe(Long parentId, ParentRequestDto.Update request) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));
        parent.updateName(request.getName());
        return ParentResponseDto.Me.from(parent);
    }

    /**
     * 회원 탈퇴 (계정 및 연관 데이터 삭제)
     *
     * 소프트 삭제(플래그 컬럼) 체계가 없는 프로젝트이므로 기존 삭제 구현
     * (WordService.deleteWord, AdminService.deleteScene)과 동일하게 하드 삭제한다.
     *
     * 보호자에 연결된 데이터를 외래키 역순으로 함께 삭제한다.
     *   utterance_analyses -> messages -> post_activity_results -> story_sessions
     *   -> word_books -> child_consents -> children -> auth_tokens -> parents
     * 엔티티에 cascade 설정이 없어 이 순서를 지키지 않으면 외래키 제약 위반이 발생한다.
     *
     * 탈퇴 후에는 저장된 리프레시 토큰이 삭제되므로 토큰 재발급이 불가능하다.
     * 다만 이미 발급된 액세스 토큰은 만료 전까지 유효하다.
     *
     * @param parentId JWT에서 추출한 보호자 ID
     */
    @Transactional
    public void withdraw(Long parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.PARENT_NOT_FOUND));

        // 소셜 계정이면 provider 연결 해제
        if (parent.getProvider() != OAuthProvider.LOCAL) {
            authRepository.findByParentId(parentId)
                    .ifPresent(auth -> unlinkSocialAccount(parent.getProvider(), auth));
        }

        utteranceAnalysisRepository.deleteByParentId(parentId);
        messageRepository.deleteByParentId(parentId);
        activityRepository.deleteByParentId(parentId);
        storySessionRepository.deleteByParentId(parentId);
        wordRepository.deleteByParentId(parentId);
        childConsentRepository.deleteByParentId(parentId);
        childRepository.deleteByParentId(parentId);
        authRepository.deleteByParentId(parentId);

        parentRepository.delete(parent);
    }

    /**
     * 소셜 provider 연결 해제
     * 실패해도 탈퇴 자체는 진행 — 계정 삭제가 우선이고 unlink 실패는 provider 측 문제일 수 있음
     */
    private void unlinkSocialAccount(OAuthProvider provider, Auth auth) {
        String token = auth.getOauthAccessToken();
        if (token == null) {
            log.warn("소셜 unlink 생략 — oauth_access_token 없음: provider={}", provider);
            return;
        }
        try {
            if (provider == OAuthProvider.GOOGLE) {
                unlinkGoogle(token);
            } else if (provider == OAuthProvider.NAVER) {
                unlinkNaver(token);
            }
            // TODO: 카카오 키 발급 후 아래 주석 해제
            // else if (provider == OAuthProvider.KAKAO) {
            //     unlinkKakao(token);
            // }
        } catch (Exception e) {
            // unlink 실패가 탈퇴를 막으면 안 됨
            log.warn("소셜 unlink 실패 (탈퇴는 계속 진행): provider={}, error={}", provider, e.getMessage());
        }
    }

    private void unlinkGoogle(String accessToken) {
        restTemplate.postForObject(
                "https://oauth2.googleapis.com/revoke?token=" + accessToken,
                HttpEntity.EMPTY, String.class);
        log.info("Google unlink 완료");
    }

    // TODO: 카카오 키 발급 후 아래 주석 해제
    // private void unlinkKakao(String accessToken) {
    //     HttpHeaders headers = new HttpHeaders();
    //     headers.set("Authorization", "Bearer " + accessToken);
    //     restTemplate.postForObject(
    //             "https://kapi.kakao.com/v1/user/unlink",
    //             new HttpEntity<>(headers), String.class);
    //     log.info("Kakao unlink 완료");
    // }

    private void unlinkNaver(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "delete");
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("access_token", accessToken);
        params.add("service_provider", "NAVER");

        restTemplate.postForObject(
                "https://nid.naver.com/oauth2.0/token",
                new HttpEntity<>(params, headers), String.class);
        log.info("Naver unlink 완료");
    }
}
