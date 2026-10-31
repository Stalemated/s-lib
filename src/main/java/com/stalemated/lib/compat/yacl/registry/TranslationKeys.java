package com.stalemated.lib.compat.yacl.registry;

import java.util.Locale;
import java.util.regex.Pattern;

final class TranslationKeys {

    static final String OP_REQUIRED = "s_lib.config_screen.op_required";
    static final String NO_ENTRIES = "s_lib.config_screen.no_entries";

    private static final Pattern CAMEL_HUMP = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9_.]+");

    private TranslationKeys() {}

    static String normalize(String path) {
        String snakeCase = CAMEL_HUMP.matcher(path).replaceAll("$1_$2").toLowerCase(Locale.ROOT);
        return SEPARATORS.matcher(snakeCase).replaceAll("_");
    }
}
