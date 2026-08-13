package com.potential.goodquestion.domain.word.controller;

import com.potential.goodquestion.common.response.ApiResponse;
import com.potential.goodquestion.common.security.CustomUserPrincipal;
import com.potential.goodquestion.domain.word.dto.WordRequestDto;
import com.potential.goodquestion.domain.word.dto.WordResponseDto;
import com.potential.goodquestion.domain.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Word", description = "단어장 API")
@RestController
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @Operation(
            summary = "단어 저장",
            description = "단어를 저장합니다. GPT가 아이 눈높이 뜻과 예시 문장을 자동으로 생성합니다. "
                    + "아이가 대화 중 직접 저장(CHILD)하거나 보호자가 리포트에서 저장(PARENT)할 수 있습니다."
    )
    @PostMapping("/api/children/{childId}/words")
    public ResponseEntity<ApiResponse<WordResponseDto.WordInfo>> saveWord(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId,
            @Valid @RequestBody WordRequestDto.Save request) {
        WordResponseDto.WordInfo word = wordService.saveWord(childId, principal.getParentId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("단어가 저장되었습니다.", word));
    }

    @Operation(
            summary = "단어장 조회",
            description = "아이의 단어장 전체 목록을 최근 저장 순으로 반환합니다. "
                    + "전체 단어 수와 즐겨찾기 수를 함께 제공합니다."
    )
    @GetMapping("/api/children/{childId}/words")
    public ResponseEntity<ApiResponse<WordResponseDto.WordList>> getWords(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "아이 ID") @PathVariable Long childId) {
        WordResponseDto.WordList words = wordService.getWords(childId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("단어장을 조회했습니다.", words));
    }

    @Operation(
        summary = "단어 단건 조회",
        description = "단어 ID로 단어 카드 상세 정보를 조회합니다. "
                + "단어, 뜻, 예시 문장, 즐겨찾기 여부를 반환합니다."
    )
    @GetMapping("/api/children/{childId}/words/{wordId}")
    public ResponseEntity<ApiResponse<WordResponseDto.WordInfo>> getOneWord(
        @AuthenticationPrincipal CustomUserPrincipal principal,
        @Parameter(description = "아이 Id") @PathVariable Long childId,
        @Parameter(description = "단어 Id") @PathVariable Long wordId
    ) {
        WordResponseDto.WordInfo word = wordService.getOneWord(wordId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("단어를 조회했습니다.", word));
    }

    @Operation(
            summary = "즐겨찾기 토글",
            description = "단어의 즐겨찾기 상태를 on/off 전환합니다."
    )
    @PatchMapping("/api/words/{wordId}/favorite")
    public ResponseEntity<ApiResponse<WordResponseDto.WordInfo>> toggleFavorite(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "단어 ID") @PathVariable Long wordId) {
        WordResponseDto.WordInfo word = wordService.toggleFavorite(wordId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("즐겨찾기가 변경되었습니다.", word));
    }

    @Operation(
            summary = "학습 완료 토글",
            description = "단어 카드의 학습 완료 상태를 on/off 전환합니다. "
                    + "기본값은 false(미완료)이며, 완료 처리하면 true로 변경됩니다."
    )
    @PatchMapping("/api/words/{wordId}/learned")
    public ResponseEntity<ApiResponse<WordResponseDto.WordInfo>> toggleLearned(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "단어 ID") @PathVariable Long wordId) {
        WordResponseDto.WordInfo word = wordService.toggleLearned(wordId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("학습 완료 상태가 변경되었습니다.", word));
    }

    @Operation(
            summary = "단어 삭제",
            description = "단어장에서 단어를 삭제합니다."
    )
    @DeleteMapping("/api/words/{wordId}")
    public ResponseEntity<ApiResponse<Void>> deleteWord(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "단어 ID") @PathVariable Long wordId) {
        wordService.deleteWord(wordId, principal.getParentId());
        return ResponseEntity.ok(ApiResponse.success("단어가 삭제되었습니다.", null));
    }

}
