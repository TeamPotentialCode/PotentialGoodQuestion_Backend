package com.potential.goodquestion.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonUtils {

    private final ObjectMapper objectMapper;

    public Set<String> toStringSet(String json) {
        try {
            if (isEmpty(json)) return new HashSet<>();
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            return new HashSet<>(list);
        } catch (Exception e) { return new HashSet<>(); }
    }

    public List<String> toStringList(String json) {
        try {
            if (isEmpty(json)) return List.of();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) { return List.of(); }
    }

    public Map<String, String> toStringMap(String json) {
        try {
            if (isEmpty(json)) return new HashMap<>();
            Map<String, String> result = objectMapper.readValue(json, new TypeReference<>() {});
            // JSON null ("null" 문자열) 파싱 시 readValue가 null을 반환할 수 있음
            return result != null ? result : new HashMap<>();
        } catch (Exception e) { return new HashMap<>(); }
    }

    public List<String> toDetectedTypes(String json) {
        try {
            if (isEmpty(json)) return List.of();
            List<Map<String, String>> elements = objectMapper.readValue(json, new TypeReference<>() {});
            return elements.stream()
                    .map(e -> e.get("type"))
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
        } catch (Exception e) { return List.of(); }
    }

    public String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "[]"; }
    }

    private boolean isEmpty(String json) {
        return json == null || json.isBlank() || "[]".equals(json) || "{}".equals(json);
    }
}
