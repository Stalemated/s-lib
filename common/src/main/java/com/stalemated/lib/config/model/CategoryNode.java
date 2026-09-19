package com.stalemated.lib.config.model;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public record CategoryNode(Class<?> type, Type genericType, Map<String, ConfigNode> children) implements ConfigNode {
    public CategoryNode(Class<?> type, Type genericType, Map<String, ConfigNode> children) {
        this.type = type;
        this.genericType = genericType;
        this.children = Collections.unmodifiableMap(children);
    }

    public ConfigNode getChild(String key) {
        return children.get(key);
    }
}
