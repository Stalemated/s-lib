package com.stalemated.lib.config.io.json5;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.stalemated.lib.util.codec.RegexFlag;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

final class PatternAdapter implements JsonSerializer<Pattern>, JsonDeserializer<Pattern> {

    private static final String PATTERN = "pattern";
    private static final String FLAGS = "flags";

    @Override
    public JsonElement serialize(Pattern src, Type typeOfSrc, JsonSerializationContext context) {
        if (src.flags() == 0) {
            return new JsonPrimitive(src.pattern());
        }

        JsonArray flags = new JsonArray();
        for (RegexFlag flag : RegexFlag.values()) {
            if (flag.isSetIn(src.flags())) {
                flags.add(flag.name());
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty(PATTERN, src.pattern());
        json.add(FLAGS, flags);
        return json;
    }

    @Override
    public Pattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return Pattern.compile(json.getAsString());
        }
        if (!json.isJsonObject() || !json.getAsJsonObject().has(PATTERN)) {
            throw new JsonParseException(json + " is not a regex");
        }

        JsonObject object = json.getAsJsonObject();
        int flags = 0;
        if (object.has(FLAGS)) {
            for (JsonElement name : object.getAsJsonArray(FLAGS)) {
                flags |= RegexFlag.named(name.getAsString()).bit();
            }
        }
        return Pattern.compile(object.get(PATTERN).getAsString(), flags);
    }
}
