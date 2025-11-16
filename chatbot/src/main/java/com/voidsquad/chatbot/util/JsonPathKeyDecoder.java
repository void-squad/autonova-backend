package com.voidsquad.chatbot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simple path-driven decoder using JsonPath for extraction and Jackson for coercion.
 */

public class JsonPathKeyDecoder {
    private static final Logger log = LoggerFactory.getLogger(JsonPathKeyDecoder.class);

    private final ObjectMapper mapper;
    private final Configuration jsonPathConfig;

    public JsonPathKeyDecoder() {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        this.jsonPathConfig = Configuration.builder()
                .mappingProvider(new JacksonMappingProvider(mapper))
                .build();
    }

    /**
     * Decode json according to schema (dotted-path -> TypeDescriptor).
     */
    public DecodeResult decode(String json, Map<String, TypeDescriptor> schema) {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(schema, "schema");

        DecodeResult result = new DecodeResult();
        DocumentContext ctx = JsonPath.using(jsonPathConfig).parse(json);

        for (Map.Entry<String, TypeDescriptor> e : schema.entrySet()) {
            String dotted = e.getKey();
            TypeDescriptor td = e.getValue();

            String jsonPath = dotted.startsWith("$") ? dotted : "$." + dotted;
            try {
                if (td.isList()) {
                    List<?> rawList = ctx.read(jsonPath);
                    if (rawList == null) {
                        result.putValue(dotted, null);
                    } else {
                        List<Object> coerced = rawList.stream()
                                .map(it -> coercePrimitive(it, td.getPrimitive(), dotted, result))
                                .collect(Collectors.toList());
                        result.putValue(dotted, coerced);
                    }
                } else {
                    Object raw = ctx.read(jsonPath);
                    Object coerced = coercePrimitive(raw, td.getPrimitive(), dotted, result);
                    result.putValue(dotted, coerced);
                }
            } catch (com.jayway.jsonpath.PathNotFoundException pnfe) {
                // missing path -> null (no error by default)
                log.debug("path not found: {}", jsonPath);
                result.putValue(dotted, null);
            } catch (Exception ex) {
                log.warn("decode error for {}: {}", dotted, ex.getMessage());
                result.putError(dotted, "decode error: " + ex.getMessage());
            }
        }
        return result;
    }

    private Object coercePrimitive(Object raw, TypeDescriptor.Primitive prim, String path, DecodeResult result) {
        if (raw == null) return null;
        try {
            switch (prim) {
                case STRING:
                    return mapper.convertValue(raw, String.class);
                case LONG:
                    if (raw instanceof Number) return ((Number) raw).longValue();
                    String s = mapper.convertValue(raw, String.class);
                    return Long.parseLong(s.trim());
                case DOUBLE:
                    if (raw instanceof Number) return ((Number) raw).doubleValue();
                    String ds = mapper.convertValue(raw, String.class);
                    return Double.parseDouble(ds.trim());
                case BOOLEAN:
                    if (raw instanceof Boolean) return raw;
                    String bs = mapper.convertValue(raw, String.class);
                    return Boolean.parseBoolean(bs.trim());
                case INSTANT:
                    if (raw instanceof Number) {
                        long v = ((Number) raw).longValue();
                        if (String.valueOf(v).length() <= 10) return Instant.ofEpochSecond(v);
                        return Instant.ofEpochMilli(v);
                    }
                    String ts = mapper.convertValue(raw, String.class);
                    return Instant.parse(ts);
                case OBJECT:
                case MAP:
                    return mapper.convertValue(raw, Map.class);
                default:
                    return mapper.convertValue(raw, Object.class);
            }
        } catch (Exception ex) {
            log.warn("coercion failed for {} at {}: {}", prim, path, ex.getMessage());
            result.putError(path, "coercion failed for " + prim + " : " + ex.getMessage());
            return null;
        }
    }
}
