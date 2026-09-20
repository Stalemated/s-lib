package com.stalemated.lib.config.io.json5;

import blue.endless.jankson.*;
import blue.endless.jankson.api.SyntaxError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stalemated.lib.config.annotation.Comment;
import com.stalemated.lib.config.annotation.Nest;
import com.stalemated.lib.config.io.ConfigSerializer;
import com.stalemated.lib.config.io.DeserializationResult;
import com.stalemated.lib.config.validation.ConfigValidator;
import com.stalemated.lib.config.model.OptionInfo;
import com.stalemated.lib.config.model.OptionTree;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
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

    private final Class<T> configClass;
    private final OptionTree optionTree;
    private final Jankson jankson;
    private final JsonGrammar grammar;
    private final Json5SchemaValidator schemaValidator;
    private final Json5SchemaEnforcer<T> schemaEnforcer;
    private final Gson gson;

    public Json5Serializer(Class<T> configClass, OptionTree optionTree) {
        this(configClass, optionTree, null, null, null, null);
    }

    public Json5Serializer(
            Class<T> configClass,
            OptionTree optionTree,
            Consumer<Jankson.Builder> janksonCustomizer,
            Consumer<GsonBuilder> gsonCustomizer,
            String modId,
            Logger logger
    ) {
        this.configClass = configClass;
        this.optionTree = optionTree;
        this.schemaValidator = new Json5SchemaValidator(optionTree);
        
        Jankson.Builder jBuilder = Jankson.builder();
        SLibJanksonDefaults.apply(jBuilder, modId, logger);
        if (janksonCustomizer != null) {
            janksonCustomizer.accept(jBuilder);
        }
        this.jankson = jBuilder.build();

        GsonBuilder gBuilder = new GsonBuilder();
        SLibGsonDefaults.apply(gBuilder, modId, logger);
        if (gsonCustomizer != null) {
            gsonCustomizer.accept(gBuilder);
        }
        this.gson = gBuilder.create();

        this.grammar = JsonGrammar.builder()
                .withComments(true)
                .printWhitespace(true)
                .build();
                
        this.schemaEnforcer = new Json5SchemaEnforcer<>(optionTree, jankson, schemaValidator, gson);
    }

    public Jankson getJankson() {
        return jankson;
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
        JsonElement element = jankson.toJson(instance);
        if (!(element instanceof JsonObject rootObject)) {
            return element.toJson(grammar);
        }

        formatAndCleanAst("", rootObject);
        Json5CommentInjector.inject(rootObject, optionTree.getSchemaRoot());
        return rootObject.toJson(grammar);
    }

    /**
     * Deserializes JSON5 text into a config instance, clamping numeric bounds and
     * detecting schema migrations across all nesting levels based on the OptionTree.
     *
     * @param json5Content The raw JSON5 string content.
     * @param defaultFactory Factory to create the default instance if content is empty or corrupt.
     * @return Result containing the deserialized instance and whether disk resave is required.
     */
    @Override
    public DeserializationResult<T> deserialize(String json5Content, Supplier<T> defaultFactory) {
        if (json5Content == null || json5Content.trim().isEmpty()) {
            return new DeserializationResult<>(defaultFactory.get(), true, false);
        }

        JsonObject rootObject;
        try {
            rootObject = jankson.load(json5Content);
        } catch (SyntaxError e) {
            throw new RuntimeException("Syntax error while parsing JSON5 config: " + e.getMessage(), e);
        }

        T instance = gson.fromJson(rootObject.toJson(false, false), configClass);
        if (instance == null) instance = defaultFactory.get();
        boolean mutatedByValidator = ConfigValidator.validate(instance, optionTree.getSchemaRoot());

        DeserializationResult<T> result = schemaEnforcer.enforce(rootObject, instance);
        
        return new DeserializationResult<>(
            result.instance(),
            result.requiresSave() || mutatedByValidator,
            result.partialCorruptionDetected()
        );
    }

    @Override
    public Object deserializeType(Object rawAstNode, Type targetType) throws IllegalArgumentException {
        if (rawAstNode instanceof JsonElement elem) {
            return gson.fromJson(elem.toJson(false, false), targetType);
        }
        throw new IllegalArgumentException("Expected JsonElement, got " + rawAstNode.getClass().getName());
    }

    private void formatAndCleanAst(String prefix, JsonObject jsonObject) {
        List<String> keys = new ArrayList<>(jsonObject.keySet());
        
        for (String localKey : keys) {
            String fullKey = prefix.isEmpty() ? localKey : prefix + "." + localKey;
            OptionInfo info = optionTree.get(fullKey);
            
            if (info != null) {
                applyComment(jsonObject, localKey, info);
                continue;
            }
            
            if (schemaValidator.isPrefixRegistered(fullKey)) {
                cleanChildNode(jsonObject, localKey, fullKey);
            } else {
                jsonObject.remove(localKey); // Remove orphaned nodes
            }
        }
    }

    private void applyComment(JsonObject jsonObject, String localKey, OptionInfo info) {
        if (info.getComment() != null) {
            jsonObject.setComment(localKey, info.getComment());
        }
    }

    private void cleanChildNode(JsonObject parentObject, String localKey, String fullKey) {
        JsonElement childElem = parentObject.get(localKey);
        
        if (childElem instanceof JsonObject childObject) {
            formatAndCleanAst(fullKey, childObject);

            if (childObject.isEmpty()) {
                parentObject.remove(localKey);
            }
        }
    }
}
