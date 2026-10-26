package com.murthinext.ae2pr.item.filter_cell;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;

/**
 * 标签匹配表达式：以物品标签 id 为操作数，支持布尔运算与 glob 通配。
 * {@code &} 与、{@code |} 或、{@code ^} 异或、{@code !} 取反、{@code ()} 分组、{@code *} 通配；优先级 {@code ! > & > ^ > |}。
 */
public final class TagExpression implements Predicate<AEKey> {

    private final Node root;

    private TagExpression(Node root) {
        this.root = root;
    }

    @Override
    public boolean test(AEKey key) {
        return key != null && root.test(key);
    }

    @Nullable
    public static TagExpression parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            var parser = new Parser(text);
            var node = parser.parseOr();
            return parser.atEnd() ? new TagExpression(node) : null;
        } catch (ParseException e) {
            return null;
        }
    }

    private interface Node {
        boolean test(AEKey key);
    }

        private record TagNode(TagKey<Item> tag) implements Node {

        @Override
            public boolean test(AEKey key) {
                return key.isTagged(tag);
            }
        }

    private static final class GlobNode implements Node {
        private final String pattern;
        private final Map<AEKeyType, List<TagKey<?>>> tagCache = new ConcurrentHashMap<>();

        private GlobNode(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean test(AEKey key) {
            var tags = tagCache.computeIfAbsent(key.getType(), this::matchingTags);
            for (var tag : tags) {
                if (key.isTagged(tag)) {
                    return true;
                }
            }
            return false;
        }

        private List<TagKey<?>> matchingTags(AEKeyType type) {
            return type.getTagNames()
                    .filter(tag -> matches(pattern, tag.location().toString()))
                    .toList();
        }

        private static boolean matches(String glob, String text) {
            int g = 0;
            int t = 0;
            int star = -1;
            int mark = 0;
            while (t < text.length()) {
                if (g < glob.length() && glob.charAt(g) == text.charAt(t)) {
                    g++;
                    t++;
                } else if (g < glob.length() && glob.charAt(g) == '*') {
                    star = g++;
                    mark = t;
                } else if (star != -1) {
                    g = star + 1;
                    t = ++mark;
                } else {
                    return false;
                }
            }
            while (g < glob.length() && glob.charAt(g) == '*') {
                g++;
            }
            return g == glob.length();
        }
    }

    private record NotNode(Node child) implements Node {
        @Override
        public boolean test(AEKey key) {
            return !child.test(key);
        }
    }

    private record AndNode(Node left, Node right) implements Node {
        @Override
        public boolean test(AEKey key) {
            return left.test(key) && right.test(key);
        }
    }

    private record OrNode(Node left, Node right) implements Node {
        @Override
        public boolean test(AEKey key) {
            return left.test(key) || right.test(key);
        }
    }

    private record XorNode(Node left, Node right) implements Node {
        @Override
        public boolean test(AEKey key) {
            return left.test(key) ^ right.test(key);
        }
    }

    private static final class ParseException extends RuntimeException {
    }

    /**
     * 递归下降解析器：or := xor ('|' xor)*；xor := and ('^' and)*；and := unary ('&' unary)*；
     * unary := '!' unary | factor；factor := '(' or ')' | tag。
     */
    private static final class Parser {
        private final String text;
        private int pos;

        private Parser(String text) {
            this.text = text;
        }

        private Node parseOr() {
            var left = parseXor();
            while (peek() == '|') {
                pos++;
                left = new OrNode(left, parseXor());
            }
            return left;
        }

        private Node parseXor() {
            var left = parseAnd();
            while (peek() == '^') {
                pos++;
                left = new XorNode(left, parseAnd());
            }
            return left;
        }

        private Node parseAnd() {
            var left = parseUnary();
            while (peek() == '&') {
                pos++;
                left = new AndNode(left, parseUnary());
            }
            return left;
        }

        private Node parseUnary() {
            if (peek() == '!') {
                pos++;
                return new NotNode(parseUnary());
            }
            return parseFactor();
        }

        private Node parseFactor() {
            if (peek() == '(') {
                pos++;
                var node = parseOr();
                if (peek() != ')') {
                    throw new ParseException();
                }
                pos++;
                return node;
            }
            return parseTag();
        }

        private Node parseTag() {
            int start = pos;
            while (pos < text.length() && isTagChar(text.charAt(pos))) {
                pos++;
            }
            if (pos == start) {
                throw new ParseException();
            }
            var operand = text.substring(start, pos).toLowerCase(Locale.ROOT);
            if (operand.indexOf('*') >= 0) {
                return new GlobNode(operand);
            }
            var id = ResourceLocation.tryParse(operand);
            if (id == null) {
                throw new ParseException();
            }
            return new TagNode(TagKey.create(Registries.ITEM, id));
        }

        private boolean atEnd() {
            skipWhitespace();
            return pos >= text.length();
        }

        private char peek() {
            skipWhitespace();
            return pos < text.length() ? text.charAt(pos) : '\0';
        }

        private void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private static boolean isTagChar(char c) {
            return c >= 'a' && c <= 'z'
                    || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9'
                    || c == '_' || c == '-' || c == '.' || c == ':' || c == '/' || c == '*';
        }
    }
}
