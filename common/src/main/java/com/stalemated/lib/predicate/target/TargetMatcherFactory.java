package com.stalemated.lib.predicate.target;

import com.stalemated.lib.predicate.target.strategies.*;

public class TargetMatcherFactory {

    public static TargetMatcher create(String target) {
        if (target == null || target.isEmpty()) return new NoneStrategy();

        if (target.equals("*")) return new AllItemsStrategy();

        if (target.startsWith("!")) {
            return new NotStrategy(TargetMatcherFactory.create(target.substring(1)));
        }

        if (target.startsWith("regex:")) {
            try { return new RegexStrategy(target.substring(6)); }
            catch (Exception e) { return new NoneStrategy(); }
        }

        if (target.endsWith(":*")) {
            try { return new NamespaceStrategy(target.substring(0, target.length() - 2)); }
            catch (Exception e) { return new NoneStrategy(); }
        }

        if (target.startsWith("#")) {
            try { return new TagStrategy(target.substring(1)); }
            catch (Exception e) { return new NoneStrategy(); }
        }

        try { return new ItemStrategy(target); }
        catch (Exception e) { return new NoneStrategy(); }
    }
}