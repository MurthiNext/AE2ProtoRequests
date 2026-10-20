package com.murthinext.ae2pr.logic.terminal;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * 剧情节点的可见性与可读性条件。
 */
public sealed interface TerminalStoryCondition {

    boolean matches(TerminalStoryRunner runner, Predicate<String> inventory);

    default List<String> inventoryKeys() {
        return List.of();
    }

    default void validate(Set<String> nodes, TerminalStoryRunner runner) {
    }

    record Constant(boolean value) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            return value;
        }
    }

    record Group(boolean all, List<TerminalStoryCondition> conditions) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            return all ? conditions.stream().allMatch(condition -> condition.matches(runner, inventory))
                    : conditions.stream().anyMatch(condition -> condition.matches(runner, inventory));
        }

        @Override
        public List<String> inventoryKeys() {
            return conditions.stream().flatMap(condition -> condition.inventoryKeys().stream()).distinct().toList();
        }

        @Override
        public void validate(Set<String> nodes, TerminalStoryRunner runner) {
            conditions.forEach(condition -> condition.validate(nodes, runner));
        }
    }

    record Not(TerminalStoryCondition condition) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            return !condition.matches(runner, inventory);
        }

        @Override
        public List<String> inventoryKeys() {
            return condition.inventoryKeys();
        }

        @Override
        public void validate(Set<String> nodes, TerminalStoryRunner runner) {
            condition.validate(nodes, runner);
        }
    }

    record Completed(String id) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            return runner.completed(id);
        }

        @Override
        public void validate(Set<String> nodes, TerminalStoryRunner runner) {
            if (!nodes.contains(id)) {
                throw new IllegalArgumentException("条件引用的剧情块不存在：" + id);
            }
        }
    }

    record Variable(String name, String operator, Object expected) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            Object value = runner.variable(name);
            if (value instanceof Number actual && expected instanceof Number number) {
                int compare = Double.compare(actual.doubleValue(), number.doubleValue());
                return switch (operator) {
                    case "at_least" -> compare >= 0;
                    case "at_most" -> compare <= 0;
                    default -> compare == 0;
                };
            }
            return "equals".equals(operator) && expected.equals(value);
        }

        @Override
        public void validate(Set<String> nodes, TerminalStoryRunner runner) {
            Object value = runner.variable(name);
            if (value == null || (expected instanceof Number) != (value instanceof Number)
                    || (expected instanceof Boolean) != (value instanceof Boolean)) {
                throw new IllegalArgumentException("条件变量未声明或类型不匹配：" + name);
            }
        }
    }

    record Inventory(String key) implements TerminalStoryCondition {

        @Override
        public boolean matches(TerminalStoryRunner runner, Predicate<String> inventory) {
            return inventory.test(key);
        }

        @Override
        public List<String> inventoryKeys() {
            return List.of(key);
        }
    }

    static TerminalStoryCondition parse(JsonElement element) {
        return parse(element, 0);
    }

    private static TerminalStoryCondition parse(JsonElement element, int depth) {
        if (element == null) {
            return new Constant(true);
        }
        if (depth > 16) {
            throw new IllegalArgumentException("剧情条件嵌套过深");
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return new Constant(element.getAsBoolean());
        }
        JsonObject json = element.getAsJsonObject();
        long kinds = List.of("all", "any", "not", "completed", "variable", "item", "tag").stream()
                .filter(json::has).count();
        if (kinds != 1) {
            throw new IllegalArgumentException("每个剧情条件必须指定一种判定方式");
        }
        if (json.has("all") || json.has("any")) {
            boolean all = json.has("all");
            var array = json.getAsJsonArray(all ? "all" : "any");
            if (array.isEmpty() || array.size() > 32) {
                throw new IllegalArgumentException("条件组合必须包含 1–32 个子条件");
            }
            return new Group(all, array.asList().stream().map(child -> parse(child, depth + 1)).toList());
        }
        if (json.has("not")) {
            return new Not(parse(json.get("not"), depth + 1));
        }
        if (json.has("completed")) {
            return new Completed(json.get("completed").getAsString());
        }
        if (json.has("variable")) {
            List<String> operators = List.of("equals", "at_least", "at_most").stream().filter(json::has).toList();
            if (operators.size() != 1) {
                throw new IllegalArgumentException("变量条件必须指定 equals、at_least 或 at_most");
            }
            String operator = operators.get(0);
            var value = json.getAsJsonPrimitive(operator);
            Object expected = value.isBoolean() ? value.getAsBoolean()
                    : value.isNumber() ? value.getAsDouble() : value.getAsString();
            if (!operator.equals("equals") && !(expected instanceof Number)) {
                throw new IllegalArgumentException("范围条件仅支持数值变量");
            }
            return new Variable(json.get("variable").getAsString(), operator, expected);
        }
        boolean tag = json.has("tag");
        String id = json.get(tag ? "tag" : "item").getAsString();
        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+") || count < 1 || count > 9999) {
            throw new IllegalArgumentException("物品条件无效：" + id);
        }
        String key = (tag ? "tag|" : "item|") + id + "|" + count;
        if (key.length() > 160) {
            throw new IllegalArgumentException("物品条件标识过长");
        }
        return new Inventory(key);
    }
}
