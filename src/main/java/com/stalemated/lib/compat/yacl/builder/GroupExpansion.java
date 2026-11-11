package com.stalemated.lib.compat.yacl.builder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

final class GroupExpansion {
    static final GroupExpansion NONE = new GroupExpansion(List.of(), List.of());

    private final List<String> groupNames;
    private final List<Boolean> expanded;

    private GroupExpansion(List<String> groupNames, List<Boolean> expanded) {
        this.groupNames = groupNames;
        this.expanded = expanded;
    }

    static GroupExpansion of(List<String> groupNames, List<Boolean> expanded) {
        if (groupNames.size() != expanded.size()) {
            throw new IllegalArgumentException("group names and expanded states must have the same size, got " + groupNames.size() + " and " + expanded.size());
        }
        return new GroupExpansion(List.copyOf(groupNames), List.copyOf(expanded));
    }

    Optional<Boolean> expandedAt(List<String> currentGroupNames, int index) {
        String name = currentGroupNames.get(index);
        int occurrence = Collections.frequency(currentGroupNames.subList(0, index), name);
        for (int savedIndex = 0; savedIndex < groupNames.size(); savedIndex++) {
            if (groupNames.get(savedIndex).equals(name) && occurrence-- == 0) {
                return Optional.of(expanded.get(savedIndex));
            }
        }
        return Optional.empty();
    }
}
