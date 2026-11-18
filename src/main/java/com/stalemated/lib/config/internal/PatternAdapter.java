package com.stalemated.lib.config.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.stalemated.lib.util.internal.RegexFlag;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
            return compile(json, json.getAsString(), 0);
        }
        if (!json.isJsonObject() || !json.getAsJsonObject().has(PATTERN)) {
            throw new JsonParseException(json + " is not a regex");
        }

        JsonObject object = json.getAsJsonObject();
        JsonElement pattern = object.get(PATTERN);
        return compile(pattern, SLibGsonDefaults.textOf(pattern, "a regex"), flagsOf(object));
    }

    private static int flagsOf(JsonObject object) {
        if (!object.has(FLAGS)) return 0;
        JsonElement names = object.get(FLAGS);
        if (!names.isJsonArray()) {
            throw new JsonParseException(names + " is not a list of regex flags");
        }

        int flags = 0;
        for (JsonElement name : names.getAsJsonArray()) {
            flags |= flagNamed(name).bit();
        }
        return flags;
    }

    private static RegexFlag flagNamed(JsonElement name) {
        try {
            return RegexFlag.named(SLibGsonDefaults.textOf(name, "a regex flag"));
        } catch (IllegalArgumentException e) {
            throw new JsonParseException(name + " is not a regex flag, expected one of " + Arrays.toString(RegexFlag.values()), e);
        }
    }

    private static Pattern compile(JsonElement json, String regex, int flags) {
        try {
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            throw new JsonParseException(json + " is not a regex: " + e.getDescription(), e);
        }
    }
}
