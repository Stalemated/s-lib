package com.stalemated.lib.predicate.target;

import com.stalemated.lib.predicate.target.strategies.*;

public final class TargetMatcherFactory {

    private TargetMatcherFactory() {}

    public static TargetMatcher create(String target) {
        if (target == null || target.isEmpty()) return new NoneStrategy();
        if (target.equals("*")) return new AllItemsStrategy();

        if (target.startsWith("!")) {
            String negated = target.substring(1);
            if (negated.isEmpty()) {
                throw invalid(target, "'!' needs a target to negate");
            }
            return new NotStrategy(create(negated));
        }

        try {
            if (target.startsWith("regex:")) return new RegexStrategy(target.substring(6));
            if (target.endsWith(":*")) return new NamespaceStrategy(target.substring(0, target.length() - 2));
            if (target.startsWith("#")) return new TagStrategy(target.substring(1));
            return new ItemStrategy(target);
        } catch (RuntimeException e) {
            throw invalid(target, e.getMessage());
        }
    }

    private static IllegalArgumentException invalid(String target, String reason) {
        return new IllegalArgumentException("Invalid item target '" + target + "': " + reason);
    }
}
