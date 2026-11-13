package com.murthinext.ae2pr.api.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record LatexFormula(String source, Expression expression) {

    public static LatexFormula parse(String source) {
        if (source == null || source.isBlank() || source.length() > 1024) {
            throw new IllegalArgumentException("Formula must contain 1 to 1024 characters");
        }
        String text = source.replaceAll("\\s+", "");
        if (text.startsWith("$") && text.endsWith("$")) {
            text = text.substring(1, text.length() - 1);
        } else if (text.startsWith("\\(") && text.endsWith("\\)")) {
            text = text.substring(2, text.length() - 2);
        }
        if (text.startsWith("f(x)=")) {
            text = text.substring(5);
        }
        Parser parser = new Parser(text);
        Expression expression = parser.expression();
        if (!parser.token.isEmpty()) {
            throw parser.error("Unexpected token");
        }
        return new LatexFormula(source, expression);
    }

    public double evaluate(double x) {
        return expression.evaluate(x);
    }

    public sealed interface Expression permits Literal, Variable, Unary, Binary, Fraction, Root, Function, Group {
        double evaluate(double x);
    }

    public record Literal(String text, double value) implements Expression {
        @Override
        public double evaluate(double x) {
            return value;
        }
    }

    public record Variable() implements Expression {
        @Override
        public double evaluate(double x) {
            return x;
        }
    }

    public record Unary(String operator, Expression operand) implements Expression {
        @Override
        public double evaluate(double x) {
            return operator.equals("-") ? -operand.evaluate(x) : operand.evaluate(x);
        }
    }

    public record Binary(String operator, Expression left, Expression right) implements Expression {
        @Override
        public double evaluate(double x) {
            double a = left.evaluate(x);
            double b = right.evaluate(x);
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                return Double.NaN;
            }
            return switch (operator) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*", "" -> a *b;
                case "/" -> a / b;
                case "^" -> Math.pow(a, b);
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        }
    }

    public record Fraction(Expression numerator, Expression denominator) implements Expression {
        @Override
        public double evaluate(double x) {
            double a = numerator.evaluate(x);
            double b = denominator.evaluate(x);
            return Double.isFinite(a) && Double.isFinite(b) ? a / b : Double.NaN;
        }
    }

    public record Root(Expression radicand, Expression degree) implements Expression {
        @Override
        public double evaluate(double x) {
            double value = radicand.evaluate(x);
            double n = degree == null ? 2 : degree.evaluate(x);
            if (!Double.isFinite(value) || !Double.isFinite(n) || n == 0) {
                return Double.NaN;
            }
            return value < 0 && n == Math.rint(n) && Math.abs(n % 2) == 1 ? -Math.pow(-value, 1 / n)
                                                                          : Math.pow(value, 1 / n);
        }
    }

    public enum FunctionName {
        MIN(2),
        MAX(2),
        ABS(1),
        FLOOR(1),
        CEIL(1),
        LN(1),
        LOG(1),
        EXP(1),
        SIN(1),
        COS(1),
        TAN(1);

        private final int arguments;

        FunctionName(int arguments) {
            this.arguments = arguments;
        }

        public String text() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Function(FunctionName name, List<Expression> arguments, Expression base) implements Expression {
        public Function {
            arguments = List.copyOf(arguments);
        }

        @Override
        public double evaluate(double x) {
            double a = arguments.get(0).evaluate(x);
            double b = arguments.size() == 2 ? arguments.get(1).evaluate(x) : 0;
            if (!Double.isFinite(a) || !Double.isFinite(b)) {
                return Double.NaN;
            }
            double logBase = base == null ? 10 : base.evaluate(x);
            return switch (name) {
                case MIN -> Math.min(a, b);
                case MAX -> Math.max(a, b);
                case ABS -> Math.abs(a);
                case FLOOR -> Math.floor(a);
                case CEIL -> Math.ceil(a);
                case LN -> Math.log(a);
                case LOG ->
                    Double.isFinite(logBase) && logBase > 0 && logBase != 1 ? Math.log(a) / Math.log(logBase)
                                                                            : Double.NaN;
                case EXP -> Math.exp(a);
                case SIN -> Math.sin(a);
                case COS -> Math.cos(a);
                case TAN -> Math.tan(a);
            };
        }
    }

    public record Group(String open, String close, Expression content) implements Expression {
        @Override
        public double evaluate(double x) {
            return content.evaluate(x);
        }
    }

    private static final class Parser {
        private final String text;
        private int position;
        private int depth;
        private String token;

        private Parser(String text) {
            this.text = text;
            next();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + position + ": " + token);
        }

        private void next() {
            token = "";
            while (position < text.length()) {
                int start = position++;
                char first = text.charAt(start);
                if (first == '\\') {
                    while (position < text.length() && Character.isLetter(text.charAt(position))) {
                        position++;
                    }
                    if (position == start + 1 && position < text.length()) {
                        position++;
                    }
                    token = text.substring(start, position);
                    if (List.of("\\left", "\\right", "\\,", "\\;", "\\!", "\\quad", "\\qquad").contains(token)) {
                        token = "";
                        continue;
                    }
                    token = switch (token) {
                        case "\\cdot", "\\times" -> "*";
                        case "\\div" -> "/";
                        case "\\lvert", "\\rvert" -> "|";
                        default -> token;
                    };
                } else if (Character.isDigit(first) || first == '.') {
                    while (position < text.length() &&
                           (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                        position++;
                    }
                    token = text.substring(start, position);
                } else {
                    token = String.valueOf(first);
                }
                return;
            }
        }

        private boolean take(String expected) {
            if (!token.equals(expected)) {
                return false;
            }
            next();
            return true;
        }

        private void require(String expected) {
            if (!take(expected)) {
                throw error("Expected " + expected);
            }
        }

        private Expression expression() {
            Expression result = product();
            while (token.equals("+") || token.equals("-")) {
                String operator = token;
                next();
                result = new Binary(operator, result, product());
            }
            return result;
        }

        private Expression product() {
            Expression result = unary();
            while (true) {
                if (token.equals("*") || token.equals("/")) {
                    String operator = token;
                    next();
                    result = new Binary(operator, result, unary());
                } else if (startsAtom()) {
                    result = new Binary("", result, unary());
                } else {
                    return result;
                }
            }
        }

        private boolean startsAtom() {
            return !token.isEmpty() &&
                    (Character.isDigit(token.charAt(0)) || token.charAt(0) == '.' || token.equals("x") ||
                     token.equals("e") || token.equals("(") || token.equals("[") || token.equals("{") ||
                     token.startsWith("\\") && !token.equals("\\rfloor") && !token.equals("\\rceil"));
        }

        private Expression unary() {
            if (++depth > 32) {
                throw error("Formula nesting exceeds 32 levels");
            }
            try {
                if (token.equals("+") || token.equals("-")) {
                    String operator = token;
                    next();
                    return new Unary(operator, unary());
                }
                Expression result = atom();
                if (take("^")) {
                    result = new Binary("^", result, unary());
                }
                return result;
            } finally {
                depth--;
            }
        }

        private Expression grouped(String open, String close) {
            require(open);
            Expression result = expression();
            require(close);
            return result;
        }

        private Expression atom() {
            String value = token;
            if (take("x")) {
                return new Variable();
            }
            if (take("e")) {
                return new Literal("e", Math.E);
            }
            if (take("\\pi")) {
                return new Literal("π", Math.PI);
            }
            if (value.equals("{") || value.equals("(") || value.equals("[")) {
                String close = value.equals("{") ? "}" : value.equals("(") ? ")" : "]";
                Expression content = grouped(value, close);
                return value.equals("{") ? content : new Group(value, close, content);
            }
            if (value.equals("|") || value.equals("\\lfloor") || value.equals("\\lceil")) {
                FunctionName name = value.equals("|") ? FunctionName.ABS
                        : value.equals("\\lfloor")    ? FunctionName.FLOOR
                                                      : FunctionName.CEIL;
                String close = value.equals("|") ? "|" : value.equals("\\lfloor") ? "\\rfloor" : "\\rceil";
                return new Function(name, List.of(grouped(value, close)), null);
            }
            if (take("\\frac") || take("\\dfrac") || take("\\tfrac")) {
                return new Fraction(grouped("{", "}"), grouped("{", "}"));
            }
            if (take("\\sqrt")) {
                Expression degree = token.equals("[") ? grouped("[", "]") : null;
                return new Root(grouped("{", "}"), degree);
            }
            if (value.startsWith("\\")) {
                next();
                String name = value.substring(1);
                if (name.equals("operatorname")) {
                    require("{");
                    StringBuilder identifier = new StringBuilder();
                    while (!token.equals("}") && !token.isEmpty()) {
                        identifier.append(token);
                        next();
                    }
                    require("}");
                    name = identifier.toString();
                }
                FunctionName function;
                try {
                    function = FunctionName.valueOf(name.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw error("Unsupported command: " + value);
                }
                Expression base = function == FunctionName.LOG && take("_") ? atom() : null;
                String close = token.equals("{") ? "}" : ")";
                require(close.equals("}") ? "{" : "(");
                List<Expression> arguments = new ArrayList<>();
                arguments.add(expression());
                while (take(",")) {
                    arguments.add(expression());
                }
                require(close);
                if (arguments.size() != function.arguments) {
                    throw error("Wrong argument count for " + name);
                }
                return new Function(function, arguments, base);
            }
            try {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number)) {
                    throw error("Non-finite literal");
                }
                next();
                return new Literal(value, number);
            } catch (NumberFormatException exception) {
                throw error("Expected a number, x or a supported LaTeX command");
            }
        }
    }
}
