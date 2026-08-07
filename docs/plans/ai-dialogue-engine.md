# AI 대화 엔진·음성 연동·리포트 Implementation Plan

> 담당자: 전우선
> 작성일: 2026-08-07
> 최종 수정: 2026-08-08
> **상태: 구현 완료 (API 키 수령 후 통합 테스트 대기)**

**Goal:** POST /api/sessions/:id/utterances 핵심 파이프라인(발화 저장→LLM 분석→후처리→진행 판단→캐릭터 대사 생성)과 STT/TTS, 보호자 리포트 API를 구현한다.

**Architecture:** 발화 분석·진행 판단·캐릭터 응답을 독립 모듈로 분리한다. 진행 판단 엔진은 LLM 없이 순수 규칙으로 동작하여 토큰 비용을 최소화한다. LLM 호출은 AnalysisLlmClient(분석)와 CharacterResponseClient(대사 생성) 두 클라이언트로 격리하고, 나머지 로직은 서버 코드로 처리한다.

**Tech Stack:** Spring Boot 4.1.0 · Java 17 · JPA(Hibernate) · PostgreSQL(Railway) · OpenAI API(gpt-4o-mini, Whisper-1, TTS-1) · Spring RestClient · Jackson · JUnit 5 · Mockito · Lombok

## Global Constraints

- 모델: `gpt-4o-mini` (분석 + 캐릭터 대사 모두 동일)
- 1세션(8턴) 기준 토큰 약 1.5만~2만. 토큰 절감 우선.
- `scene_description`, `character_opening`, `character_closing` 수정 불가 고정 콘텐츠.
- `character_opening`, `character_closing`의 `ㅇㅇ`는 아이 이름으로 자동 치환.
- `missing_elements` DB 저장 안 함. 서버에서 매 요청마다 계산.
- `evidence` 반드시 아이 발화 원문의 부분 문자열이어야 함.
- CLOSING 턴에서는 LLM 호출 없이 `character_closing` 고정 대사 사용.
- 장면 이동: 내러레이션 장면 건너뛰고 다음 대화 장면(character_name != null)으로 직행.
- 담당 API: `POST /api/sessions/:id/utterances`, `POST /api/speech/stt`, `POST /api/speech/tts`, `GET /api/reports/:sessionId`
- 다른 팀원 담당 API는 인터페이스만 소비. 직접 구현 금지.
- 패키지 루트: `com.potential.goodquestion`
- StorySession 패키지: `domain/storysession/` ✅ 완료
- StorySession.currentScene: `@ManyToOne StoryScene` ✅ 완료

---

## File Map

```
src/main/java/com/potential/goodquestion/
├── common/
│   ├── code/
│   │   └── AiErrorCode.java                ✅ AI_001~AI_004 에러 코드
│   ├── engine/
│   │   ├── PostProcessor.java              ✅ evidence 검증, 중복 제거
│   │   ├── ProgressJudgeEngine.java        ✅ NORMAL/GUIDED/CLOSING 판정
│   │   ├── GuidanceTargetSelector.java     ✅ 유도 대상 요소 선택
│   │   ├── ReactionKeyResolver.java        ✅ childIntent+validity → reactionKey 매핑
│   │   └── vo/
│   │       ├── DetectedElement.java        ✅ {type, evidence} record
│   │       ├── SessionState.java           ✅ 진행 판단 입력 VO
│   │       └── ProgressJudgeResult.java    ✅ 진행 판단 출력 VO
│   ├── config/
│   │   └── OpenAiConfig.java               ✅ OpenAI RestClient 빈
│   ├── openai/
│   │   ├── AnalysisLlmClient.java          ✅ 발화 분석 LLM + 이야기 STT 힌트 포함
│   │   ├── CharacterResponseClient.java    ✅ 캐릭터 대사 생성 LLM
│   │   ├── WhisperClient.java              ✅ STT + 방귀 뀌는 며느리 키워드 힌트
│   │   ├── OpenAiTtsClient.java            ✅ TTS
│   │   └── dto/
│   │       ├── AnalysisRequest.java        ✅
│   │       ├── AnalysisResponse.java       ✅
│   │       ├── CharacterRequest.java       ✅
│   │       └── CharacterResponse.java      ✅
│   ├── enums/
│   │   ├── SpeakerType.java                ✅ CHILD, CHARACTER
│   │   ├── ResponseMode.java               ✅ NORMAL, GUIDED, CLOSING
│   │   ├── ClosingReason.java              ✅ GOAL_MET, MAX_TURNS
│   │   ├── UtteranceValidity.java          ✅ VALID, SHORT, UNCLEAR, OFF_TOPIC, PLAYFUL
│   │   ├── ThinkingElement.java            ✅ 8종 사고 요소
│   │   └── ReactionKey.java                ✅ 7종 반응 키 + isSoftCueSkip()
│   └── util/
│       └── JsonUtils.java                  ✅ JSON 파싱 공통 유틸
├── domain/
│   ├── scene/
│   │   ├── entity/StoryScene.java          ✅ has_mission 필드 포함
│   │   └── repository/StorySceneRepository.java ✅ @Cacheable("scenes") 적용
│   ├── story/
│   │   └── entity/Story.java               ✅ summary, difficulty, topics, status, post_activity_config 추가
│   ├── storysession/
│   │   ├── entity/StorySession.java        ✅ currentScene(@ManyToOne) + advanceScene() 완료
│   │   └── repository/StorySessionRepository.java ✅
│   ├── message/
│   │   ├── entity/Message.java             ✅
│   │   └── repository/MessageRepository.java ✅
│   ├── utterance/
│   │   ├── entity/UtteranceAnalysis.java   ✅
│   │   ├── repository/UtteranceAnalysisRepository.java ✅
│   │   ├── controller/UtteranceController.java ✅ /api/sessions/{id}/utterances
│   │   ├── service/UtteranceService.java   ✅ showMission, ㅇㅇ→이름 치환 포함
│   │   └── dto/
│   │       ├── UtteranceRequest.java       ✅
│   │       └── UtteranceResponse.java      ✅ showMission 필드 포함
│   ├── speech/
│   │   ├── controller/SpeechController.java ✅ /api/speech/stt, /api/speech/tts
│   │   ├── service/SttService.java         ✅
│   │   ├── service/TtsService.java         ✅
│   │   └── dto/
│   │       ├── SttResponse.java            ✅
│   │       └── TtsRequest.java             ✅
│   └── report/
│       ├── controller/ReportController.java ✅ /api/reports/{sessionId}
│       ├── service/ReportService.java      ✅ N+1 해결, JsonUtils 적용
│       └── dto/ReportResponse.java         ✅

src/test/java/com/potential/goodquestion/
├── engine/
│   ├── PostProcessorTest.java
│   ├── ProgressJudgeEngineTest.java
│   └── ReactionKeyResolverTest.java        ✅ 21개 케이스
├── utterance/
│   └── UtteranceServiceTest.java
└── speech/
    └── SpeechControllerTest.java
```

---

## Task 1: 공유 엔티티 + 열거형 생성

**Files:**
- Create: `domain/scene/entity/StoryScene.java`
- Create: `domain/scene/repository/StorySceneRepository.java`
- Create: `domain/session/entity/StorySession.java` (기존 Session.java 대체)
- Create: `domain/session/repository/StorySessionRepository.java`
- Create: `domain/message/entity/Message.java`
- Create: `domain/message/repository/MessageRepository.java`
- Create: `domain/utterance/entity/UtteranceAnalysis.java`
- Create: `domain/utterance/repository/UtteranceAnalysisRepository.java`
- Create: `common/enums/SpeakerType.java`, `ResponseMode.java`, `ClosingReason.java`, `UtteranceValidity.java`, `ThinkingElement.java`

**Interfaces:**
- Produces: `StoryScene`, `StorySession`, `Message`, `UtteranceAnalysis` — Task 3~9가 모두 의존

- [ ] **Step 1: 열거형 생성**

```java
// common/enums/SpeakerType.java
public enum SpeakerType { CHILD, CHARACTER }

// common/enums/ResponseMode.java
public enum ResponseMode { NORMAL, GUIDED, CLOSING }

// common/enums/ClosingReason.java
public enum ClosingReason { GOAL_MET, MAX_TURNS }

// common/enums/UtteranceValidity.java
public enum UtteranceValidity { VALID, SHORT, UNCLEAR, OFF_TOPIC, PLAYFUL }

// common/enums/ThinkingElement.java
public enum ThinkingElement {
    DECISION, REASON, PERSPECTIVE, SOLUTION, RESULT, EMOTION, EMPATHY, REQUEST;

    public static boolean isValid(String code) {
        try { valueOf(code); return true; } catch (IllegalArgumentException e) { return false; }
    }
}
```

- [ ] **Step 2: StoryScene 엔티티 생성**

```java
// domain/scene/entity/StoryScene.java
@Comment("이야기 장면")
@Entity
@Table(name = "story_scenes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryScene extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @Column(name = "scene_order", nullable = false)
    private Integer sceneOrder;

    @Column(name = "scene_description", columnDefinition = "TEXT", nullable = false)
    private String sceneDescription;

    @Column(name = "conflict", columnDefinition = "TEXT")
    private String conflict;

    @Column(name = "character_name", length = 50)
    private String characterName;

    @Column(name = "character_opening", columnDefinition = "TEXT")
    private String characterOpening;

    @Column(name = "character_closing", columnDefinition = "TEXT")
    private String characterClosing;

    @Column(name = "scene_goal", columnDefinition = "TEXT")
    private String sceneGoal;

    @Column(name = "required_elements", columnDefinition = "TEXT")
    private String requiredElements; // JSON: ["REASON","PERSPECTIVE","SOLUTION"]

    @Column(name = "element_criteria", columnDefinition = "TEXT")
    private String elementCriteria; // JSON: {"REASON": "기준설명", ...}

    @Column(name = "remaining_worries", columnDefinition = "TEXT")
    private String remainingWorries; // JSON: {"REASON": "걱정내용", ...}

    @Column(name = "preferred_turns", nullable = false)
    private Integer preferredTurns;

    @Column(name = "max_turns", nullable = false)
    private Integer maxTurns;
}
```

```java
// domain/scene/repository/StorySceneRepository.java
public interface StorySceneRepository extends JpaRepository<StoryScene, Long> {
    Optional<StoryScene> findByStoryIdAndSceneOrder(Long storyId, Integer sceneOrder);
    List<StoryScene> findByStoryIdOrderBySceneOrder(Long storyId);
}
```

- [ ] **Step 3: StorySession 엔티티 수정 (김현정 구현 완료 — 전우선은 currentScene 필드 교체만 담당)**

> ⚠️ `domain/storysession/entity/StorySession.java` 경로. 김현정이 기본 구현 완료.
> 전우선은 StoryScene 엔티티 생성 후 아래 두 가지만 교체:
> - `currentSceneId (Long)` → `@ManyToOne StoryScene currentScene`
> - `advanceToNextScene(Long nextSceneId)` → `advanceScene(StoryScene nextScene)`

```java
// domain/session/entity/StorySession.java
@Comment("이야기 진행 세션")
@Entity
@Table(name = "story_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorySession extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_scene_id")
    private StoryScene currentScene;

    @Column(name = "current_child_turn_count", nullable = false)
    private int currentChildTurnCount = 0;

    @Column(name = "accumulated_elements", columnDefinition = "TEXT")
    private String accumulatedElements = "[]"; // JSON array of ThinkingElement codes

    @Column(name = "last_detected_elements", columnDefinition = "TEXT")
    private String lastDetectedElements = "[]";

    @Column(name = "last_response_mode", length = 20)
    private String lastResponseMode;

    @Column(name = "last_guidance_target", length = 30)
    private String lastGuidanceTarget;

    @Column(name = "turns_without_new_element", nullable = false)
    private int turnsWithoutNewElement = 0;

    @Column(name = "consecutive_low_information_turns", nullable = false)
    private int consecutiveLowInformationTurns = 0;

    @Column(name = "scene_goal_met", nullable = false)
    private boolean sceneGoalMet = false;

    @Column(name = "scene_end_reason", length = 20)
    private String sceneEndReason;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "IN_PROGRESS";

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    // 진행 판단 후 상태 갱신 메서드
    public void updateAfterUtterance(
            String newAccumulatedElements,
            String newDetectedElements,
            String responseMode,
            String guidanceTarget,
            int turnsWithoutNewElement,
            int consecutiveLowInfoTurns,
            boolean goalMet,
            String endReason
    ) {
        this.currentChildTurnCount++;
        this.accumulatedElements = newAccumulatedElements;
        this.lastDetectedElements = newDetectedElements;
        this.lastResponseMode = responseMode;
        this.lastGuidanceTarget = guidanceTarget;
        this.turnsWithoutNewElement = turnsWithoutNewElement;
        this.consecutiveLowInformationTurns = consecutiveLowInfoTurns;
        this.sceneGoalMet = goalMet;
        this.sceneEndReason = endReason;
        this.lastActivityAt = LocalDateTime.now();
    }

    public void advanceScene(StoryScene nextScene) {
        this.currentScene = nextScene;
        this.currentChildTurnCount = 0;
        this.accumulatedElements = "[]";
        this.lastDetectedElements = "[]";
        this.lastResponseMode = null;
        this.lastGuidanceTarget = null;
        this.turnsWithoutNewElement = 0;
        this.consecutiveLowInformationTurns = 0;
        this.sceneGoalMet = false;
        this.sceneEndReason = null;
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }
}
```

```java
// domain/session/repository/StorySessionRepository.java
public interface StorySessionRepository extends JpaRepository<StorySession, Long> {
    Optional<StorySession> findByIdAndStatus(Long id, String status);
    List<StorySession> findByChildIdOrderByLastActivityAtDesc(Long childId);
}
```

- [ ] **Step 4: Message 엔티티 생성**

```java
// domain/message/entity/Message.java
@Comment("대화 메시지")
@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StorySession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    private StoryScene scene;

    @Enumerated(EnumType.STRING)
    @Column(name = "speaker_type", length = 20, nullable = false)
    private SpeakerType speakerType;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "stt_raw_text", columnDefinition = "TEXT")
    private String sttRawText;

    @Builder
    public Message(StorySession session, StoryScene scene, SpeakerType speakerType,
                   String text, String sttRawText) {
        this.session = session;
        this.scene = scene;
        this.speakerType = speakerType;
        this.text = text;
        this.sttRawText = sttRawText;
    }

    public static Message ofChild(StorySession session, StoryScene scene,
                                  String text, String sttRawText) {
        return new Message(session, scene, SpeakerType.CHILD, text, sttRawText);
    }

    public static Message ofCharacter(StorySession session, StoryScene scene, String text) {
        return new Message(session, scene, SpeakerType.CHARACTER, text, null);
    }
}
```

```java
// domain/message/repository/MessageRepository.java
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySessionIdAndSceneIdOrderByCreatedAtAsc(Long sessionId, Long sceneId);
    Optional<Message> findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
            Long sessionId, Long sceneId, SpeakerType speakerType);
    List<Message> findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(Long sessionId, SpeakerType speakerType);
}
```

- [ ] **Step 5: UtteranceAnalysis 엔티티 생성**

```java
// domain/utterance/entity/UtteranceAnalysis.java
@Comment("아이 발화 분석 결과")
@Entity
@Table(name = "utterance_analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UtteranceAnalysis extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false, unique = true)
    private Message message;

    @Column(name = "child_intent", length = 30)
    private String childIntent;

    @Column(name = "main_point", columnDefinition = "TEXT")
    private String mainPoint;

    @Column(name = "detected_elements", columnDefinition = "TEXT")
    private String detectedElements; // JSON: [{"type":"REASON","evidence":"..."}]

    @Column(name = "utterance_validity", length = 20)
    private String utteranceValidity;

    @Builder
    public UtteranceAnalysis(Message message, String childIntent, String mainPoint,
                             String detectedElements, String utteranceValidity) {
        this.message = message;
        this.childIntent = childIntent;
        this.mainPoint = mainPoint;
        this.detectedElements = detectedElements;
        this.utteranceValidity = utteranceValidity;
    }
}
```

```java
// domain/utterance/repository/UtteranceAnalysisRepository.java
public interface UtteranceAnalysisRepository extends JpaRepository<UtteranceAnalysis, Long> {
    List<UtteranceAnalysis> findByMessageSessionIdOrderByCreatedAtAsc(Long sessionId);
    List<UtteranceAnalysis> findByMessageSessionIdAndMessageSceneId(Long sessionId, Long sceneId);
}
```

- [ ] **Step 6: 빌드 확인**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL (컴파일 오류 없음)

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/enums/ \
        src/main/java/com/potential/goodquestion/domain/scene/ \
        src/main/java/com/potential/goodquestion/domain/session/entity/StorySession.java \
        src/main/java/com/potential/goodquestion/domain/session/repository/StorySessionRepository.java \
        src/main/java/com/potential/goodquestion/domain/message/ \
        src/main/java/com/potential/goodquestion/domain/utterance/entity/ \
        src/main/java/com/potential/goodquestion/domain/utterance/repository/
git commit -m "feat: add story_scenes, story_sessions, messages, utterance_analyses entities"
```

---

## Task 2: OpenAI 의존성 추가 및 RestClient 설정

**Files:**
- Modify: `build.gradle`
- Create: `common/config/OpenAiConfig.java`
- Modify: `src/main/resources/application.yaml`

**Interfaces:**
- Produces: `RestClient openAiRestClient` 빈 — Task 4, 5, 7, 8이 사용

- [ ] **Step 1: build.gradle에 의존성 추가**

```groovy
// build.gradle dependencies 블록에 추가
implementation 'com.fasterxml.jackson.core:jackson-databind' // 이미 포함된 경우 생략
```

> OpenAI 공식 Java SDK(`com.openai:openai-java`) 대신 Spring RestClient로 직접 호출. 이유: SDK가 아직 stable 아니며 토큰 제어를 직접 해야 함.

- [ ] **Step 2: application.yaml에 OpenAI 설정 추가**

```yaml
openai:
  api-key: ${OPENAI_API_KEY}
  base-url: https://api.openai.com/v1
  model:
    analysis: gpt-5-mini
    character: gpt-5-mini
    stt: whisper-1
    tts: tts-1
    tts-voice: nova
```

- [ ] **Step 3: OpenAiConfig 생성**

```java
// common/config/OpenAiConfig.java
@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Bean
    public RestClient openAiRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew compileJava
```

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/main/resources/application.yaml \
        src/main/java/com/potential/goodquestion/common/config/OpenAiConfig.java
git commit -m "feat: configure OpenAI RestClient"
```

---

## Task 3: 서버 후처리 + 진행 판단 엔진 (순수 Java, 테스트 우선)

**Files:**
- Create: `common/engine/vo/SessionState.java`
- Create: `common/engine/vo/ProgressJudgeResult.java`
- Create: `common/engine/PostProcessor.java`
- Create: `common/engine/ProgressJudgeEngine.java`
- Create: `common/engine/GuidanceTargetSelector.java`
- Test: `test/.../engine/PostProcessorTest.java`
- Test: `test/.../engine/ProgressJudgeEngineTest.java`

**Interfaces:**
- Consumes: `ThinkingElement`, `ResponseMode`, `UtteranceValidity` (Task 1)
- Produces:
  - `PostProcessor.process(List<DetectedElement>, String) → List<DetectedElement>`
  - `ProgressJudgeEngine.judge(SessionState) → ProgressJudgeResult`
  - `GuidanceTargetSelector.select(Set<String> missing, String previousTarget, List<String> preferredOrder) → String`

- [ ] **Step 1: VO 클래스 생성**

```java
// common/engine/vo/DetectedElement.java
public record DetectedElement(String type, String evidence) {}

// common/engine/vo/SessionState.java
public record SessionState(
        int turnCount,
        int preferredTurns,
        int maxTurns,
        Set<String> accumulatedElements,
        Set<String> requiredElements,
        Set<String> newlyDetectedElements,
        String previousMode,       // null이면 첫 턴
        int turnsWithoutNewElement,
        int consecutiveLowInformationTurns
) {
    public Set<String> missingElements() {
        var missing = new HashSet<>(requiredElements);
        missing.removeAll(accumulatedElements);
        return missing;
    }
    public boolean isFirstTurn() { return turnCount == 1; }
    public boolean hasNewlyDetected() { return !newlyDetectedElements.isEmpty(); }
    public boolean wasGuidedLastTurn() { return "GUIDED".equals(previousMode); }
    public int remainingTurns() { return maxTurns - turnCount; }
}

// common/engine/vo/ProgressJudgeResult.java
public record ProgressJudgeResult(
        ResponseMode mode,
        ClosingReason closingReason,   // CLOSING이 아니면 null
        String guidanceTarget          // GUIDED가 아니면 null
) {
    public static ProgressJudgeResult normal() {
        return new ProgressJudgeResult(ResponseMode.NORMAL, null, null);
    }
    public static ProgressJudgeResult guided(String target) {
        return new ProgressJudgeResult(ResponseMode.GUIDED, null, target);
    }
    public static ProgressJudgeResult closing(ClosingReason reason) {
        return new ProgressJudgeResult(ResponseMode.CLOSING, reason, null);
    }
    public boolean isClosing() { return mode == ResponseMode.CLOSING; }
}
```

- [ ] **Step 2: PostProcessorTest 작성 (실패 확인)**

```java
// test/.../engine/PostProcessorTest.java
class PostProcessorTest {

    private final PostProcessor processor = new PostProcessor();

    @Test
    void evidence가_발화에_없으면_요소_제거() {
        var elements = List.of(new DetectedElement("REASON", "이 문장은 발화에 없음"));
        var result = processor.process(elements, "며느리가 창피해서 참았어요");
        assertThat(result).isEmpty();
    }

    @Test
    void evidence가_발화에_있으면_요소_유지() {
        var elements = List.of(new DetectedElement("REASON", "창피해서 참았어요"));
        var result = processor.process(elements, "며느리가 창피해서 참았어요");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("REASON");
    }

    @Test
    void 동일_type_중복시_하나만_유지() {
        var elements = List.of(
            new DetectedElement("REASON", "창피해서"),
            new DetectedElement("REASON", "부끄러워서")
        );
        var result = processor.process(elements, "창피해서 부끄러워서 참았어요");
        assertThat(result).hasSize(1);
    }

    @Test
    void 스키마에_없는_요소_제거() {
        var elements = List.of(new DetectedElement("UNKNOWN_TYPE", "참았어요"));
        var result = processor.process(elements, "참았어요");
        assertThat(result).isEmpty();
    }
}
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "*PostProcessorTest"
```
Expected: FAIL (PostProcessor 미존재)

- [ ] **Step 4: PostProcessor 구현**

```java
// common/engine/PostProcessor.java
@Component
public class PostProcessor {

    public List<DetectedElement> process(List<DetectedElement> rawElements, String childUtterance) {
        Set<String> seenTypes = new HashSet<>();
        return rawElements.stream()
                .filter(e -> ThinkingElement.isValid(e.type()))
                .filter(e -> childUtterance.contains(e.evidence()))
                .filter(e -> seenTypes.add(e.type()))   // 첫 번째 occurrence만 유지
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 5: 테스트 실행 → 성공 확인**

```bash
./gradlew test --tests "*PostProcessorTest"
```
Expected: PASS

- [ ] **Step 6: ProgressJudgeEngineTest 작성 (실패 확인)**

```java
// test/.../engine/ProgressJudgeEngineTest.java
class ProgressJudgeEngineTest {

    private final ProgressJudgeEngine engine = new ProgressJudgeEngine();

    private SessionState.Builder base() {
        return new SessionState(
                1, 2, 4,
                new HashSet<>(),
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                new HashSet<>(),
                null, 0, 0
        );
    }

    @Test
    void 필수요소_충족_AND_최소턴_이상이면_CLOSING_GOAL_MET() {
        var state = new SessionState(
                3, 2, 4,
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of(),
                "NORMAL", 0, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(result.closingReason()).isEqualTo(ClosingReason.GOAL_MET);
    }

    @Test
    void maxTurns_도달시_CLOSING_MAX_TURNS() {
        var state = new SessionState(
                4, 2, 4,
                Set.of("REASON"),
                Set.of("REASON", "PERSPECTIVE", "SOLUTION"),
                Set.of(),
                "NORMAL", 2, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(result.closingReason()).isEqualTo(ClosingReason.MAX_TURNS);
    }

    @Test
    void 첫_발화이면_NORMAL() {
        var state = new SessionState(
                1, 2, 4,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                null, 0, 0
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 직전_GUIDED이면_NORMAL_강제() {
        var state = new SessionState(
                2, 2, 4,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "GUIDED", 2, 2
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 저정보_2회연속이면_GUIDED() {
        var state = new SessionState(
                3, 2, 4,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 0, 2
        );
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.GUIDED);
    }

    @Test
    void 잔여턴_2이하이면_GUIDED() {
        var state = new SessionState(
                2, 2, 4,
                Set.of(),
                Set.of("REASON"),
                Set.of(),
                "NORMAL", 0, 0
        );
        // maxTurns=4, turnCount=2 → remainingTurns=2 ≤ 2
        var result = engine.judge(state);
        assertThat(result.mode()).isEqualTo(ResponseMode.GUIDED);
    }
}
```

- [ ] **Step 7: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "*ProgressJudgeEngineTest"
```
Expected: FAIL

- [ ] **Step 8: ProgressJudgeEngine 구현**

```java
// common/engine/ProgressJudgeEngine.java
@Component
public class ProgressJudgeEngine {

    public ProgressJudgeResult judge(SessionState state) {
        // 1. 종료 조건
        if (state.missingElements().isEmpty() && state.turnCount() >= state.preferredTurns()) {
            return ProgressJudgeResult.closing(ClosingReason.GOAL_MET);
        }
        if (state.turnCount() >= state.maxTurns()) {
            return ProgressJudgeResult.closing(ClosingReason.MAX_TURNS);
        }
        // 2. 강한 유도 제한 → NORMAL 강제
        if (state.isFirstTurn() || state.hasNewlyDetected() || state.wasGuidedLastTurn()) {
            return ProgressJudgeResult.normal();
        }
        // 3. 유도 필요성 판단
        boolean needsGuidance = !state.missingElements().isEmpty() &&
                (state.consecutiveLowInformationTurns() >= 2
                || state.turnsWithoutNewElement() >= 2
                || state.remainingTurns() <= 2);
        if (needsGuidance) {
            return ProgressJudgeResult.guided(null); // target은 GuidanceTargetSelector가 결정
        }
        return ProgressJudgeResult.normal();
    }
}
```

- [ ] **Step 9: GuidanceTargetSelector 구현**

```java
// common/engine/GuidanceTargetSelector.java
@Component
public class GuidanceTargetSelector {

    public String select(Set<String> missingElements, String previousTarget, List<String> preferredOrder) {
        if (missingElements.isEmpty()) return null;
        // 직전과 다른 요소 우선
        return preferredOrder.stream()
                .filter(missingElements::contains)
                .filter(e -> !e.equals(previousTarget))
                .findFirst()
                .orElse(missingElements.iterator().next()); // fallback
    }
}
```

- [ ] **Step 10: 테스트 실행 → 성공 확인**

```bash
./gradlew test --tests "*ProgressJudgeEngineTest" --tests "*PostProcessorTest"
```
Expected: PASS

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/engine/ \
        src/test/java/com/potential/goodquestion/engine/
git commit -m "feat: implement PostProcessor and ProgressJudgeEngine with tests"
```

---

## Task 4: 발화 분석 LLM 클라이언트

**Files:**
- Create: `common/openai/dto/AnalysisRequest.java`
- Create: `common/openai/dto/AnalysisResponse.java`
- Create: `common/openai/AnalysisLlmClient.java`

**Interfaces:**
- Consumes: `RestClient openAiRestClient` (Task 2), `DetectedElement` (Task 3)
- Produces: `AnalysisLlmClient.analyze(AnalysisRequest) → AnalysisResponse`

- [ ] **Step 1: AnalysisRequest / AnalysisResponse DTO 생성**

```java
// common/openai/dto/AnalysisRequest.java
public record AnalysisRequest(
        String sceneContext,
        String goal,
        String previousCharacterMessage,
        String childUtterance,
        List<String> targetElements,
        Map<String, String> elementCriteria
) {}

// common/openai/dto/AnalysisResponse.java
public record AnalysisResponse(
        String childIntent,
        List<DetectedElement> detectedElements,
        String utteranceValidity,
        String mainPoint
) {}
```

- [ ] **Step 2: AnalysisLlmClient 구현**

```java
// common/openai/AnalysisLlmClient.java
@Component
@RequiredArgsConstructor
public class AnalysisLlmClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model.analysis}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            당신은 아이의 발화를 분석하는 전문가입니다. 제공된 장면 맥락을 바탕으로 아이의 발화를 분석하고 반드시 JSON 형식으로만 응답하세요.
            
            분석 규칙:
            1. childIntent: 발화의 중심 의도 (QUESTION/OPINION/REASONING/SOLUTION/DECISION/PERSPECTIVE/EMOTION/REQUEST/CHALLENGE/PLAYFUL/OFF_TOPIC/SHORT_RESPONSE/UNCLEAR 중 하나)
            2. detectedElements: 이번 발화에서 직접 확인된 사고 요소와 원문 근거. 각 요소의 evidence는 반드시 childUtterance의 부분 문자열이어야 함.
            3. utteranceValidity: VALID/SHORT/UNCLEAR/OFF_TOPIC/PLAYFUL 중 하나
            4. mainPoint: 발화의 핵심 의미 (없으면 null)
            
            절대 금지: 아이가 말하지 않은 내용을 추론하거나 evidence를 만들어내지 마세요.
            응답 형식 예시:
            {"childIntent":"REASONING","detectedElements":[{"type":"PERSPECTIVE","evidence":"창피해서 계속 참았던 것 같아요"}],"utteranceValidity":"VALID","mainPoint":"며느리가 수치심으로 인해 방귀를 참았다는 것"}
            """;

    public AnalysisResponse analyze(AnalysisRequest request) {
        String userPrompt = buildUserPrompt(request);
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 500,
                "temperature", 0.3
        );

        String rawJson = openAiRestClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class)
                .get("choices").get(0).get("message").get("content")
                .asText();

        try {
            return objectMapper.readValue(rawJson, AnalysisResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("발화 분석 LLM 응답 파싱 실패: " + rawJson, e);
        }
    }

    private String buildUserPrompt(AnalysisRequest req) {
        return String.format("""
                장면 상황: %s
                학습 목표: %s
                직전 캐릭터 대사: %s
                아이 발화: %s
                확인할 사고 요소: %s
                요소별 인정 기준: %s
                """,
                req.sceneContext(),
                req.goal(),
                req.previousCharacterMessage() != null ? req.previousCharacterMessage() : "없음",
                req.childUtterance(),
                req.targetElements(),
                req.elementCriteria()
        );
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew compileJava
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/openai/
git commit -m "feat: add AnalysisLlmClient for utterance analysis"
```

---

## Task 5: 캐릭터 응답 LLM 클라이언트

**Files:**
- Create: `common/openai/dto/CharacterRequest.java`
- Create: `common/openai/dto/CharacterResponse.java`
- Create: `common/openai/CharacterResponseClient.java`

**Interfaces:**
- Consumes: `RestClient openAiRestClient`, `ProgressJudgeResult` (Task 3)
- Produces: `CharacterResponseClient.generate(CharacterRequest) → CharacterResponse`

- [ ] **Step 1: CharacterRequest / CharacterResponse DTO 생성**

```java
// common/openai/dto/CharacterRequest.java
public record CharacterRequest(
        String characterName,
        String sceneContext,
        String childUtterance,
        String childIntent,
        String responseMode,           // NORMAL or GUIDED
        String guidanceTarget,         // null if NORMAL without soft-cue
        String guidanceWorry,          // remainingWorries[guidanceTarget]
        String previousCharacterMessage
) {}

// common/openai/dto/CharacterResponse.java
public record CharacterResponse(String text) {}
```

- [ ] **Step 2: CharacterResponseClient 구현**

```java
// common/openai/CharacterResponseClient.java
@Component
@RequiredArgsConstructor
public class CharacterResponseClient {

    private final RestClient openAiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${openai.model.character}")
    private String model;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 한국 전래동화 속 캐릭터 '%s'입니다.
            아이와 이야기 속에서 자연스럽게 대화하세요.
            
            반응 원칙:
            - 아이의 발화에 진심으로 반응하세요.
            - 학습 질문처럼 보이는 표현("해결 방법을 말해 봐", "왜 그랬을까?")은 사용하지 마세요.
            - 캐릭터의 감정과 상황 안에서 이야기하세요.
            - 짧고 자연스러운 한 두 문장으로 답하세요.
            - 반드시 JSON 형식으로만 응답: {"text": "캐릭터 대사"}
            """;

    public CharacterResponse generate(CharacterRequest request) {
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, request.characterName());
        String userPrompt = buildUserPrompt(request);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "max_tokens", 200,
                "temperature", 0.7
        );

        String rawJson = openAiRestClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(JsonNode.class)
                .get("choices").get(0).get("message").get("content")
                .asText();

        try {
            return objectMapper.readValue(rawJson, CharacterResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("캐릭터 응답 LLM 파싱 실패: " + rawJson, e);
        }
    }

    private String buildUserPrompt(CharacterRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("장면 상황: ").append(req.sceneContext()).append("\n");
        sb.append("아이 발화: ").append(req.childUtterance()).append("\n");
        sb.append("아이 발화 의도: ").append(req.childIntent()).append("\n");
        if (req.previousCharacterMessage() != null) {
            sb.append("직전 내 대사: ").append(req.previousCharacterMessage()).append("\n");
        }
        if ("GUIDED".equals(req.responseMode()) && req.guidanceWorry() != null) {
            sb.append("(내가 아직 해소하지 못한 걱정: ").append(req.guidanceWorry()).append(")\n");
        }
        return sb.toString();
    }
}
```

- [ ] **Step 3: 빌드 확인**

```bash
./gradlew compileJava
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/openai/
git commit -m "feat: add CharacterResponseClient for LLM character dialogue generation"
```

---

## Task 6: POST /sessions/:id/utterances 핵심 API

**Files:**
- Create: `domain/utterance/dto/UtteranceRequest.java`
- Create: `domain/utterance/dto/UtteranceResponse.java`
- Create: `domain/utterance/service/UtteranceService.java`
- Create: `domain/utterance/controller/UtteranceController.java`
- Test: `test/.../utterance/UtteranceServiceTest.java`

**Interfaces:**
- Consumes: 모든 이전 태스크 결과물
- Produces: `POST /sessions/{sessionId}/utterances` → `UtteranceResponse`

**전체 처리 흐름:**
```
1. StorySession 조회 (sessionId, status=IN_PROGRESS)
2. currentScene(StoryScene) 조회
3. previousCharacterMessage 조회 (messages 테이블)
4. 아이 Message 저장 (messages)
5. AnalysisLlmClient.analyze() 호출
6. PostProcessor.process() — evidence 검증, 중복 제거
7. UtteranceAnalysis 저장
8. 누적 요소 갱신 (기존 accumulated ∪ 신규 detected)
9. SessionState 구성
10. ProgressJudgeEngine.judge()
11. GUIDED이면 GuidanceTargetSelector.select()
12. StorySession 상태 갱신 (updateAfterUtterance)
13. CLOSING이면: character_closing 사용, 다음 장면으로 advanceScene
14. NORMAL/GUIDED이면: CharacterResponseClient.generate() 호출
15. 캐릭터 Message 저장
16. UtteranceResponse 반환
```

- [ ] **Step 1: UtteranceRequest / UtteranceResponse DTO 생성**

```java
// domain/utterance/dto/UtteranceRequest.java
public record UtteranceRequest(
        @NotNull Long sceneId,
        @NotBlank String text,
        String sttRawText
) {}

// domain/utterance/dto/UtteranceResponse.java
public record UtteranceResponse(
        Long sessionId,
        Long sceneId,
        Long childMessageId,
        AnalysisResult analysisResult,
        ProgressResult progressResult,
        CharacterMessageResult characterMessage,
        boolean sceneCompleted,
        Long nextSceneId
) {
    public record AnalysisResult(
            String childIntent,
            List<DetectedElementDto> detectedElements,
            String utteranceValidity
    ) {}

    public record DetectedElementDto(String type, String evidence) {}

    public record ProgressResult(
            String mode,
            List<String> accumulatedElements,
            List<String> missingElements
    ) {}

    public record CharacterMessageResult(
            Long messageId,
            String text,
            boolean isClosing
    ) {}
}
```

- [ ] **Step 2: UtteranceServiceTest 작성 (실패 확인)**

```java
// test/.../utterance/UtteranceServiceTest.java
@ExtendWith(MockitoExtension.class)
class UtteranceServiceTest {

    @Mock StorySessionRepository sessionRepository;
    @Mock StorySceneRepository sceneRepository;
    @Mock MessageRepository messageRepository;
    @Mock UtteranceAnalysisRepository analysisRepository;
    @Mock AnalysisLlmClient analysisLlmClient;
    @Mock CharacterResponseClient characterResponseClient;
    @Mock PostProcessor postProcessor;
    @Mock ProgressJudgeEngine progressJudgeEngine;
    @Mock GuidanceTargetSelector guidanceTargetSelector;
    @Mock ObjectMapper objectMapper;

    @InjectMocks UtteranceService utteranceService;

    @Test
    void NORMAL_모드에서_캐릭터_응답_생성() throws Exception {
        // given
        var session = mockSession();
        var scene = mockScene();
        var request = new UtteranceRequest(1L, "며느리가 창피해서 참았어요", null);

        when(sessionRepository.findByIdAndStatus(1L, "IN_PROGRESS")).thenReturn(Optional.of(session));
        when(session.getCurrentScene()).thenReturn(scene);
        when(messageRepository.findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(any(), any(), eq(SpeakerType.CHARACTER))).thenReturn(Optional.empty());
        when(analysisLlmClient.analyze(any())).thenReturn(
            new AnalysisResponse("PERSPECTIVE", List.of(new DetectedElement("PERSPECTIVE", "창피해서 참았어요")), "VALID", null)
        );
        when(postProcessor.process(any(), any())).thenReturn(List.of(new DetectedElement("PERSPECTIVE", "창피해서 참았어요")));
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");
        when(objectMapper.readValue(anyString(), eq(List.class))).thenReturn(List.of());
        when(progressJudgeEngine.judge(any())).thenReturn(ProgressJudgeResult.normal());
        when(characterResponseClient.generate(any())).thenReturn(new CharacterResponse("그래, 많이 힘들었겠구나."));
        var savedChild = mock(Message.class);
        var savedCharacter = mock(Message.class);
        when(savedChild.getId()).thenReturn(10L);
        when(savedCharacter.getId()).thenReturn(11L);
        when(messageRepository.save(any())).thenReturn(savedChild, savedCharacter);

        // when
        var result = utteranceService.processUtterance(1L, request);

        // then
        assertThat(result.progressResult().mode()).isEqualTo("NORMAL");
        assertThat(result.characterMessage().isClosing()).isFalse();
        assertThat(result.sceneCompleted()).isFalse();
    }
}
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

```bash
./gradlew test --tests "*UtteranceServiceTest"
```
Expected: FAIL (UtteranceService 미존재)

- [ ] **Step 4: UtteranceService 구현**

```java
// domain/utterance/service/UtteranceService.java
@Service
@RequiredArgsConstructor
@Transactional
public class UtteranceService {

    private final StorySessionRepository sessionRepository;
    private final StorySceneRepository sceneRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final AnalysisLlmClient analysisLlmClient;
    private final CharacterResponseClient characterResponseClient;
    private final PostProcessor postProcessor;
    private final ProgressJudgeEngine progressJudgeEngine;
    private final GuidanceTargetSelector guidanceTargetSelector;
    private final ObjectMapper objectMapper;

    public UtteranceResponse processUtterance(Long sessionId, UtteranceRequest request) {
        // 1. 세션 + 장면 로드
        StorySession session = sessionRepository.findByIdAndStatus(sessionId, "IN_PROGRESS")
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        StoryScene scene = session.getCurrentScene();

        // 2. 직전 캐릭터 대사 조회
        String prevCharacterMsg = messageRepository
                .findTopBySessionIdAndSceneIdAndSpeakerTypeOrderByCreatedAtDesc(
                        sessionId, scene.getId(), SpeakerType.CHARACTER)
                .map(Message::getText)
                .orElse(null);

        // 3. 아이 메시지 저장
        Message childMessage = messageRepository.save(
                Message.ofChild(session, scene, request.text(), request.sttRawText()));

        // 4. LLM 분석
        AnalysisRequest analysisReq = buildAnalysisRequest(scene, prevCharacterMsg, request.text());
        AnalysisResponse rawAnalysis = analysisLlmClient.analyze(analysisReq);

        // 5. 서버 후처리
        List<DetectedElement> processedElements = postProcessor.process(
                rawAnalysis.detectedElements(), request.text());

        // 6. UtteranceAnalysis 저장
        analysisRepository.save(UtteranceAnalysis.builder()
                .message(childMessage)
                .childIntent(rawAnalysis.childIntent())
                .mainPoint(rawAnalysis.mainPoint())
                .detectedElements(toJson(processedElements))
                .utteranceValidity(rawAnalysis.utteranceValidity())
                .build());

        // 7. 누적 요소 갱신
        Set<String> existing = parseElements(session.getAccumulatedElements());
        Set<String> newlyDetected = processedElements.stream()
                .map(DetectedElement::type).collect(Collectors.toSet());
        existing.addAll(newlyDetected);
        Set<String> required = parseElements(scene.getRequiredElements());

        // 8. 저정보 카운터 갱신
        boolean isLowInfo = isLowInformation(rawAnalysis.utteranceValidity());
        int newLowInfoCount = isLowInfo ? session.getConsecutiveLowInformationTurns() + 1 : 0;
        int newNoProgressCount = newlyDetected.isEmpty() ? session.getTurnsWithoutNewElement() + 1 : 0;

        // 9. 진행 판단
        SessionState state = new SessionState(
                session.getCurrentChildTurnCount() + 1,
                scene.getPreferredTurns(),
                scene.getMaxTurns(),
                existing,
                required,
                newlyDetected,
                session.getLastResponseMode(),
                newNoProgressCount,
                newLowInfoCount
        );
        ProgressJudgeResult judgeResult = progressJudgeEngine.judge(state);

        // 10. GUIDED이면 유도 대상 선택
        String guidanceTarget = null;
        if (judgeResult.mode() == ResponseMode.GUIDED) {
            Map<String, String> preferredOrder = parseElementCriteria(scene.getRequiredElements());
            guidanceTarget = guidanceTargetSelector.select(
                    state.missingElements(),
                    session.getLastGuidanceTarget(),
                    new ArrayList<>(preferredOrder.keySet())
            );
        }

        // 11. 세션 상태 갱신
        session.updateAfterUtterance(
                toJson(new ArrayList<>(existing)),
                toJson(processedElements),
                judgeResult.mode().name(),
                guidanceTarget,
                newNoProgressCount,
                newLowInfoCount,
                judgeResult.isClosing() && judgeResult.closingReason() == ClosingReason.GOAL_MET,
                judgeResult.isClosing() ? judgeResult.closingReason().name() : null
        );

        // 12. 캐릭터 대사 결정
        Message characterMessage;
        boolean sceneCompleted = false;
        Long nextSceneId = null;

        if (judgeResult.isClosing()) {
            // CLOSING: 고정 마지막 대사 사용
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, scene.getCharacterClosing()));
            sceneCompleted = true;

            // 다음 장면으로 이동
            StoryScene nextScene = sceneRepository
                    .findByStoryIdAndSceneOrder(scene.getStory().getId(), scene.getSceneOrder() + 1)
                    .orElse(null);
            if (nextScene != null) {
                session.advanceScene(nextScene);
                nextSceneId = nextScene.getId();
            } else {
                session.complete();
            }
        } else {
            // NORMAL / GUIDED: LLM 캐릭터 대사 생성
            String worryForTarget = null;
            if (guidanceTarget != null) {
                Map<String, String> worries = parseMap(scene.getRemainingWorries());
                worryForTarget = worries.get(guidanceTarget);
            }
            CharacterRequest charReq = new CharacterRequest(
                    scene.getCharacterName(),
                    scene.getSceneDescription(),
                    request.text(),
                    rawAnalysis.childIntent(),
                    judgeResult.mode().name(),
                    guidanceTarget,
                    worryForTarget,
                    prevCharacterMsg
            );
            CharacterResponse charResp = characterResponseClient.generate(charReq);
            characterMessage = messageRepository.save(
                    Message.ofCharacter(session, scene, charResp.text()));
        }

        // 13. 응답 조립
        List<String> missing = new ArrayList<>(state.missingElements());
        return new UtteranceResponse(
                sessionId,
                scene.getId(),
                childMessage.getId(),
                new UtteranceResponse.AnalysisResult(
                        rawAnalysis.childIntent(),
                        processedElements.stream()
                                .map(e -> new UtteranceResponse.DetectedElementDto(e.type(), e.evidence()))
                                .toList(),
                        rawAnalysis.utteranceValidity()
                ),
                new UtteranceResponse.ProgressResult(
                        judgeResult.mode().name(),
                        new ArrayList<>(existing),
                        missing
                ),
                new UtteranceResponse.CharacterMessageResult(
                        characterMessage.getId(),
                        characterMessage.getText(),
                        sceneCompleted
                ),
                sceneCompleted,
                nextSceneId
        );
    }

    private boolean isLowInformation(String validity) {
        return validity != null && (validity.equals("SHORT") || validity.equals("UNCLEAR") || validity.equals("OFF_TOPIC"));
    }

    private AnalysisRequest buildAnalysisRequest(StoryScene scene, String prevCharMsg, String childUtterance) {
        Map<String, String> criteria = parseMap(scene.getElementCriteria());
        List<String> targetElements = parseElements(scene.getRequiredElements()).stream().toList();
        return new AnalysisRequest(
                scene.getSceneDescription() + "\n" + (scene.getConflict() != null ? scene.getConflict() : ""),
                scene.getSceneGoal(),
                prevCharMsg,
                childUtterance,
                targetElements,
                criteria
        );
    }

    private Set<String> parseElements(String json) {
        try {
            if (json == null || json.isBlank()) return new HashSet<>();
            return new HashSet<>(objectMapper.readValue(json, List.class));
        } catch (Exception e) { return new HashSet<>(); }
    }

    private Map<String, String> parseMap(String json) {
        try {
            if (json == null || json.isBlank()) return new HashMap<>();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) { return new HashMap<>(); }
    }

    private Map<String, String> parseElementCriteria(String json) {
        return parseMap(json);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }
}
```

- [ ] **Step 5: UtteranceController 구현**

```java
// domain/utterance/controller/UtteranceController.java
@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
public class UtteranceController {

    private final UtteranceService utteranceService;

    @PostMapping("/{sessionId}/utterances")
    public ResponseEntity<ApiResponse<UtteranceResponse>> processUtterance(
            @PathVariable Long sessionId,
            @RequestBody @Valid UtteranceRequest request) {
        UtteranceResponse response = utteranceService.processUtterance(sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

- [ ] **Step 6: ErrorCode에 SESSION_NOT_FOUND 추가**

```java
// common/code/ErrorCode.java에 추가
SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_001", "진행 중인 세션을 찾을 수 없습니다.")
```

- [ ] **Step 7: 테스트 실행 → 성공 확인**

```bash
./gradlew test --tests "*UtteranceServiceTest"
```
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/potential/goodquestion/domain/utterance/ \
        src/test/java/com/potential/goodquestion/utterance/
git commit -m "feat: implement POST /sessions/:id/utterances pipeline"
```

---

## Task 7: POST /speech/stt

**Files:**
- Create: `common/openai/WhisperClient.java`
- Create: `domain/speech/service/SttService.java`
- Create: `domain/speech/controller/SpeechController.java`
- Create: `domain/speech/dto/SttResponse.java`

**Interfaces:**
- Produces: `POST /speech/stt` (multipart/form-data, audio file) → `{ text, sttRawText }`

- [ ] **Step 1: SttResponse DTO 생성**

```java
// domain/speech/dto/SttResponse.java
public record SttResponse(String text, String sttRawText) {}
```

- [ ] **Step 2: WhisperClient 구현**

```java
// common/openai/WhisperClient.java
@Component
@RequiredArgsConstructor
public class WhisperClient {

    private final RestClient openAiRestClient;

    @Value("${openai.model.stt}")
    private String model;

    public String transcribe(MultipartFile audioFile) {
        try {
            byte[] audioBytes = audioFile.getBytes();
            String filename = audioFile.getOriginalFilename() != null
                    ? audioFile.getOriginalFilename() : "audio.webm";

            var multipart = new LinkedMultiValueMap<String, Object>();
            multipart.add("file", new ByteArrayResource(audioBytes) {
                @Override public String getFilename() { return filename; }
            });
            multipart.add("model", model);
            multipart.add("language", "ko");

            String response = openAiRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(JsonNode.class)
                    .get("text")
                    .asText();

            return response;
        } catch (IOException e) {
            throw new RuntimeException("STT 변환 실패", e);
        }
    }
}
```

- [ ] **Step 3: SttService + SpeechController 구현**

```java
// domain/speech/service/SttService.java
@Service
@RequiredArgsConstructor
public class SttService {
    private final WhisperClient whisperClient;

    public SttResponse transcribe(MultipartFile audioFile) {
        String text = whisperClient.transcribe(audioFile);
        return new SttResponse(text, text); // rawText = text (Whisper 결과 그대로)
    }
}

// domain/speech/controller/SpeechController.java
@RestController
@RequestMapping("/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SttService sttService;
    private final TtsService ttsService;

    @PostMapping("/stt")
    public ResponseEntity<ApiResponse<SttResponse>> stt(
            @RequestParam("audio") MultipartFile audioFile) {
        return ResponseEntity.ok(ApiResponse.success(sttService.transcribe(audioFile)));
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> tts(@RequestBody TtsRequest request) {
        byte[] audio = ttsService.synthesize(request.text());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .body(audio);
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew compileJava
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/openai/WhisperClient.java \
        src/main/java/com/potential/goodquestion/domain/speech/
git commit -m "feat: implement POST /speech/stt with Whisper API"
```

---

## Task 8: POST /speech/tts

**Files:**
- Create: `common/openai/OpenAiTtsClient.java`
- Create: `domain/speech/service/TtsService.java`
- Create: `domain/speech/dto/TtsRequest.java`

**Interfaces:**
- Produces: `POST /speech/tts` → `audio/mpeg` bytes

- [ ] **Step 1: TtsRequest DTO 생성**

```java
// domain/speech/dto/TtsRequest.java
public record TtsRequest(@NotBlank String text) {}
```

- [ ] **Step 2: OpenAiTtsClient 구현**

```java
// common/openai/OpenAiTtsClient.java
@Component
@RequiredArgsConstructor
public class OpenAiTtsClient {

    private final RestClient openAiRestClient;

    @Value("${openai.model.tts}")
    private String model;

    @Value("${openai.model.tts-voice}")
    private String voice;

    public byte[] synthesize(String text) {
        Map<String, Object> body = Map.of(
                "model", model,
                "input", text,
                "voice", voice
        );
        return openAiRestClient.post()
                .uri("/audio/speech")
                .body(body)
                .retrieve()
                .body(byte[].class);
    }
}
```

- [ ] **Step 3: TtsService 구현**

```java
// domain/speech/service/TtsService.java
@Service
@RequiredArgsConstructor
public class TtsService {
    private final OpenAiTtsClient ttsClient;

    public byte[] synthesize(String text) {
        return ttsClient.synthesize(text);
    }
}
```

- [ ] **Step 4: 빌드 확인 (SpeechController는 Task 7에서 이미 ttsService 주입 포함)**

```bash
./gradlew compileJava
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/potential/goodquestion/common/openai/OpenAiTtsClient.java \
        src/main/java/com/potential/goodquestion/domain/speech/service/TtsService.java \
        src/main/java/com/potential/goodquestion/domain/speech/dto/TtsRequest.java
git commit -m "feat: implement POST /speech/tts with OpenAI TTS API"
```

---

## Task 9: GET /reports/:sessionId

**Files:**
- Create: `domain/report/dto/ReportResponse.java`
- Create: `domain/report/service/ReportService.java`
- Create: `domain/report/controller/ReportController.java`

**Interfaces:**
- Consumes: `StorySession`, `Message`, `UtteranceAnalysis` (읽기 전용)
- Produces: `GET /reports/{sessionId}` → 말하기 역량 분석(논리/감정·공감/관계), 대표 발화 확인, 가정 학습 가이드

**역량 카테고리 매핑 (MVP 기준):**
- 논리: REASON, DECISION, SOLUTION, RESULT
- 감정·공감: EMOTION, EMPATHY
- 관점·관계: PERSPECTIVE, REQUEST

- [ ] **Step 1: ReportResponse DTO 생성**

```java
// domain/report/dto/ReportResponse.java
public record ReportResponse(
        Long sessionId,
        String storyTitle,
        String completedAt,
        ElementSummary elementSummary,
        List<SceneReport> scenes,
        List<RepresentativeUtterance> representativeUtterances,
        LearningGuide learningGuide
) {
    public record ElementSummary(
            List<String> accumulated,
            int totalRequired,
            double achievementRate,
            CategoryScore logic,       // REASON, DECISION, SOLUTION, RESULT
            CategoryScore empathy,     // EMOTION, EMPATHY
            CategoryScore perspective  // PERSPECTIVE, REQUEST
    ) {}

    public record CategoryScore(List<String> detected, int total) {
        public double rate() { return total == 0 ? 0 : (double) detected.size() / total; }
    }

    public record SceneReport(
            int sceneOrder,
            String characterName,
            int turnCount,
            String endReason,
            List<String> detectedElements
    ) {}

    public record RepresentativeUtterance(
            int sceneOrder,
            String text,
            List<String> elements
    ) {}

    public record LearningGuide(
            String summary,
            List<String> strengthElements,
            List<String> growthElements
    ) {}
}
```

- [ ] **Step 2: ReportService 구현**

```java
// domain/report/service/ReportService.java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final StorySessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final StorySceneRepository sceneRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> LOGIC_ELEMENTS = Set.of("REASON", "DECISION", "SOLUTION", "RESULT");
    private static final Set<String> EMPATHY_ELEMENTS = Set.of("EMOTION", "EMPATHY");
    private static final Set<String> PERSPECTIVE_ELEMENTS = Set.of("PERSPECTIVE", "REQUEST");

    public ReportResponse getReport(Long sessionId) {
        StorySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        List<StoryScene> scenes = sceneRepository.findByStoryIdOrderBySceneOrder(session.getStory().getId());
        List<UtteranceAnalysis> allAnalyses = analysisRepository.findByMessageSessionIdOrderByCreatedAtAsc(sessionId);
        List<Message> childMessages = messageRepository.findBySessionIdAndSpeakerTypeOrderByCreatedAtAsc(sessionId, SpeakerType.CHILD);

        // 누적 요소 집계
        Set<String> accumulated = new HashSet<>();
        for (UtteranceAnalysis a : allAnalyses) {
            parseElements(a.getDetectedElements()).forEach(e -> accumulated.add(((Map<?, ?>) e).get("type").toString()));
        }

        Set<String> allRequired = scenes.stream()
                .flatMap(s -> parseElements(s.getRequiredElements()).stream().map(Object::toString))
                .collect(Collectors.toSet());

        double achievementRate = allRequired.isEmpty() ? 0 : (double) accumulated.size() / allRequired.size();

        // 카테고리별 점수
        var logicScore = categoryScore(accumulated, LOGIC_ELEMENTS, allRequired);
        var empathyScore = categoryScore(accumulated, EMPATHY_ELEMENTS, allRequired);
        var perspectiveScore = categoryScore(accumulated, PERSPECTIVE_ELEMENTS, allRequired);

        // 장면별 리포트
        List<ReportResponse.SceneReport> sceneReports = scenes.stream().map(scene -> {
            List<UtteranceAnalysis> sceneAnalyses = analysisRepository
                    .findByMessageSessionIdAndMessageSceneId(sessionId, scene.getId());
            Set<String> sceneDetected = new HashSet<>();
            sceneAnalyses.forEach(a ->
                parseElements(a.getDetectedElements()).forEach(e -> sceneDetected.add(((Map<?, ?>) e).get("type").toString()))
            );
            int turnCount = (int) childMessages.stream()
                    .filter(m -> m.getScene().getId().equals(scene.getId())).count();
            return new ReportResponse.SceneReport(
                    scene.getSceneOrder(),
                    scene.getCharacterName(),
                    turnCount,
                    session.getSceneEndReason(),
                    new ArrayList<>(sceneDetected)
            );
        }).toList();

        // 대표 발화 (각 장면에서 가장 많은 요소가 탐지된 발화 1개)
        List<ReportResponse.RepresentativeUtterance> repUtterances = allAnalyses.stream()
                .filter(a -> !parseElements(a.getDetectedElements()).isEmpty())
                .map(a -> {
                    List<String> elements = parseElements(a.getDetectedElements()).stream()
                            .map(e -> ((Map<?, ?>) e).get("type").toString()).toList();
                    int sceneOrder = scenes.stream()
                            .filter(s -> s.getId().equals(a.getMessage().getScene().getId()))
                            .findFirst().map(StoryScene::getSceneOrder).orElse(0);
                    return new ReportResponse.RepresentativeUtterance(
                            sceneOrder, a.getMessage().getText(), elements);
                })
                .sorted((a, b) -> b.elements().size() - a.elements().size())
                .limit(3)
                .toList();

        // 학습 가이드
        Set<String> growthNeeded = new HashSet<>(allRequired);
        growthNeeded.removeAll(accumulated);
        ReportResponse.LearningGuide guide = new ReportResponse.LearningGuide(
                buildGuideSummary(accumulated, allRequired),
                new ArrayList<>(accumulated),
                new ArrayList<>(growthNeeded)
        );

        return new ReportResponse(
                sessionId,
                session.getStory().getTitle(),
                session.getCompletedAt() != null ? session.getCompletedAt().toString() : null,
                new ReportResponse.ElementSummary(
                        new ArrayList<>(accumulated),
                        allRequired.size(),
                        achievementRate,
                        logicScore, empathyScore, perspectiveScore
                ),
                sceneReports,
                repUtterances,
                guide
        );
    }

    private ReportResponse.CategoryScore categoryScore(Set<String> accumulated, Set<String> category, Set<String> required) {
        List<String> detected = accumulated.stream().filter(category::contains).toList();
        int total = (int) required.stream().filter(category::contains).count();
        return new ReportResponse.CategoryScore(detected, Math.max(total, category.size()));
    }

    private String buildGuideSummary(Set<String> accumulated, Set<String> required) {
        int rate = required.isEmpty() ? 0 : (int) ((double) accumulated.size() / required.size() * 100);
        return "이번 이야기에서 " + accumulated.size() + "가지 사고 요소를 표현했어요. (목표 달성률 " + rate + "%)";
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseElements(String json) {
        try {
            if (json == null || json.isBlank() || json.equals("[]")) return List.of();
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) { return List.of(); }
    }
}
```

- [ ] **Step 3: ReportController 구현**

```java
// domain/report/controller/ReportController.java
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getReport(sessionId)));
    }
}
```

- [ ] **Step 4: 빌드 확인**

```bash
./gradlew compileJava
```

- [ ] **Step 5: 전체 테스트 실행**

```bash
./gradlew test
```
Expected: PASS (컴파일 + 단위 테스트 전체)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/potential/goodquestion/domain/report/
git commit -m "feat: implement GET /reports/:sessionId with element analysis and learning guide"
```

---

## 자가 검토 (Spec Coverage)

| MVP 스펙 항목 | 커버 태스크 |
|---|---|
| POST /sessions/:id/utterances — 발화 저장 | Task 6 (childMessage 저장) |
| 사고 요소 추출 (LLM) | Task 4, Task 6 |
| evidence 원문 검증 후처리 | Task 3 (PostProcessor) |
| NORMAL/GUIDED/CLOSING 판정 | Task 3 (ProgressJudgeEngine) |
| 캐릭터 대사 생성 (LLM) | Task 5, Task 6 |
| CLOSING → 고정 대사 사용 | Task 6 |
| 다음 장면 이동 | Task 6 (advanceScene) |
| accumulatedElements 갱신 | Task 6 |
| POST /speech/stt (Whisper) | Task 7 |
| POST /speech/tts (OpenAI TTS) | Task 8 |
| GET /reports/:sessionId | Task 9 |
| 말하기 역량 분석 (논리/감정/관점) | Task 9 |
| 대표 발화 확인 | Task 9 |
| 가정 학습 가이드 | Task 9 |
| 토큰 절감 (CLOSING시 LLM 미호출) | Task 6 |
| gpt-5-mini 모델 사용 | Task 2 (설정), Task 4, Task 5 |

**타 팀원 담당 (이 플랜에 미포함):**
- `POST /stories/:id/sessions` — 김현정
- `GET /sessions/:id` — 김현정/이서우
- `POST /sessions/:id/post/order` — 이서우
- `POST /sessions/:id/post/retelling` — 이서우
- OAuth 인증 — 김현정
