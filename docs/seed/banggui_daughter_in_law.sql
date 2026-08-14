-- =====================================================================
-- 시드 데이터: 방귀 뀌는 며느리 (이야기 1편 + 장면 10개)
-- 노션 MVP 콘텐츠) 방귀 뀌는 며느리 (1) 전체 내용 반영
-- 대상 스키마: ddl-auto=update 로 엔티티에서 생성되는 stories / story_scenes
-- 실행: psql "$DB_URL" -f docs/seed/banggui_daughter_in_law.sql
--
-- [변경 이력]
--  - 초안 (이서우): 9개 장면
--  - 2차: 장면 scene_description 줄거리 원문 전체 반영
--          결과 전개 장면(scene_order=8, "방귀 나갑니다!") 신규 추가 → 총 10개 장면
--          대화1 required_elements REASON 추가
--          대화2 required_elements 노션 장면 구성 테이블 기준으로 수정
--          전개4·대화4 scene_order 8·9 → 9·10
-- =====================================================================

BEGIN;

-- stories.topic 컬럼이 NOT NULL 로 남아있어 INSERT를 막으므로 제약만 해제
ALTER TABLE stories ALTER COLUMN topic DROP NOT NULL;

-- 기존 데이터 정리 (재실행 안전, 외래키 참조 순서대로 삭제)
DELETE FROM post_activity_results
 WHERE session_id IN (
     SELECT id FROM story_sessions
      WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리')
 );
DELETE FROM activities
 WHERE session_id IN (
     SELECT id FROM story_sessions
      WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리')
 );
DELETE FROM messages
 WHERE session_id IN (
     SELECT id FROM story_sessions
      WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리')
 );
DELETE FROM story_sessions
 WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리');
DELETE FROM story_scenes
 WHERE story_id IN (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리');
DELETE FROM stories WHERE title = '방귀 뀌는 며느리';

-- ── 이야기 ──────────────────────────────────────────────────────────
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

-- ── 장면 10개 ─────────────────────────────────────────────────────────

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
    '내 방귀가 너무 크다는 걸 알면 나를 이상하게 생각하지 않을까?
며느리는 걱정이 많았습니다. 사실 방귀는 누구에게나 나오는 자연스러운 일이지만, 며느리에게는 그것이 큰 비밀처럼 느껴졌습니다. 특히 자신의 방귀는 한 번 나오면 지붕이 흔들릴 만큼 우렁찼기 때문에 더욱 부끄러웠습니다.',
    'https://cdn.example.com/stories/banggui/scenes/3-talk1.png',
    '며느리가 방귀를 들키면 이상하게 보일까 봐 사실을 말하지 못하고 참는다.',
    '방귀쟁이 며느리',
    'ㅇㅇ아, 내 방귀가 너무 크다는 걸 알면 가족들이 나를 이상하게 생각하지 않을까?',
    '그래도 아직은 못 말하겠어. 조금만 더 참아 볼게.',
    '방귀를 숨기고 싶어하는 며느리의 입장을 이해하고, 공감해주며 문제를 숨기지 않고 솔직하게 말할 수 있는 용기를 준다.',
    '["PERSPECTIVE","EMOTION","REASON","SOLUTION"]', NULL, NULL,
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
    '그러던 어느 날, 며느리는 더 이상 참을 수 없었습니다. 배가 너무 아프고 숨 쉬기도 힘들었습니다. 며느리는 조심스럽게 가족들에게 말했습니다.
"저… 사실은 방귀를 너무 오래 참아서 배가 아파요. 조금만 뀌어도 될까요?"
며느리는 아주 살짝만 뀌려고 했습니다. 하지만 그동안 너무 오래 참았던 탓에 방귀는 생각보다 훨씬 크게 터져 나왔습니다. 마당의 먼지가 휘리릭 날아가고, 기왓장이 달그락거리고, 시아버지의 갓까지 휙 날아가 버렸습니다.',
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
    '시아버지는 깜짝 놀라 화를 냈습니다.
"이게 무슨 일이냐! 며느리가 이렇게 요란한 방귀를 뀌다니, 우리 집안이 다 흔들리는구나!"
며느리는 고개를 푹 숙였습니다. 일부러 그런 것이 아니었지만, 모두가 놀란 모습을 보니 마음이 더 작아졌습니다. 시아버지는 며느리의 방귀가 너무 별나다며, 이런 며느리와는 함께 살 수 없다고 말했습니다.
며느리는 슬펐습니다. 자신이 가족에게 피해만 주는 사람처럼 느껴졌기 때문입니다. 하지만 며느리의 방귀가 정말 쓸모없는 것인지는 아직 아무도 알지 못했습니다.
결국 시아버지는 방귀 뀌는 며느리를 데리고 친정에 데려다주러 길을 나섰습니다.',
    'https://cdn.example.com/stories/banggui/scenes/5-talk2.png',
    '시아버지가 며느리의 큰 방귀에 놀라 창피하다며 함께 살 수 없다고 한다.',
    '시아버지',
    '아이고, 이게 무슨 일이냐! 우리 집안이 다 흔들리는구나! 이렇게 창피한 며느리와 함께 못 살겠다! 그렇지 않니?',
    '흥, 그래도 도저히 이런 며느리와는 함께 살 수 없으니 친정으로 데려다줘야겠다.',
    '시아버지가 놀란 마음을 이해하면서도, 며느리가 일부러 그런 것이 아니라 오래 참아서 힘들었던 것임을 말하고, 며느리를 따뜻하게 이해해 달라고 설득한다.',
    '["PERSPECTIVE","EMOTION","REASON","SOLUTION"]', NULL, NULL,
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
    '그런데 한참 걷다 보니 길가에 아주 높은 배나무가 한 그루 서 있었습니다. 나무 꼭대기에는 노랗고 탐스러운 배들이 주렁주렁 매달려 있었습니다.
시아버지는 배를 보자 군침이 돌았습니다.
"참 맛있어 보이는 배로구나. 그런데 너무 높아서 딸 수가 없겠네."
마을 사람들도 그 배를 먹고 싶어 했지만, 나무가 너무 높아 아무도 딸 수 없었습니다. 긴 장대를 가져와도 닿지 않았고, 나무에 올라가려 해도 가지가 너무 높았습니다.',
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
    '그때 며느리는 문득 생각했습니다.
내 방귀가 지붕도 흔들 만큼 힘이 세다면, 저 높은 배를 떨어뜨릴 수도 있지 않을까?
며느리는 조심스럽게 시아버지에게 말했습니다.
"아버님, 제가 한번 해 볼게요. 대신 사람들이 다치지 않도록 모두 조금 떨어져 주세요."',
    'https://cdn.example.com/stories/banggui/scenes/7-talk3.png',
    '높은 배나무의 배를 아무도 따지 못해 마을 사람들이 방법을 찾지 못하고 있다.',
    '마을 이장',
    '이 배나무는 해마다 탐스러운 배가 열리지만, 너무 높아서 아무도 딸 수가 없었단다. 무슨 뾰족한 방법이 없겠는가?',
    '아이고, 방귀 뀌는 며느리 덕분에 온 마을이 배 잔치를 할 수 있겠구려, 고맙소!',
    '며느리의 방귀를 안전하게 사용해 높은 배를 떨어뜨릴 구체적인 방법을 제안한다.',
    '["SOLUTION","REASON","REQUEST","RESULT"]', NULL, NULL,
    true, 3, 5, now(), now()
);

-- 장면 8) 결과 전개 - 방귀로 배 따기 결과 (내레이션, 신규 추가)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 8,
    '마을 사람들은 처음에는 어리둥절했습니다. 하지만 며느리는 나무를 향해 자리를 잡고, 배가 떨어질 곳을 살폈습니다. 사람들이 없는 쪽으로 몸을 돌리고, 배나무 위쪽을 향해 힘을 모았습니다.
그리고 크게 외쳤습니다.
"방귀 나갑니다!"
곧이어 천둥 같은 방귀 소리가 울려 퍼졌습니다. 바람이 세차게 불더니 높은 나무에 매달려 있던 배들이 우수수 떨어졌습니다. 사람들은 깜짝 놀라면서도 곧 기뻐했습니다. 아무도 따지 못했던 배가 마당 가득 떨어졌기 때문입니다.
시아버지도 떨어진 배를 하나 먹어 보았습니다. 배는 달고 시원했습니다. 마을 사람들도 배를 나누어 먹으며 즐거워했습니다. 모두가 배불리 먹고 나자, 시아버지는 며느리를 다시 바라보았습니다.
처음에는 시끄럽고 별나다고만 생각했던 며느리의 방귀가, 알고 보니 모두를 도울 수 있는 특별한 힘이었던 것입니다.',
    'https://cdn.example.com/stories/banggui/scenes/8-result.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 9) 전개4 (내레이션)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 9,
    '시아버지는 며느리에게 미안한 마음이 들었습니다.
"내가 네 모습을 제대로 보지 못했구나. 남들과 다르다고 해서 부끄러운 것이 아닌데, 내가 너무 성급하게 생각했다."',
    'https://cdn.example.com/stories/banggui/scenes/9-dev4.png',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    false, 0, 0, now(), now()
);

-- 장면 10) 대화4 - 방귀쟁이 며느리 (미션2 포함)
INSERT INTO story_scenes (
    story_id, scene_order, scene_description, image_url, conflict,
    character_name, character_opening, character_closing, scene_goal,
    required_elements, element_criteria, remaining_worries,
    has_mission, preferred_turns, max_turns, created_at, updated_at
) VALUES (
    (SELECT id FROM stories WHERE title = '방귀 뀌는 며느리'), 10,
    '며느리는 그 말을 듣고 마음이 조금씩 편안해졌습니다. 자신이 숨기고 싶어 했던 특징이 누군가에게 도움이 될 수도 있다는 것을 알게 되었기 때문입니다.
그 뒤로 며느리는 더 이상 방귀를 무조건 참지 않았습니다. 물론 아무 때나 함부로 뀌지는 않았습니다. 대신 몸이 힘들 때는 솔직하게 말하고, 사람들이 놀라지 않도록 미리 알려 주었습니다.
마을 사람들도 며느리를 놀리지 않았습니다. 오히려 높은 나무의 열매를 딸 때나, 큰 바람이 필요할 때 며느리에게 도움을 부탁했습니다. 며느리는 자신의 방귀를 부끄러운 비밀이 아니라, 잘 쓰면 모두에게 도움이 되는 특별한 힘으로 여기게 되었습니다.',
    'https://cdn.example.com/stories/banggui/scenes/10-talk4.png',
    '며느리가 자신의 특징을 여전히 부끄러워하며 받아들일지 망설인다.',
    '방귀쟁이 며느리',
    'ㅇㅇ이 덕분에 내 방귀가 누군가에게 도움이 될 수 있다는 걸 처음 알았어. 이제는 방귀 소리가 큰 걸 부끄러워하지 않아도 될까?',
    '이제는 부끄러워하며 숨기지 않고, 조심해서 좋은 일에 써 볼게.',
    '다름을 인정하고, 자신의 특징을 긍정적으로 받아들이는 태도를 말한다.',
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
