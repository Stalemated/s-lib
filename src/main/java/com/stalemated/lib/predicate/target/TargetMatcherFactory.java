package com.stalemated.lib.predicate.target;


import java.util.Optional;

public final class TargetMatcherFactory {

    private TargetMatcherFactory() {}

    public static TargetMatcher create(String target) {
        String trimmed = target == null ? "" : target.trim();
        try {
            return parse(trimmed);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid item target '" + trimmed + "': " + e.getMessage(), e);
        }
    }

    public static Optional<TargetMatcher> tryCreate(String target) {
        try {
            return Optional.of(create(target));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static TargetMatcher parse(String target) {
        TargetKind kind = TargetKind.of(target);
        String operand = kind.operandOf(target);
        return switch (kind) {
            case NONE -> new NoneStrategy();
            case ALL -> new AllItemsStrategy();
            case NEGATED -> new NotStrategy(parseNegated(operand.trim()));
            case REGEX -> new RegexStrategy(operand);
            case NAMESPACE -> new NamespaceStrategy(operand);
            case TAG -> new TagStrategy(operand);
            case ITEM -> new ItemStrategy(operand);
        };
    }

    private static TargetMatcher parseNegated(String operand) {
        if (operand.isEmpty()) {
            throw new IllegalArgumentException("'!' needs a target to negate");
        }
        return parse(operand);
    }
}
