package com.stalemated.lib.config.model;

import java.lang.reflect.Type;

public interface ConfigNode {
    /**
     * Gets the runtime type represented by this node.
     */
    Class<?> type();

    /**
     * Gets the generic type represented by this node.
     */
    Type genericType();
}
