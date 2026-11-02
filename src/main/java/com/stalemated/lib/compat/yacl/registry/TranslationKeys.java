package com.stalemated.lib.compat.yacl.registry;

import java.util.Locale;
import java.util.regex.Pattern;

final class TranslationKeys {

    static final String NO_PERMISSION = "s_lib.config_screen.no_permission";
    static final String SERVER_WITHOUT_MOD = "s_lib.config_screen.server_without_mod";

    private static final Pattern ACRONYM_END = Pattern.compile("([A-Z]+)([A-Z][a-z])");
    private static final Pattern CAMEL_HUMP = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9_.]+");

    private TranslationKeys() {}

    static String normalize(String path) {
        String words = CAMEL_HUMP.matcher(ACRONYM_END.matcher(path).replaceAll("$1_$2")).replaceAll("$1_$2");
        return SEPARATORS.matcher(words.toLowerCase(Locale.ROOT)).replaceAll("_");
    }
}
