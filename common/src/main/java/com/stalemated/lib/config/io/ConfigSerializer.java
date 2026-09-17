package com.stalemated.lib.config.io;

import com.stalemated.lib.config.io.record.DeserializationResult;

import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * Defines a contract for converting config instances to and from text formats.
 *
 * @param <T> The config data model class.
 */
public interface ConfigSerializer<T> {

    /**
     * Serializes the config instance into a formatted string.
     *
     * @param instance The config instance to serialize.
     * @return The formatted text representation of the config.
     */
    String serialize(T instance);

    /**
     * Deserializes text content into a config instance.
     *
     * @param content The raw text content.
     * @param defaultFactory Factory to create the default instance if content is empty or corrupt.
     * @return Result containing the deserialized instance and whether a disk resave is required.
     */
    DeserializationResult<T> deserialize(String content, Supplier<T> defaultFactory);

    /**
     * Deserializes a specific option value from a raw AST node.
     * Useful for network synchronization where only specific fields are transmitted.
     *
     * @param rawAstNode The raw AST element (e.g., JsonElement)
     * @param targetType The generic type to deserialize into
     * @return The deserialized Java object
     * @throws Exception If deserialization fails
     */
    Object deserializeType(Object rawAstNode, Type targetType) throws Exception;
}
