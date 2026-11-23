package com.authoritypro.platform.audit.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Flat, allowlisted metadata only: 20 entries and 4 KiB of canonical JSON at most. */
public final class AuditMetadata {
    public static final int MAX_ENCODED_BYTES = 4096;
    public static final int MAX_ENTRIES = 20;
    private static final Pattern KEY = Pattern.compile("[A-Za-z][A-Za-z0-9]{0,63}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,63}");
    private static final Pattern TEXT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:@/-]{0,127}");
    private static final Pattern DIGEST = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]{10,}\\.");
    private static final Set<String> NUMBER_KEYS = Set.of(
            "revision", "fromRevision", "toRevision", "removedCount", "policyRevision");
    private static final Set<String> ARRAY_KEYS = Set.of("changedFields", "changedKeys", "keyNames");
    private static final Set<String> INSTANT_KEYS = Set.of("windowStart", "windowEnd");

    private final Map<String, Object> values;
    private final String encoded;

    private AuditMetadata(Map<String, Object> values, String encoded) {
        this.values = values;
        this.encoded = encoded;
    }

    public static AuditMetadata of(AuditAction action, Map<String, Object> input) {
        if (action == null || input == null || input.size() > MAX_ENTRIES
                || !input.keySet().equals(action.requiredMetadata())) {
            throw rejected();
        }
        Map<String, Object> normalized = new TreeMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            String key = entry.getKey();
            if (key == null || !KEY.matcher(key).matches()) throw rejected();
            normalized.put(key, normalize(key, entry.getValue()));
        }
        if (action == AuditAction.AUDIT_RETENTION_EXECUTED) {
            Instant start = Instant.parse((String) normalized.get("windowStart"));
            Instant end = Instant.parse((String) normalized.get("windowEnd"));
            if (!start.isBefore(end)) throw rejected();
        }
        String encoded = encode(normalized);
        if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_ENCODED_BYTES) throw rejected();
        return new AuditMetadata(Collections.unmodifiableMap(new LinkedHashMap<>(normalized)), encoded);
    }

    public Map<String, Object> values() {
        return values;
    }

    public String encoded() {
        return encoded;
    }

    private static Object normalize(String key, Object value) {
        if (ARRAY_KEYS.contains(key)) return names(value);
        if (NUMBER_KEYS.contains(key)) return number(key, value);
        if (INSTANT_KEYS.contains(key)) return instant(value);
        if (value instanceof String text) {
            if (!TEXT.matcher(text).matches() || sensitive(text)) throw rejected();
            if ("descriptorDigest".equals(key) && !DIGEST.matcher(text).matches()) throw rejected();
            return text;
        }
        throw rejected();
    }

    private static List<String> names(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty() || list.size() > 32) throw rejected();
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof String name) || !NAME.matcher(name).matches()) throw rejected();
            result.add(name);
        }
        return List.copyOf(result);
    }

    private static Integer number(String key, Object value) {
        if (!(value instanceof Integer || value instanceof Long || value instanceof BigDecimal)) throw rejected();
        BigDecimal decimal = new BigDecimal(value.toString());
        if (decimal.stripTrailingZeros().scale() > 0 || decimal.toBigIntegerExact().bitLength() > 31) throw rejected();
        if (decimal.signum() < 0 || ("removedCount".equals(key) ? false : decimal.signum() == 0)) {
            throw rejected();
        }
        return decimal.intValueExact();
    }

    private static String instant(Object value) {
        if (!(value instanceof String text) || !TEXT.matcher(text).matches()) throw rejected();
        try {
            return Instant.parse(text).toString();
        } catch (DateTimeParseException failure) {
            throw rejected();
        }
    }

    private static boolean sensitive(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        return lower.contains("bearer ") || lower.contains("token=") || lower.contains("password=")
                || lower.contains("secret=") || JWT.matcher(text).find();
    }

    private static String encode(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            quote(json, entry.getKey());
            json.append(':');
            Object value = entry.getValue();
            if (value instanceof String text) {
                quote(json, text);
            } else if (value instanceof Boolean bool) {
                json.append(bool);
            } else if (value instanceof Number number) {
                json.append(number);
            } else {
                json.append('[');
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(',');
                    quote(json, (String) list.get(i));
                }
                json.append(']');
            }
        }
        return json.append('}').toString();
    }

    private static void quote(StringBuilder json, String text) {
        json.append('"');
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }

    private static AuditRejectedException rejected() {
        return new AuditRejectedException(AuditErrorCode.AUDIT_METADATA_REJECTED);
    }
}
