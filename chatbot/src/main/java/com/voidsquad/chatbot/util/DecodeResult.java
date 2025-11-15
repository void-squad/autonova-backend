package com.voidsquad.chatbot.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds decoded values and per-path errors.
 */
public final class DecodeResult {
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();

    public void putValue(String path, Object v) { values.put(path, v); }
    public void putError(String path, String err) { errors.put(path, err); }

    public Map<String, Object> getValues() { return Collections.unmodifiableMap(values); }
    public Map<String, String> getErrors() { return Collections.unmodifiableMap(errors); }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
