package com.stalemated.lib.predicate.target;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public enum TargetKind {
    NONE(String::isEmpty, target -> target),
    ALL("*"::equals, target -> target),
    NEGATED(target -> target.startsWith("!"), target -> target.substring(1)),
    REGEX(target -> target.startsWith("regex:"), target -> target.substring("regex:".length())),
    NAMESPACE(target -> target.endsWith(":*"), target -> target.substring(0, target.length() - ":*".length())),
    TAG(target -> target.startsWith("#"), target -> target.substring(1)),
    ITEM(target -> true, target -> target);

    private final Predicate<String> syntax;
    private final UnaryOperator<String> operand;

    TargetKind(Predicate<String> syntax, UnaryOperator<String> operand) {
        this.syntax = syntax;
        this.operand = operand;
    }

    public static TargetKind of(String target) {
        for (TargetKind kind : values()) {
            if (kind.syntax.test(target)) return kind;
        }
        return ITEM;
    }

    public String operandOf(String target) {
        return operand.apply(target);
    }
}
