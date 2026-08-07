# AI 대화 엔진 · 음성 연동 · 리포트 설계문서

> 담당자: 전우선
> 작성일: 2026-08-07
> 대상 독자: 백엔드 팀원, 프론트엔드 팀

---

## 1. 개요

### 담당 API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/sessions/:id/utterances` | 아이 발화 제출 → 분석 → 캐릭터 반응 |
| POST | `/speech/stt` | 음성 파일 → 텍스트 변환 |
| POST | `/speech/tts` | 텍스트 → 음성 파일 |
| GET | `/reports/:sessionId` | 보호자 리포트 조회 |

### 아키텍처 개요

```
POST /sessions/:id/utterances
        │
        ├─ 1. 아이 메시지 저장 (messages)
        ├─ 2. 발화 분석 LLM 호출 (gpt-5-mini)
        ├─ 3. 서버 후처리 (evidence 검증, 중복 제거)
        ├─ 4. 분석 결과 저장 (utterance_analyses)
        ├─ 5. 누적 요소 갱신 + 진행 모드 판정 (서버 규칙)
        ├─ 6a. CLOSING → 고정 마지막 대사 사용, LLM 미호출
        └─ 6b. NORMAL/GUIDED → 캐릭터 대사 생성 LLM 호출 (gpt-5-mini)
```

**LLM 호출 횟수:** 일반 턴 2회, CLOSING 턴 1회 (토큰 절감)

---

## 2. 관련 DB 테이블

### story_scenes (읽기 전용)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| story_id | BIGINT FK | |
| scene_order | INTEGER | 장면 순서 (1부터 시작) |
| scene_description | TEXT | 장면 상황 설명 (고정, 수정 불가) |
| conflict | TEXT | 갈등 요약 |
| character_name | VARCHAR(50) | 캐릭터 이름 |
| character_opening | TEXT | 고정 첫 대사 (수정 불가) |
| character_closing | TEXT | 고정 마지막 대사 (수정 불가) |
| scene_goal | TEXT | 장면 학습 목표 |
| required_elements | TEXT | 필수 사고 요소 JSON 배열 ex) `["REASON","PERSPECTIVE","SOLUTION"]` |
| element_criteria | TEXT | 요소별 인정 기준 JSON 객체 |
| remaining_worries | TEXT | 캐릭터 걱정 정보 JSON 객체 (유도 시 사용) |
| preferred_turns | INTEGER | 권장 최소 턴 수 |
| max_turns | INTEGER | 최대 허용 턴 수 |

### story_sessions (읽기/쓰기)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| child_id | BIGINT FK | |
| story_id | BIGINT FK | |
| current_scene_id | BIGINT FK | 현재 진행 중인 장면 |
| current_child_turn_count | INTEGER | 현재 장면 아이 발화 횟수 |
| accumulated_elements | TEXT | 누적 사고 요소 JSON 배열 |
| last_detected_elements | TEXT | 직전 턴 탐지 요소 JSON 배열 |
| last_response_mode | VARCHAR(20) | NORMAL / GUIDED / CLOSING |
| last_guidance_target | VARCHAR(30) | 직전 유도 대상 요소 |
| turns_without_new_element | INTEGER | 신규 요소 없는 연속 턴 수 |
| consecutive_low_information_turns | INTEGER | 저정보 발화 연속 횟수 |
| scene_goal_met | BOOLEAN | 장면 목표 달성 여부 |
| scene_end_reason | VARCHAR(20) | GOAL_MET / MAX_TURNS |
| status | VARCHAR(20) | IN_PROGRESS / COMPLETED |
| started_at | TIMESTAMP | |
| completed_at | TIMESTAMP | |
| last_activity_at | TIMESTAMP | |

### messages (읽기/쓰기)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| session_id | BIGINT FK | |
| scene_id | BIGINT FK | |
| speaker_type | VARCHAR(20) | CHILD / CHARACTER |
| text | TEXT | 대화 내용 |
| stt_raw_text | TEXT | STT 원본 텍스트 (CHILD만 해당, nullable) |
| created_at | TIMESTAMP | |

### utterance_analyses (쓰기)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| message_id | BIGINT FK (unique) | 1:1 매핑 |
| child_intent | VARCHAR(30) | 발화 의도 코드 |
| main_point | TEXT | 핵심 뜻 (nullable) |
| detected_elements | TEXT | 탐지된 사고 요소 JSON 배열 `[{"type":"REASON","evidence":"..."}]` |
| utterance_validity | VARCHAR(20) | VALID / SHORT / UNCLEAR / OFF_TOPIC / PLAYFUL |

---

## 3. 공통 명세

### 공통 응답 형식

```json
{
  "success": true,
  "data": { },
  "message": null
}
```

실패 시:
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지"
}
```

### 에러 코드

| 코드 | HTTP | 설명 |
|------|------|------|
| SESSION_001 | 404 | 진행 중인 세션을 찾을 수 없음 |
| SESSION_002 | 400 | 이미 완료된 세션 |
| STT_001 | 500 | 음성 변환 실패 |
| TTS_001 | 500 | 음성 합성 실패 |
| AI_001 | 500 | LLM 분석 실패 |

### 인증
- 김현정 담당 인증 모듈에서 발급한 JWT를 Authorization 헤더에 포함
- `Authorization: Bearer {token}`
- 모든 API 인증 필수 (STT/TTS 포함)

---

## 4. POST /sessions/:id/utterances

아이의 발화를 제출하면 분석 → 진행 판단 → 캐릭터 반응까지 처리하고 결과를 반환합니다.

### Request

```
POST /sessions/{sessionId}/utterances
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "sceneId": 1,
  "text": "며느리가 창피해서 계속 참았던 것 같아요",
  "sttRawText": "며느리가창피해서계속참았던것같아요"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sceneId | Long | Y | 현재 장면 ID |
| text | String | Y | 아이 최종 발화 텍스트 |
| sttRawText | String | N | STT 원본 텍스트 (후처리 전) |

### Response

**NORMAL / GUIDED 모드 (대화 계속)**

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "sceneId": 1,
    "childMessageId": 10,
    "analysisResult": {
      "childIntent": "PERSPECTIVE",
      "detectedElements": [
        { "type": "PERSPECTIVE", "evidence": "창피해서 계속 참았던 것 같아요" }
      ],
      "utteranceValidity": "VALID"
    },
    "progressResult": {
      "mode": "NORMAL",
      "accumulatedElements": ["PERSPECTIVE"],
      "missingElements": ["REASON", "SOLUTION"]
    },
    "characterMessage": {
      "messageId": 11,
      "text": "그래, 며느리도 많이 힘들었겠구나...",
      "isClosing": false
    },
    "sceneCompleted": false,
    "nextSceneId": null
  }
}
```

**CLOSING 모드 (장면 종료 → 다음 장면 이동)**

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "sceneId": 1,
    "childMessageId": 14,
    "analysisResult": { ... },
    "progressResult": {
      "mode": "CLOSING",
      "accumulatedElements": ["PERSPECTIVE", "REASON", "SOLUTION"],
      "missingElements": []
    },
    "characterMessage": {
      "messageId": 15,
      "text": "고마워. 네 덕분에 용기를 낼 수 있을 것 같아.",
      "isClosing": true
    },
    "sceneCompleted": true,
    "nextSceneId": 2
  }
}
```

**마지막 장면 완료 (nextSceneId = null)**

```json
{
  "sceneCompleted": true,
  "nextSceneId": null
}
```

> `nextSceneId`가 null이면 이야기 전체 완료. 프론트는 후속 활동 화면으로 전환합니다.

### 진행 모드 설명

| mode | 의미 | 프론트 처리 |
|------|------|------------|
| NORMAL | 대화 정상 진행 | 캐릭터 대사 표시 후 마이크 재활성화 |
| GUIDED | 유도 질문 포함 | 캐릭터 대사 표시 후 마이크 재활성화 |
| CLOSING | 장면 종료 | 캐릭터 마지막 대사 표시 → nextSceneId로 이동 |

---

## 5. POST /speech/stt

음성 파일을 텍스트로 변환합니다. OpenAI Whisper 사용.

### Request

```
POST /speech/stt
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| audio | File | Y | 음성 파일 (webm, mp4, wav, m4a 등) |

### Response

```json
{
  "success": true,
  "data": {
    "text": "며느리가 창피해서 계속 참았던 것 같아요",
    "sttRawText": "며느리가 창피해서 계속 참았던 것 같아요"
  }
}
```

| 필드 | 설명 |
|------|------|
| text | 화면에 표시할 텍스트 |
| sttRawText | STT 원본 (현재는 동일, 추후 후처리 적용 시 분리) |

> 프론트 흐름: 녹음 완료 → `/speech/stt` 호출 → text를 화면 표시 → 아이 확인 후 보내기 → `/sessions/:id/utterances` 호출

---

## 6. POST /speech/tts

텍스트를 음성 파일로 변환합니다. OpenAI TTS 사용. 캐릭터 대사 재생에 사용합니다.

### Request

```
POST /speech/tts
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "text": "그래, 며느리도 많이 힘들었겠구나..."
}
```

### Response

```
Content-Type: audio/mpeg
Body: [audio binary]
```

> 프론트 흐름:
> - 장면 첫 진입 시 `character_opening` 텍스트로 TTS 호출
> - utterances 응답의 `characterMessage.text`로 TTS 호출
> - "다시 듣기" 버튼 시 동일 텍스트로 재호출

---

## 7. GET /reports/:sessionId

세션 완료 후 보호자 리포트를 반환합니다.

### Request

```
GET /reports/{sessionId}
Authorization: Bearer {token}
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "storyTitle": "방귀 뀌는 며느리",
    "completedAt": "2026-08-07T14:30:00",
    "elementSummary": {
      "accumulated": ["PERSPECTIVE", "REASON", "EMOTION"],
      "totalRequired": 6,
      "achievementRate": 0.5,
      "logic": {
        "detected": ["REASON"],
        "total": 4,
        "rate": 0.25
      },
      "empathy": {
        "detected": ["EMOTION"],
        "total": 2,
        "rate": 0.5
      },
      "perspective": {
        "detected": ["PERSPECTIVE"],
        "total": 2,
        "rate": 0.5
      }
    },
    "scenes": [
      {
        "sceneOrder": 1,
        "characterName": "며느리",
        "turnCount": 3,
        "endReason": "GOAL_MET",
        "detectedElements": ["PERSPECTIVE", "REASON"]
      },
      {
        "sceneOrder": 2,
        "characterName": "시아버지",
        "turnCount": 4,
        "endReason": "MAX_TURNS",
        "detectedElements": ["EMOTION"]
      }
    ],
    "representativeUtterances": [
      {
        "sceneOrder": 1,
        "text": "며느리가 창피해서 계속 참았던 것 같아요. 가족들이 이상하게 생각할까봐 무서웠을 것 같아요.",
        "elements": ["PERSPECTIVE", "REASON"]
      }
    ],
    "learningGuide": {
      "summary": "이번 이야기에서 3가지 사고 요소를 표현했어요. (목표 달성률 50%)",
      "strengthElements": ["PERSPECTIVE", "REASON", "EMOTION"],
      "growthElements": ["DECISION", "SOLUTION", "EMPATHY"]
    }
  }
}
```

### 역량 카테고리 매핑

| 카테고리 | 포함 사고 요소 |
|----------|--------------|
| logic (논리) | REASON, DECISION, SOLUTION, RESULT |
| empathy (감정·공감) | EMOTION, EMPATHY |
| perspective (관점·관계) | PERSPECTIVE, REQUEST |

---

## 8. 사고 요소 코드 레퍼런스

| 코드 | 의미 |
|------|------|
| DECISION | 선택·입장 결정 |
| REASON | 판단·의견·선택의 이유 |
| PERSPECTIVE | 다른 인물의 상황·입장 고려 |
| SOLUTION | 문제 해결의 구체적 행동·방법 |
| RESULT | 행동 이후 결과·영향 예측 |
| EMOTION | 감정 직접 표현 |
| EMPATHY | 타인 감정·어려움 이해·배려 |
| REQUEST | 특정 상대에게 행동·태도 변화 요구 |

---

## 9. 프론트엔드 연동 흐름 요약

### 장면 진행 흐름

```
1. 장면 진입
   → POST /speech/tts (character_opening 텍스트)
   → 음성 자동 재생

2. 아이 발화
   → 마이크 활성화
   → 녹음 완료 시 POST /speech/stt
   → text 화면 표시 (아이 확인)
   → 보내기 버튼 → POST /sessions/:id/utterances

3. 캐릭터 반응
   → 응답의 characterMessage.text 표시
   → POST /speech/tts (characterMessage.text)
   → 음성 재생

4. 반복 또는 전환
   → sceneCompleted=false: 2번으로 돌아감
   → sceneCompleted=true: nextSceneId로 이동
   → nextSceneId=null: 후속 활동 화면으로 전환
```

### 리포트 조회 흐름

```
세션 완료 후 보호자 계정으로 전환
→ GET /reports/{sessionId}
→ 말하기 역량 분석 탭: elementSummary (logic / empathy / perspective)
→ 대표 발화 탭: representativeUtterances
→ 가정 학습 가이드 탭: learningGuide
```

---

## 10. 주요 제약 및 참고사항

- `character_opening`, `character_closing`, `scene_description`은 운영사 확정 고정 콘텐츠. 수정 불가.
- LLM 모델: `gpt-5-mini` (분석·캐릭터 대사 동일 모델)
- CLOSING 턴에서 캐릭터 대사 LLM 미호출 → 토큰 절감
- STT 원본 음성 파일은 서버에 저장하지 않음
- `missing_elements`는 DB 미저장, 응답 시 서버에서 계산하여 반환
