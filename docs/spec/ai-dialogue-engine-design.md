# AI 대화 엔진 · 음성 연동 · 리포트 설계문서

> 담당자: 전우선
> 작성일: 2026-08-07
> 최종 수정: 2026-08-08
> 대상 독자: 백엔드 팀원, 프론트엔드 팀

---

## 1. 개요

### 담당 API 목록

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/sessions/{sessionId}/utterances` | 아이 발화 제출 → 분석 → 캐릭터 반응 |
| POST | `/api/speech/stt` | 음성 파일 → 텍스트 변환 |
| POST | `/api/speech/tts` | 텍스트 → 음성 파일 |
| GET | `/api/reports/{sessionId}` | 보호자 리포트 조회 |

### 아키텍처 개요

```
POST /api/sessions/:id/utterances
        │
        ├─ 1. request.sceneId로 장면 직접 조회 (story_scenes)
        ├─ 2. 아이 메시지 저장 (messages)
        ├─ 3. 발화 분석 LLM 호출 (gpt-5-mini)
        ├─ 4. 서버 후처리 (evidence 검증, 중복 제거)
        ├─ 5. 분석 결과 저장 (utterance_analyses)
        ├─ 6. 누적 요소 갱신 + 진행 모드 판정 (서버 규칙)
        ├─ 7. 미션 노출 여부 판단 (showMission)
        ├─ 8a. CLOSING → 고정 마지막 대사, 다음 대화 장면으로 이동
        └─ 8b. NORMAL/GUIDED → 캐릭터 대사 생성 LLM 호출 (gpt-5-mini)
```

**LLM 호출 횟수:** 일반 턴 2회, CLOSING 턴 1회 (토큰 절감)

**캐릭터 고정 대사의 `ㅇㅇ`:** 아이 이름으로 자동 치환 후 반환

---

## 2. 관련 DB 테이블

### stories (참고)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| title | VARCHAR(100) | 이야기 제목 |
| summary | TEXT | 목록·상세 소개 |
| difficulty | VARCHAR(20) | 난이도 (예: 보통) |
| topics | TEXT | 주제 JSON 배열 ex) `["다름","자기이해","장점 발견"]` |
| thumbnail_url | VARCHAR(500) | 대표 이미지 |
| introduction | TEXT | 도입 내용 |
| situation | TEXT | 장면 배경 설명 |
| child_role | VARCHAR(200) | 아이 역할 설명 |
| estimated_minutes | INTEGER | 예상 소요 시간 |
| post_activity_config | TEXT | 후속 활동 설정 JSON |
| status | VARCHAR(20) | published / draft |

### story_scenes (읽기 전용)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| story_id | BIGINT FK | |
| scene_order | INTEGER | 장면 순서 (1~9) |
| scene_description | TEXT | 장면 상황 설명 (고정) |
| conflict | TEXT | 갈등 요약 |
| character_name | VARCHAR(50) | 캐릭터 이름 (내러레이션 장면은 null) |
| character_opening | TEXT | 고정 첫 대사 (`ㅇㅇ` → 아이 이름 치환) |
| character_closing | TEXT | 고정 마지막 대사 (`ㅇㅇ` → 아이 이름 치환) |
| scene_goal | TEXT | 장면 학습 목표 |
| required_elements | TEXT | 필수 사고 요소 JSON 배열 |
| element_criteria | TEXT | 요소별 인정 기준 JSON 객체 |
| remaining_worries | TEXT | 캐릭터 걱정 정보 JSON 객체 (유도 시 사용) |
| has_mission | BOOLEAN | 미션 포함 여부 (true: 대화3, 대화4) |
| preferred_turns | INTEGER | 권장 최소 턴 수 |
| max_turns | INTEGER | 최대 허용 턴 수 |

### story_sessions (읽기/쓰기)

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| child_id | BIGINT FK | |
| story_id | BIGINT FK | |
| current_scene_id | BIGINT FK → story_scenes | 현재 진행 중인 장면 (대화 장면 기준) |
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
| detected_elements | TEXT | `[{"type":"REASON","evidence":"..."}]` |
| utterance_validity | VARCHAR(20) | VALID / SHORT / UNCLEAR / OFF_TOPIC / PLAYFUL |

---

## 3. 공통 명세

### 공통 응답 형식

```json
{
  "success": true,
  "data": { },
  "message": "요청이 성공했습니다."
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
| AI_001 | 500 | 발화 분석 실패 |
| AI_002 | 500 | 캐릭터 응답 생성 실패 |
| AI_003 | 500 | 음성 변환(STT) 실패 |
| AI_004 | 500 | 음성 합성(TTS) 실패 |

### 인증
- `Authorization: Bearer {token}` — 모든 API 필수

---

## 4. POST /api/sessions/{sessionId}/utterances

### Request

```
POST /api/sessions/{sessionId}/utterances
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{
  "sceneId": 3,
  "text": "며느리가 창피해서 계속 참았던 것 같아요",
  "sttRawText": "며느리가창피해서계속참았던것같아요"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sceneId | Long | Y | 현재 대화 장면 ID |
| text | String | Y | 아이 최종 발화 텍스트 |
| sttRawText | String | N | STT 원본 텍스트 |

### Response

**NORMAL / GUIDED 모드**

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "sceneId": 3,
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
      "missingElements": ["REASON", "SOLUTION", "EMOTION"]
    },
    "characterMessage": {
      "messageId": 11,
      "text": "그래, 며느리도 많이 힘들었겠구나...",
      "isClosing": false
    },
    "sceneCompleted": false,
    "nextSceneId": null,
    "showMission": false
  }
}
```

**CLOSING 모드 (장면 종료)**

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "sceneId": 3,
    "childMessageId": 14,
    "analysisResult": { "...": "..." },
    "progressResult": {
      "mode": "CLOSING",
      "accumulatedElements": ["PERSPECTIVE", "REASON", "SOLUTION", "EMOTION"],
      "missingElements": []
    },
    "characterMessage": {
      "messageId": 15,
      "text": "그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.",
      "isClosing": true
    },
    "sceneCompleted": true,
    "nextSceneId": 5,
    "showMission": false
  }
}
```

> `nextSceneId`가 null이면 이야기 전체 완료 → 후속 활동 화면으로 전환

### showMission 필드

| 값 | 의미 | 프론트 처리 |
|----|------|------------|
| false | 미션 없거나 노출 조건 미충족 | 미션 카드 숨김 |
| true | 미션 노출 시점 | 미션 카드 표시 |

**노출 조건:**
- 미션이 있는 장면(`has_mission=true`)에서만 true 가능
- 미션1(대화3): SOLUTION이 이번 턴에 탐지됐거나, 2턴 이상 경과 후 SOLUTION 미충족
- 미션2(대화4): EMOTION 또는 PERSPECTIVE가 누적 요소에 있을 때

### 진행 모드

| mode | 프론트 처리 |
|------|------------|
| NORMAL | 캐릭터 대사 표시 후 마이크 재활성화 |
| GUIDED | 캐릭터 대사 표시 후 마이크 재활성화 |
| CLOSING | 마지막 대사 표시 → nextSceneId로 이동 |

---

## 5. POST /api/speech/stt

### Request

```
POST /api/speech/stt
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

> 프론트 흐름: 녹음 완료 → `/api/speech/stt` → text 화면 표시 → 아이 확인 → 보내기 → `/api/sessions/:id/utterances`

---

## 6. POST /api/speech/tts

### Request

```
POST /api/speech/tts
Authorization: Bearer {token}
Content-Type: application/json
```

```json
{ "text": "그래, 며느리도 많이 힘들었겠구나..." }
```

### Response

```
Content-Type: audio/mpeg
Body: [audio binary]
```

> 프론트 흐름:
> - 대화 장면 진입 시 `character_opening` 텍스트로 TTS 호출
> - utterances 응답의 `characterMessage.text`로 TTS 호출
> - "다시 듣기" 버튼 시 동일 텍스트로 재호출

---

## 7. GET /api/reports/{sessionId}

### Request

```
GET /api/reports/{sessionId}
Authorization: Bearer {token}
```

### Response

```json
{
  "success": true,
  "data": {
    "sessionId": 1,
    "storyTitle": "방귀 뀌는 며느리",
    "completedAt": "2026-08-08T14:30:00",
    "elementSummary": {
      "accumulated": ["PERSPECTIVE", "REASON", "EMOTION"],
      "totalRequired": 12,
      "achievementRate": 0.25,
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
        "sceneOrder": 3,
        "characterName": "방귀쟁이 며느리",
        "turnCount": 3,
        "endReason": "GOAL_MET",
        "detectedElements": ["PERSPECTIVE", "REASON"]
      }
    ],
    "representativeUtterances": [
      {
        "sceneOrder": 3,
        "text": "며느리가 창피해서 계속 참았던 것 같아요.",
        "elements": ["PERSPECTIVE", "REASON"]
      }
    ],
    "learningGuide": {
      "summary": "이번 이야기에서 3가지 사고 요소를 표현했어요. (목표 달성률 25%)",
      "strengthElements": ["PERSPECTIVE", "REASON", "EMOTION"],
      "growthElements": ["DECISION", "SOLUTION", "EMPATHY", "REQUEST", "RESULT"]
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

## 8. 사고 요소 코드

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

## 9. 프론트엔드 연동 흐름

### 장면 진행 흐름

```
1. 대화 장면 진입
   → POST /api/speech/tts (character_opening 텍스트)
   → 음성 자동 재생 + 텍스트 표시

2. 아이 발화
   → 마이크 활성화
   → 녹음 완료 시 POST /api/speech/stt
   → text 화면 표시 (아이 확인)
   → 보내기 → POST /api/sessions/{id}/utterances

3. 응답 처리
   → characterMessage.text 표시 + TTS 호출
   → showMission=true이면 미션 카드 표시
   → sceneCompleted=false: 2번으로
   → sceneCompleted=true, nextSceneId 있음: 다음 대화 장면으로
   → sceneCompleted=true, nextSceneId=null: 후속 활동 화면
```

### 화면-장면 매핑

| 화면 | 내러레이션 | 대화 | 미션 |
|------|-----------|------|------|
| 도입 | sc_banggui_01 | - | - |
| 장면1 | sc_banggui_02 | sc_banggui_03 | - |
| 장면2 | sc_banggui_04 | sc_banggui_05 | - |
| 장면3 | sc_banggui_06 | sc_banggui_07 | 미션1 |
| 장면4 | sc_banggui_08 | sc_banggui_09 | 미션2 |

---

## 10. 주요 제약 및 참고사항

- `character_opening`, `character_closing`, `scene_description` 고정 콘텐츠 (수정 불가)
- `character_opening`, `character_closing`의 `ㅇㅇ`는 아이 이름으로 자동 치환
- LLM 모델: `gpt-5-mini` (분석·캐릭터 대사 동일)
- CLOSING 턴에서 캐릭터 대사 LLM 미호출 → 토큰 절감
- STT 원본 음성 파일 서버 저장 안 함
- `missing_elements` DB 미저장, 응답 시 서버 계산
- 장면 이동 시 내러레이션 장면 건너뛰고 다음 대화 장면으로 직행
- 시드 데이터: has_mission=true → sc_banggui_07(대화3), sc_banggui_09(대화4)
