package com.stalemated.lib.config.io;

import com.stalemated.lib.config.io.exception.ConfigSyntaxException;

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
     * @param defaultFactory Factory for the instance that the valid values are read into.
     * @return Result containing the deserialized instance, the values that were rejected,
     * and whether a disk re-save is required.
     * @throws ConfigSyntaxException If the content cannot be parsed at all. Callers rely on this
     * exception to tell an unreadable file apart from one with invalid values.
     */
    DeserializationResult<T> deserialize(String content, Supplier<T> defaultFactory);

    /**
     * Serializes a specific option value into a raw JSON string.
     * Useful for network synchronization where only specific fields are transmitted.
     *
     * @param value The value to serialize
     * @return The serialized JSON string
     */
    String serializeOption(Object value);

    /**
     * Deserializes a specific option value from a raw JSON string.
     * Useful for network synchronization where only specific fields are transmitted.
     *
     * @param rawJson The raw JSON string representing the field's value
     * @param targetType The generic type to deserialize into
     * @return The deserialized Java object
     * @throws Exception If deserialization fails
     */
    Object deserializeOption(String rawJson, Type targetType) throws Exception;
}
