package com.stalemated.lib.helper.attribute;

final class AttributeTotal {

    private double added;
    private double multipliedBase;
    private double multipliedTotal = 1.0;
    private boolean empty = true;

    void add(double amount) {
        added += amount;
        empty = false;
    }

    void addMultipliedBase(double amount) {
        multipliedBase += amount;
        empty = false;
    }

    void addMultipliedTotal(double amount) {
        multipliedTotal *= 1.0 + amount;
        empty = false;
    }

    boolean isEmpty() {
        return empty;
    }

    double valueFrom(double base) {
        double withAdditions = base + added;
        return (withAdditions + withAdditions * multipliedBase) * multipliedTotal;
    }
}
