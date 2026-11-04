package com.stalemated.lib.compat.yacl;

import com.google.gson.JsonParser;
import com.stalemated.lib.compat.yacl.builder.YaclScreenBuilder;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class SLibTesting {

    private static final String SLIB_LANG = "/assets/s_lib/lang/en_us.json";

    private SLibTesting() {}

    public static void assertScreenBuilds(YaclScreenBuilder screen, Path langFile) throws IOException {
        Objects.requireNonNull(screen, "screen cannot be null");
        Objects.requireNonNull(langFile, "langFile cannot be null");

        Set<String> required = requiredKeys(screen.buildLib());
        Set<String> missing = new TreeSet<>(required);
        missing.removeAll(keysIn(Files.readString(langFile, StandardCharsets.UTF_8)));
        missing.removeAll(sLibKeys());
        if (!missing.isEmpty()) {
            throw new AssertionError("Missing " + missing.size() + " translations in " + langFile.getFileName() + ": " + missing);
        }
    }

    private static Set<String> sLibKeys() throws IOException {
        try (InputStream lang = SLibTesting.class.getResourceAsStream(SLIB_LANG)) {
            if (lang == null) {
                throw new IllegalStateException("S-Lib's " + SLIB_LANG + " is not on the classpath");
            }
            return keysIn(new String(lang.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static Set<String> keysIn(String langJson) {
        return JsonParser.parseString(langJson).getAsJsonObject().keySet();
    }

    private static Set<String> requiredKeys(YetAnotherConfigLib lib) {
        Set<String> keys = new HashSet<>();
        for (ConfigCategory category : lib.categories()) {
            collectKeys(category.name(), keys);
            for (OptionGroup group : category.groups()) {
                if (!group.isRoot()) {
                    collectKeys(group.name(), keys);
                }
                for (Option<?> option : group.options()) {
                    collectKeys(option.name(), keys);
                    if (option instanceof LabelOption label) {
                        collectKeys(label.label(), keys);
                    }
                }
            }
        }
        return keys;
    }

    private static void collectKeys(Component component, Set<String> keys) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getFallback() == null) {
                keys.add(translatable.getKey());
            }
            for (Object argument : translatable.getArgs()) {
                if (argument instanceof Component nested) {
                    collectKeys(nested, keys);
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            collectKeys(sibling, keys);
        }
    }
}
