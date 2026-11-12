package com.murthinext.ae2pr.client.tooltip;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.math.LatexFormula;
import com.murthinext.ae2pr.api.math.LatexFormula.Expression;

final class FormulaLayout {
    private static final int COLOR = 0xFF80E5EE;
    // 两套 Computer Modern 字形，字号与 shift 写在 assets/ae2pr/font 下，基线都落在 7 像素
    private static final Style ROMAN = Style.EMPTY.withFont(Ids.of("latex_roman"));
    private static final Style MATH = Style.EMPTY.withFont(Ids.of("latex_math"));
    private static final float ASCENT = 7.7F;
    private static final float DESCENT = 2.6F;
    // 与默认字体一致，基线落在绘制原点下方 7 像素
    private static final int BASELINE = 7;

    private final Font font;

    FormulaLayout(Font font) {
        this.font = font;
    }

    Box formula(LatexFormula formula) {
        return row(math("f"), text("("), math("x"), text(") = "), expression(formula.expression()));
    }

    private Box expression(Expression expression) {
        if (expression instanceof LatexFormula.Literal literal) {
            String source = literal.text();
            return source.equals("π") || source.equals("e") ? math(source) : text(source);
        }
        if (expression instanceof LatexFormula.Variable) {
            return math("x");
        }
        if (expression instanceof LatexFormula.Group group) {
            return delimit(group.open(), expression(group.content()), group.close());
        }
        if (expression instanceof LatexFormula.Unary unary) {
            return row(text(unary.operator().equals("-") ? "−" : unary.operator()), operand(unary.operand(), 3));
        }
        if (expression instanceof LatexFormula.Binary binary) {
            if (binary.operator().equals("^")) {
                return script(operand(binary.left(), 4), expression(binary.right()), true);
            }
            int precedence = precedence(binary);
            Box left = operand(binary.left(), precedence);
            Box right = operand(binary.right(), precedence + (binary.operator().equals("-")
                    || binary.operator().equals("/") ? 1 : 0));
            if (binary.right() instanceof LatexFormula.Unary) {
                right = delimit("(", expression(binary.right()), ")");
            }
            String operator = switch (binary.operator()) {
                case "*" -> " ⋅ ";
                case "" -> binary.right() instanceof LatexFormula.Literal ? " ⋅ " : "";
                case "-" -> " − ";
                default -> " " + binary.operator() + " ";
            };
            return row(left, text(operator), right);
        }
        if (expression instanceof LatexFormula.Fraction fraction) {
            Box top = expression(fraction.numerator());
            Box bottom = expression(fraction.denominator());
            float width = Math.max(top.width(), bottom.width()) + 6;
            float topBaseline = -6 - top.descent();
            float bottomBaseline = bottom.ascent();
            return new Composite(width, top.ascent() - topBaseline, bottomBaseline + bottom.descent(),
                    List.of(new Placed(top, (width - top.width()) / 2, topBaseline),
                            new Placed(bottom, (width - bottom.width()) / 2, bottomBaseline)),
                    List.of(new Line(0, -3, width - 1, -3)));
        }
        if (expression instanceof LatexFormula.Root root) {
            Box content = expression(root.radicand());
            Box degree = root.degree() == null ? text("") : new Scaled(expression(root.degree()), 0.65F, 0.65F);
            float offset = Math.max(9, degree.width() + 4);
            float width = offset + content.width() + 2;
            float top = -content.ascent() - 3;
            float degreeBaseline = Math.min(-5, top + degree.ascent() - 2);
            return new Composite(width, Math.max(-top, degree.ascent() - degreeBaseline), content.descent(),
                    List.of(new Placed(content, offset, 0), new Placed(degree, 0, degreeBaseline)),
                    List.of(new Line(offset - 9, -3, offset - 7, -3),
                            new Line(offset - 7, -3, offset - 5, 1),
                            new Line(offset - 5, 1, offset - 2, top),
                            new Line(offset - 2, top, width - 1, top)));
        }
        if (expression instanceof LatexFormula.Function function) {
            List<Box> arguments = new ArrayList<>();
            for (Expression argument : function.arguments()) {
                if (!arguments.isEmpty()) {
                    arguments.add(text(", "));
                }
                arguments.add(expression(argument));
            }
            Box content = row(arguments.toArray(Box[]::new));
            return switch (function.name()) {
                case ABS -> delimit("|", content, "|");
                case FLOOR -> delimit("⌊", content, "⌋");
                case CEIL -> delimit("⌈", content, "⌉");
                default -> row(function.base() == null ? text(function.name().text())
                        : script(text(function.name().text()), expression(function.base()), false),
                        delimit("(", content, ")"));
            };
        }
        throw new IllegalArgumentException("Unsupported formula node: " + expression);
    }

    private Box operand(Expression expression, int minimumPrecedence) {
        Box box = expression(expression);
        return precedence(expression) < minimumPrecedence ? delimit("(", box, ")") : box;
    }

    private static int precedence(Expression expression) {
        if (expression instanceof LatexFormula.Binary binary) {
            return switch (binary.operator()) {
                case "+", "-" -> 1;
                case "*", "/", "" -> 2;
                case "^" -> 3;
                default -> 4;
            };
        }
        return expression instanceof LatexFormula.Unary ? 3 : 4;
    }

    // 变量与常数斜排，数字、运算符和函数名保持直立体
    private Box math(String text) {
        return new Text(font, text, MATH);
    }

    private Box text(String text) {
        return new Text(font, text, ROMAN);
    }

    private Box delimit(String open, Box content, String close) {
        float height = Math.max(ASCENT + DESCENT, content.ascent() + content.descent());
        Box left = new Scaled(text(open), 1, height / (ASCENT + DESCENT));
        Box right = new Scaled(text(close), 1, height / (ASCENT + DESCENT));
        // 括号下缘对齐内容下缘
        float baseline = content.descent() - left.descent();
        return new Composite(left.width() + content.width() + right.width(),
                Math.max(content.ascent(), left.ascent() - baseline), content.descent(),
                List.of(new Placed(left, 0, baseline), new Placed(content, left.width(), 0),
                        new Placed(right, left.width() + content.width(), baseline)), List.of());
    }

    private static Box script(Box base, Box script, boolean superscript) {
        Box small = new Scaled(script, 0.72F, 0.72F);
        float baseline = superscript ? -base.ascent() * 0.65F - small.descent() : base.descent() + small.ascent() * 0.65F;
        return new Composite(base.width() + small.width() + 1,
                Math.max(base.ascent(), small.ascent() - baseline),
                Math.max(base.descent(), small.descent() + baseline),
                List.of(new Placed(base, 0, 0), new Placed(small, base.width() + 1, baseline)), List.of());
    }

    private static Box row(Box... boxes) {
        float width = 0;
        float ascent = 0;
        float descent = 0;
        List<Placed> children = new ArrayList<>();
        for (Box box : boxes) {
            children.add(new Placed(box, width, 0));
            width += box.width();
            ascent = Math.max(ascent, box.ascent());
            descent = Math.max(descent, box.descent());
        }
        return new Composite(width, ascent, descent, children, List.of());
    }

    interface Box {
        float width();
        float ascent();
        float descent();
        void draw(GuiGraphics graphics);
    }

    private record Text(Font font, String text, Style style) implements Box {
        @Override
        public float width() {
            // 与绘制时的推进量保持同一精度，避免逐段取整造成错位
            return font.getSplitter().stringWidth(component());
        }

        @Override
        public float ascent() {
            return ASCENT;
        }

        @Override
        public float descent() {
            return DESCENT;
        }

        @Override
        public void draw(GuiGraphics graphics) {
            graphics.drawString(font, component(), 0, -BASELINE, COLOR, false);
        }

        private Component component() {
            return Component.literal(text).withStyle(style);
        }
    }

    private record Scaled(Box content, float x, float y) implements Box {
        @Override
        public float width() {
            return content.width() * x;
        }

        @Override
        public float ascent() {
            return content.ascent() * y;
        }

        @Override
        public float descent() {
            return content.descent() * y;
        }

        @Override
        public void draw(GuiGraphics graphics) {
            graphics.pose().pushPose();
            graphics.pose().scale(x, y, 1);
            content.draw(graphics);
            graphics.pose().popPose();
        }
    }

    private record Placed(Box box, float x, float baseline) {
    }

    private record Line(float x1, float y1, float x2, float y2) {
        void draw(GuiGraphics graphics) {
            int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1))));
            for (int i = 0; i <= steps; i++) {
                int x = Math.round(x1 + (x2 - x1) * i / steps);
                int y = Math.round(y1 + (y2 - y1) * i / steps);
                graphics.fill(x, y, x + 1, y + 1, COLOR);
            }
        }
    }

    private record Composite(float width, float ascent, float descent, List<Placed> children, List<Line> lines)
            implements Box {
        @Override
        public void draw(GuiGraphics graphics) {
            for (Placed child : children) {
                graphics.pose().pushPose();
                graphics.pose().translate(child.x(), child.baseline(), 0);
                child.box().draw(graphics);
                graphics.pose().popPose();
            }
            for (Line line : lines) {
                line.draw(graphics);
            }
        }
    }
}
