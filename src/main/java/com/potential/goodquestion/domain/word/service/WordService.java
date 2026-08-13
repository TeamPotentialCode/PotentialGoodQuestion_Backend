package com.potential.goodquestion.domain.word.service;

import com.potential.goodquestion.common.code.AuthErrorCode;
import com.potential.goodquestion.common.code.ChildErrorCode;
import com.potential.goodquestion.common.code.WordErrorCode;
import com.potential.goodquestion.common.exception.CustomException;
import com.potential.goodquestion.common.openai.WordAnalysisClient;
import com.potential.goodquestion.common.openai.dto.WordAnalysisRequest;
import com.potential.goodquestion.common.openai.dto.WordAnalysisResponse;
import com.potential.goodquestion.domain.child.entity.Child;
import com.potential.goodquestion.domain.child.repository.ChildRepository;
import com.potential.goodquestion.domain.parent.repository.ParentRepository;
import com.potential.goodquestion.domain.word.dto.WordRequestDto;
import com.potential.goodquestion.domain.word.dto.WordResponseDto;
import com.potential.goodquestion.domain.word.entity.Word;
import com.potential.goodquestion.domain.word.repository.WordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 단어장 서비스
 *
 * 담당 API:
 * - POST   /api/children/{childId}/words          : 단어 저장 (GPT 뜻·예시 문장 자동 생성)
 * - GET    /api/children/{childId}/words          : 단어장 전체 조회
 * - PATCH  /api/words/{wordId}/favorite           : 즐겨찾기 토글
 * - DELETE /api/words/{wordId}                    : 단어 삭제
 *
 * 보안:
 * - 아이 ID로 접근 시 보호자 소유권 검증
 * - 단어 ID로 접근 시 단어 소유 아이의 보호자 검증
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;
    private final ChildRepository childRepository;
    private final ParentRepository parentRepository;
    private final WordAnalysisClient wordAnalysisClient;

    /**
     * 단어 저장
     * GPT를 통해 아이 눈높이 뜻과 예시 문장을 자동 생성한 뒤 저장한다.
     * 동일 아이가 같은 단어를 이미 저장한 경우 409 반환.
     *
     * @param childId  아이 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @param request  단어, 원문 문장, 저장 출처
     * @return 저장된 단어 정보 (GPT 생성 뜻·예시 문장 포함)
     */
    @Transactional
    public WordResponseDto.WordInfo saveWord(Long childId, Long parentId, WordRequestDto.Save request) {
        Child child = getChildWithOwnerCheck(parentId, childId);

        // 중복 저장 방지
        if (wordRepository.existsByChildIdAndWord(childId, request.getWord())) {
            throw new CustomException(WordErrorCode.WORD_ALREADY_EXISTS);
        }

        // GPT로 아이 눈높이 뜻·예시 문장 생성
        WordAnalysisResponse analysis = generateWordAnalysis(request.getWord(),
                request.getContextSentence(), child.calculateAge());

        Word word = Word.create(
                child,
                request.getWord(),
                request.getContextSentence(),
                analysis.meaning(),
                analysis.exampleSentence(),
                request.getSource()
        );
        return WordResponseDto.WordInfo.from(wordRepository.save(word));
    }

    /**
     * 단어장 전체 조회 (최근 저장 순)
     *
     * @param childId  아이 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @return 단어 목록 + 전체 수 + 즐겨찾기 수
     */
    public WordResponseDto.WordList getWords(Long childId, Long parentId) {
        getChildWithOwnerCheck(parentId, childId);
        List<Word> words = wordRepository.findByChildIdOrderByCreatedAtDesc(childId);
        return WordResponseDto.WordList.from(words);
    }

    /**
     * 단어장 단건 조회
     * 단어 카드 화면 진입 시 사용한다.
     *
     * @param wordId 단어 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자ID (소유권 검증)
     * @return 단어 상세 정보 (뜻, 예시 문장, 즐겨찾기 포함)
     */
    public WordResponseDto.WordInfo getOneWord(Long wordId, Long parentId) {
        Word word = getWordWithOwnerCheck(parentId, wordId);
        return WordResponseDto.WordInfo.from(word);
    }

    /**
     * 즐겨찾기 토글
     * 즐겨찾기 상태를 on/off 전환한다.
     *
     * @param wordId   단어 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @return 변경된 단어 정보
     */
    @Transactional
    public WordResponseDto.WordInfo toggleFavorite(Long wordId, Long parentId) {
        Word word = getWordWithOwnerCheck(parentId, wordId);
        word.toggleFavorite();
        return WordResponseDto.WordInfo.from(word);
    }

    /**
     * 학습 완료 상태 토글
     * 단어 카드에서 완료 버튼을 누를 때 사용한다.
     *
     * @param wordId   단어 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     * @return 변경된 단어 정보
     */
    @Transactional
    public WordResponseDto.WordInfo toggleLearned(Long wordId, Long parentId) {
        Word word = getWordWithOwnerCheck(parentId, wordId);
        word.toggleLearned();
        return WordResponseDto.WordInfo.from(word);
    }

    /**
     * 단어 삭제
     *
     * @param wordId   단어 ID (URL PathVariable)
     * @param parentId JWT에서 추출한 보호자 ID (소유권 검증)
     */
    @Transactional
    public void deleteWord(Long wordId, Long parentId) {
        Word word = getWordWithOwnerCheck(parentId, wordId);
        wordRepository.delete(word);
    }

    // ─────────── private ────────────────

    /**
     * GPT 단어 분석 호출
     * 실패 시 WordErrorCode.WORD_ANALYSIS_FAILED 예외 발생
     */
    private WordAnalysisResponse generateWordAnalysis(String word, String contextSentence, int childAge) {
        try {
            return wordAnalysisClient.analyze(new WordAnalysisRequest(word, contextSentence, childAge));
        } catch (Exception e) {
            throw new CustomException(WordErrorCode.WORD_ANALYSIS_FAILED);
        }
    }

    /**
     * 아이 조회 + 보호자 소유권 검증
     */
    private Child getChildWithOwnerCheck(Long parentId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new CustomException(ChildErrorCode.CHILD_NOT_FOUND));

        // 로그인한 보호자의 아이가 아니면 403
        if (!child.getParent().getId().equals(parentId)) {
            throw new CustomException(ChildErrorCode.CHILD_ACCESS_DENIED);
        }
        return child;
    }

    /**
     * 단어 조회 + 보호자 소유권 검증
     * 단어 → 아이 → 보호자 경로로 소유권 확인
     */
    private Word getWordWithOwnerCheck(Long parentId, Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new CustomException(WordErrorCode.WORD_NOT_FOUND));

        // 단어를 저장한 아이의 보호자가 아니면 403
        if (!word.getChild().getParent().getId().equals(parentId)) {
            throw new CustomException(WordErrorCode.WORD_ACCESS_DENIED);
        }
        return word;
    }
}
