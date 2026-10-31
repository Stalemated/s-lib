package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonElement;
import blue.endless.jankson.JsonGrammar;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.io.exception.ConfigSyntaxException;
import com.stalemated.lib.config.io.DeserializationResult;
import com.stalemated.lib.config.validation.ConfigValidator;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Pure JSON5 serializer that supports line comments via {@link Comment},
 * arbitrary recursive nesting via {@link Nest}, and delegates strict
 * validation/auto-migration to internal enforcers.
 *
 * @param <T> The config data model class.
 */
public class Json5Serializer<T> implements ConfigSerializer<T> {

    private final OptionTree optionTree;
    private final Jankson jankson;
    private final JsonGrammar grammar;
    private final Gson gson;
    private final Json5KeyLayout layout;
    private final OptionValueReader reader;

    public Json5Serializer(OptionTree optionTree) {
        this(optionTree, null);
    }

    public Json5Serializer(OptionTree optionTree, Consumer<GsonBuilder> gsonCustomizer) {
        this.optionTree = optionTree;

        // Jankson used as preprocessor
        this.jankson = Jankson.builder().build();

        GsonBuilder gBuilder = new GsonBuilder();
        SLibGsonDefaults.apply(gBuilder);
        if (gsonCustomizer != null) {
            gsonCustomizer.accept(gBuilder);
        }
        this.gson = gBuilder.create();

        this.layout = new Json5KeyLayout(optionTree, gson.fieldNamingStrategy());
        this.reader = new OptionValueReader(gson, layout::jsonName);

        this.grammar = JsonGrammar.builder()
                .withComments(true)
                .printWhitespace(true)
                .build();
    }

    /**
     * Serializes the config instance into formatted JSON5 with comments,
     * traversing nested structures recursively.
     *
     * @param instance The config instance to serialize.
     * @return Formatted JSON5 string.
     */
    @Override
    public String serialize(T instance) {
        com.google.gson.JsonObject tree = new com.google.gson.JsonObject();
        for (OptionInfo option : optionTree.all()) {
            layout.put(tree, option, gson.toJsonTree(option.getValue(instance), option.genericType()));
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(gson.toJson(tree));
        } catch (SyntaxError e) {
            throw new IllegalStateException("Jankson rejected JSON produced by GSON: " + e.getCompleteMessage(), e);
        }

        Json5CommentInjector.inject(rootObject, optionTree.getSchemaRoot(), layout::jsonName);
        return rootObject.toJson(grammar);
    }

    /**
     * Deserializes JSON5 text into a config instance, clamping numeric bounds and
     * detecting schema migrations across all nesting levels based on the OptionTree.
     *
     * @param json5Content The raw JSON5 string content.
     * @param defaultFactory Factory for the instance that the valid values are read into.
     * @return Result containing the deserialized instance, whether a migration is required,
     * and the entries that were rejected or are not options of this config.
     * @throws ConfigSyntaxException If the content is not a valid JSON5.
     */
    @Override
    public DeserializationResult<T> deserialize(String json5Content, Supplier<T> defaultFactory) {
        T instance = defaultFactory.get();
        if (json5Content == null || json5Content.isBlank()) {
            return new DeserializationResult<>(instance, true, List.of(), List.of());
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(json5Content);
        } catch (SyntaxError e) {
            throw new ConfigSyntaxException(e.getCompleteMessage(), e);
        }

        LoadReport report = new LoadReport();
        layout.reportUnexpectedKeys(rootObject, report);
        for (OptionInfo option : optionTree.all()) {
            readOption(instance, rootObject, option, report);
        }

        if (ConfigValidator.validate(instance, optionTree.getSchemaRoot())) {
            report.requireMigration();
        }
        return report.toResult(instance);
    }

    @Override
    public String serializeOption(Object value) {
        return gson.toJson(value);
    }

    @Override
    public Object deserializeOption(String rawJson, Type targetType) {
        return gson.fromJson(rawJson, targetType);
    }

    private void readOption(T instance, JsonObject rootObject, OptionInfo option, LoadReport report) {
        JsonElement raw = layout.find(rootObject, option);
        if (raw == null) {
            report.requireMigration();
            return;
        }

        String path = layout.pathOf(option);
        try {
            Object value = reader.read(option, path, raw, report);
            option.setValue(instance, value);
            if (!Objects.equals(value, option.getValue(instance))) {
                report.requireMigration();
            }
        } catch (RuntimeException e) {
            report.reject(path, raw, e);
        }
    }
}
