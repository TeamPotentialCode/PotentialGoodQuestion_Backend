-- =====================================================================
-- 시드 데이터: 방귀 뀌는 며느리 (이야기 1편 + 장면 9개)
-- 대상 스키마: ddl-auto=update 로 엔티티에서 생성되는 stories / story_scenes
-- 실행: psql "$DB_URL" -f docs/seed/banggui_daughter_in_law.sql
--
-- [참고]
--  - PK는 IDENTITY(자동 증가)이므로 값을 직접 넣지 않고, 장면은 title 로 story_id 를 참조한다.
--  - 재실행 대비: 동일 제목의 이야기/장면을 먼저 삭제한다.
--  - image_url 은 자산 배포 위치에 맞춰 교체할 것(현재는 예시 URL).
--  - situation / child_role 은 콘텐츠 원문에 없어 임시 문구를 사용했다. [임시] 표기 참고.
--  - required_elements 는 서버 대화 엔진용 사고 요소 집합
--    (허용값: DECISION, REASON, PERSPECTIVE, SOLUTION, RESULT, EMOTION, EMPATHY, REQUEST).
--    콘텐츠 문서의 대화1 목표에 있던 "EXPRESSION" 은 표준 요소 집합에 없어 제외했다(확인 필요).
--  - element_criteria / remaining_worries 는 유도(GUIDED) 로직용이며 원문에 값이 없어 NULL 로 둔다(추후 보강).
--  - preferred_turns 는 원문에 없어 max_turns 보다 작은 합리값으로 임시 설정했다.
-- =====================================================================

BEGIN;

-- [사전 수정] 잔재 컬럼 stories.topic 처리
--  과거 엔티티의 'topic'(varchar 50) 이 'topics'(text) 로 변경됐으나, ddl-auto=update 는
--  컬럼을 삭제하지 않아 'topic NOT NULL' 이 남아 모든 이야기 INSERT 를 막는다.
--  현재 엔티티는 이 컬럼을 매핑하지 않으므로 NOT NULL 제약만 해제한다(비파괴/가역).
--  ▶ 권장: 팀 검토 후 완전 제거 → ALTER TABLE stories DROP COLUMN topic;
ALTER TABLE stories ALTER COLUMN topic DROP NOT NULL;

-- 기존 데이터 정리(재실행 안전)
DELETE FROM story_scenes
 WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리');
DELETE FROM stories WHERE title = '방귀 뀌는 며느리';

-- ── 이야기 ────────────────────────────────────────────────────────────
INSERT INTO stories (
    title, summary, difficulty, topics, thumbnail_url,
    introduction, situation, child_role, estimated_minutes,
    post_activity_config, status, created_at, updated_at
) VALUES (
    '방귀 뀌는 며느리',
    '큰 방귀를 부끄러워하던 며느리가 자신의 다름을 장점으로 바꾸는 이야기',
    '보통',
    '["다름","자기이해","장점 발견"]',
    'https://cdn.example.com/stories/banggui/thumbnail.png',
    '옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다. 며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다. 시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.',
    '[임시] 시댁에서 큰 방귀를 부끄러워하던 며느리가 방귀를 참다가 결국 온 마을 앞에서 크게 뀌게 되고, 그 방귀가 뜻밖에 모두를 돕게 되는 상황입니다.',
    '[임시] 아이는 며느리·시아버지·마을 이장과 차례로 대화하며 며느리의 마음을 이해하고, 며느리의 방귀를 좋은 일에 쓸 방법을 함께 찾는 역할을 맡습니다.',
    20,
    '{"cards":[{"id":"card_1","text":"며느리가 방귀를 참아 배가 아팠어요.","correct_order":1},{"id":"card_2","text":"참던 방귀가 크게 터져 시아버지가 놀랐어요.","correct_order":2},{"id":"card_3","text":"시아버지가 며느리를 친정에 데려가려 했어요.","correct_order":3},{"id":"card_4","text":"며느리의 방귀로 높은 배를 떨어뜨렸어요.","correct_order":4},{"id":"card_5","text":"시아버지가 사과하고 며느리는 당당해졌어요.","correct_order":5}],"retelling_keywords":["며느리","방귀","배나무","마을","특별한 힘"]}',
    'published',
    now(), now()
);

-- ── 장면 9개 ──────────────────────────────────────────────────────────
-- 공통: story_id 는 방금 삽입한 이야기의 id 를 title 로 참조

-- 장면 1) 도입 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 1,
    '옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다. 며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다. 시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.',
    'https://cdn.example.com/stories/banggui/scenes/1-intro.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 2) 전개1 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 2,
    '그래서 며느리는 방귀가 나오려고 할 때마다 꾹꾹 참았습니다. 하루도 참고, 이틀도 참고, 그렇게 오래 참다 보니 배는 점점 빵빵하게 부풀어 올랐고 얼굴은 노랗게 변했습니다. 몸도 마음도 너무 힘들었지만, 며느리는 차마 가족들에게 솔직하게 말하지 못했습니다.',
    'https://cdn.example.com/stories/banggui/scenes/2-dev1.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 3) 대화1 - 방귀쟁이 며느리
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 3,
    '며느리는 방귀를 뀌면 가족들이 자신을 이상하게 볼까 봐 걱정하며 사실을 말하지 못하고 있다.',
    'https://cdn.example.com/stories/banggui/scenes/3-talk1.png',
    '며느리가 방귀를 들키면 이상하게 보일까 봐 사실을 말하지 못하고 참는다.',
    '방귀쟁이 며느리',
    'ㅇㅇ아, 내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
    '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
    '며느리의 부끄러움과 걱정하는 마음을 이해하고, 참기만 하는 것 말고 할 수 있는 방법을 함께 생각한다.',
    '["PERSPECTIVE","EMOTION","SOLUTION"]', NULL, NULL,
    false, 2, 4, now(), now()
);

-- 장면 4) 전개2 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 4,
    '며느리는 더 이상 참을 수 없어 조심스럽게 살짝만 방귀를 뀌려 했지만, 오래 참았던 탓에 방귀가 크게 터져 나왔습니다. 마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다.',
    'https://cdn.example.com/stories/banggui/scenes/4-dev2.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 5) 대화2 - 시아버지
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 5,
    '요란한 방귀에 놀란 시아버지가 며느리를 나무라며 함께 살 수 없다고 말한다.',
    'https://cdn.example.com/stories/banggui/scenes/5-talk2.png',
    '시아버지가 며느리의 큰 방귀에 놀라 창피하다며 함께 살 수 없다고 한다.',
    '시아버지',
    '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
    '흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다.',
    '시아버지의 입장을 이해하면서도 며느리의 사정과 이유를 설명하고, 며느리를 이해해 달라고 요청한다.',
    '["PERSPECTIVE","EMPATHY","REASON","REQUEST"]', NULL, NULL,
    false, 3, 5, now(), now()
);

-- 장면 6) 전개3 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 6,
    '한참 걷다 보니 아랫마을 길가에 아주 높은 배나무가 서 있었습니다. 나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다. 마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다.',
    'https://cdn.example.com/stories/banggui/scenes/6-dev3.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 7) 대화3 - 마을 이장 (미션1 포함)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 7,
    '아무도 따지 못하는 높은 배나무 앞에서 마을 이장이 방법을 고민한다.',
    'https://cdn.example.com/stories/banggui/scenes/7-talk3.png',
    '높은 배나무의 배를 아무도 따지 못해 마을 사람들이 방법을 찾지 못하고 있다.',
    '마을 이장',
    '이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었단다. 무슨 뾰족한 방법이 없겠는가?',
    '아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!',
    '며느리의 방귀를 안전하게 사용해 높은 배를 떨어뜨릴 구체적인 방법을 제안한다.',
    '["SOLUTION","REASON","REQUEST","RESULT"]', NULL, NULL,
    true, 3, 5, now(), now()
);

-- 장면 8) 전개4 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 8,
    '시아버지는 며느리의 방귀가 시끄럽고 별난 것이 아니라 모두를 도울 수 있는 특별한 힘이라는 것을 깨닫습니다. 자신이 며느리를 구박했던 일을 후회하고 사과합니다.',
    'https://cdn.example.com/stories/banggui/scenes/8-dev4.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 9) 대화4 - 방귀쟁이 며느리 (미션2 포함)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 9,
    '자신의 특징이 누군가에게 도움이 될 수 있다는 것을 알게 된 며느리가 아이와 이야기한다.',
    'https://cdn.example.com/stories/banggui/scenes/9-talk4.png',
    '며느리가 자신의 특징을 여전히 부끄러워하며 받아들일지 망설인다.',
    '방귀쟁이 며느리',
    'ㅇㅇ이 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
    '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
    '숨기고 싶던 특징이 누군가에게 도움이 될 수 있음을 감정·관점·결과로 표현한다.',
    '["EMOTION","PERSPECTIVE","RESULT","SOLUTION"]', NULL, NULL,
    true, 2, 4, now(), now()
);

COMMIT;

-- 확인용 조회
-- SELECT id, title, status FROM stories WHERE title = '방귀 뀌는 며느리';
-- SELECT scene_order, character_name, has_mission, max_turns
--   FROM story_scenes
--  WHERE story_id = (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리')
--  ORDER BY scene_order;
