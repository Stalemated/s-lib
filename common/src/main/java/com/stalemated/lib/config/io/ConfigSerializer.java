package com.stalemated.lib.config.io;

import com.stalemated.lib.config.io.record.DeserializationResult;
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
}
