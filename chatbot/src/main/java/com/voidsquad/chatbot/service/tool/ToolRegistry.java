package com.voidsquad.chatbot.service.tool;

import com.voidsquad.chatbot.service.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of available tools. Populated from Spring's application context by autowiring all Tool beans.
 */
@Component
public class ToolRegistry {
    private final Map<String, Tool> registry;

    /**
     * Construct a registry from the given list of Tool beans. Spring will supply all Tool beans
     * automatically when this class is a component and Tool implementations are components.
     * The resulting registry is immutable to reflect a static set of available tools at startup.
     */
    public ToolRegistry(List<Tool> tools) {
        Map<String, Tool> map = new HashMap<>();
        if (tools != null) {
            for (Tool t : tools) {
                if (t != null && t.name() != null) {
                    map.put(t.name(), t);
                }
            }
        }
        this.registry = Collections.unmodifiableMap(map);
    }

    /**
     * Lookup a tool by its name. Returns Optional.empty() if not found.
     */
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(registry.get(name));
    }

    /**
     * Return an unmodifiable view of all registered tools.
     */
    public Map<String, Tool> all() { return registry; }
}
