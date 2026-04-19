package com.chroniccare.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        if (value == null) return null;
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> toStringMap(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyMap();

        try {
            return MAPPER.readValue(raw, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception ignored) {
            // fallthrough
        }

        return parseJavaMapString(raw);
    }

    private static Map<String, String> parseJavaMapString(String raw) {
        String s = raw.trim();
        if (!s.startsWith("{") || !s.endsWith("}")) return Collections.emptyMap();

        String body = s.substring(1, s.length() - 1).trim();
        if (body.isEmpty()) return Collections.emptyMap();

        Map<String, String> map = new LinkedHashMap<>();
        for (String part : body.split(",\\s*")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;

            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            map.put(key, value);
        }
        return map;
    }
}

