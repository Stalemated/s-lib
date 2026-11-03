package com.stalemated.lib.config.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

public record CategoryNode(Field field, Class<?> type, Type genericType, Map<String, ConfigNode> children, String title, String comment, Constraint constraint) implements ConfigNode {

    public CategoryNode {
        children = Collections.unmodifiableMap(children);
    }

    public ConfigNode getChild(String key) {
        return children.get(key);
    }

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }
}
