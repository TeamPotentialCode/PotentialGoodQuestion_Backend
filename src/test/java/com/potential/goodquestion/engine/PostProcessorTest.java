package com.potential.goodquestion.engine;

import com.potential.goodquestion.common.engine.PostProcessor;
import com.potential.goodquestion.common.engine.vo.DetectedElement;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

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
    void 동일_type_중복시_첫번째만_유지() {
        var elements = List.of(
                new DetectedElement("REASON", "창피해서"),
                new DetectedElement("REASON", "부끄러워서")
        );
        var result = processor.process(elements, "창피해서 부끄러워서 참았어요");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).evidence()).isEqualTo("창피해서");
    }

    @Test
    void 스키마에_없는_요소_제거() {
        var elements = List.of(new DetectedElement("UNKNOWN_TYPE", "참았어요"));
        var result = processor.process(elements, "참았어요");
        assertThat(result).isEmpty();
    }

    @Test
    void 서로_다른_type은_모두_유지() {
        var elements = List.of(
                new DetectedElement("REASON", "창피해서"),
                new DetectedElement("PERSPECTIVE", "가족들이 이상하게 볼까봐")
        );
        var result = processor.process(elements, "창피해서 가족들이 이상하게 볼까봐 참았어요");
        assertThat(result).hasSize(2);
    }
}
