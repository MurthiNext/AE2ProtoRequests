package com.murthinext.ae2pr.client.tooltip;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.math.LatexFormula;
import com.murthinext.ae2pr.api.math.LatexFormula.Expression;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4f;

final class FormulaLayout {
    private static final int COLOR = 0xFF80E5EE;
    private static final Style ROMAN = Style.EMPTY.withFont(Ids.of("latex_roman"));
    private static final Style MATH = Style.EMPTY.withFont(Ids.of("latex_math"));
    // TeX 的分级括号与根号字形，下标与 delimiter size 一致，0 为正文用字
    private static final Style[] SIZED = {
        ROMAN,
        Style.EMPTY.withFont(Ids.of("latex_size1")),
        Style.EMPTY.withFont(Ids.of("latex_size2")),
        Style.EMPTY.withFont(Ids.of("latex_size3")),
        Style.EMPTY.withFont(Ids.of("latex_size4"))
    };
    // 分级字形的整体高度与上下缘，单位 em
    private static final float[] SIZED_HEIGHT = {1.0F, 1.199F, 1.799F, 2.399F, 2.999F};
    private static final float[] SIZED_ASCENT = {0.750F, 0.850F, 1.150F, 1.450F, 1.750F};
    private static final float[] SIZED_DESCENT = {0.250F, 0.349F, 0.649F, 0.949F, 1.249F};
    // 根号的上缘，以及从字形右上角延伸出顶栏的位置
    private static final float[] RADICAL_ASCENT = {0.800F, 0.849F, 1.150F, 1.450F, 1.750F};
    private static final float[] RADICAL_TIP = {0.853F, 1.020F, 1.020F, 1.020F, 1.020F};
    // 分数线的厚度
    private static final float RULE_THICKNESS = 0.037F;
    // 分数线在基线上方 3 像素
    private static final float AXIS = 3;
    // TeX 的 \delimiterfactor 与 \delimitershortfall
    private static final float DELIMITER_FACTOR = 0.901F;
    private static final float DELIMITER_SHORTFALL = 0.5F;
    private static final float ASCENT = 7.7F;
    private static final float DESCENT = 2.6F;
    // 与默认字体一致，基线落在绘制原点下方 7 像素
    private static final int BASELINE = 7;

    private final Font font;
    private final float em;

    FormulaLayout(Font font) {
        this.font = font;
        this.em = Math.max(1, text("—").width());
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
            Box right = operand(
                    binary.right(),
                    precedence
                            + (binary.operator().equals("-")
                                            || binary.operator().equals("/")
                                    ? 1
                                    : 0));
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
            return new Composite(
                    width,
                    top.ascent() - topBaseline,
                    bottomBaseline + bottom.descent(),
                    List.of(
                            new Placed(top, (width - top.width()) / 2, topBaseline),
                            new Placed(bottom, (width - bottom.width()) / 2, bottomBaseline),
                            rule(0, width, -AXIS)));
        }
        if (expression instanceof LatexFormula.Root root) {
            Box content = expression(root.radicand());
            Box degree = root.degree() == null ? null : new Scaled(expression(root.degree()), 0.65F, 0.65F);
            float ink = simpleInk(root.radicand());
            float top = -Math.max((Float.isNaN(ink) ? content.ascent() : ink) + 1, 6.5F);
            int size = 0;
            while (size < SIZED.length - 1 && RADICAL_ASCENT[size] * em < -top) {
                size++;
            }
            float scale = Math.min(1, -top / (RADICAL_ASCENT[size] * em));
            float surdOffset = degree == null ? 0 : degree.width() + 3;
            Box surd = new Scaled(text(SIZED[size], "√"), scale, scale);
            float contentOffset = surdOffset + surd.width() + 1.5F;
            float width = contentOffset + content.width() + 2;
            float degreeBaseline = degree == null ? 0 : Math.min(-5, top + degree.ascent() - 2);
            List<Placed> children = new ArrayList<>();
            if (degree != null) {
                children.add(new Placed(degree, 0, degreeBaseline));
            }
            children.add(new Placed(surd, surdOffset, 0));
            children.add(new Placed(content, contentOffset, 0));
            float tip = surdOffset + RADICAL_TIP[size] * em * scale - 0.5F;
            children.add(rule(tip, width - tip, top));
            return new Composite(
                    width,
                    Math.max(-top + 0.5F, degree == null ? 0 : degree.ascent() - degreeBaseline),
                    Math.max(content.descent(), surd.descent()),
                    children);
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
                default ->
                    row(
                            function.base() == null
                                    ? text(function.name().text())
                                    : script(text(function.name().text()), expression(function.base()), false),
                            delimit("(", content, ")"));
            };
        }
        throw new IllegalArgumentException("Unsupported formula node: " + expression);
    }

    // 简单算式按实际字形的视觉高度计算，避免根号横线与内容之间出现大片空隙
    private static float simpleInk(Expression expression) {
        if (expression instanceof LatexFormula.Variable) {
            return 4.0F;
        }
        if (expression instanceof LatexFormula.Literal) {
            return 5.5F;
        }
        if (expression instanceof LatexFormula.Unary unary) {
            return simpleInk(unary.operand());
        }
        if (expression instanceof LatexFormula.Binary binary
                && !binary.operator().equals("^")
                && !binary.operator().equals("/")) {
            float left = simpleInk(binary.left());
            float right = simpleInk(binary.right());
            return Float.isNaN(left) || Float.isNaN(right) ? Float.NaN : Math.max(left, right);
        }
        return Float.NaN;
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

    private Box text(Style style, String text) {
        return new Text(font, text, style);
    }

    // 横线厚度不足一个逻辑像素，直接铺几何图形会随亚像素位置时隐时现，因此按整设备像素铺一行，再用不透明度还原覆盖率
    private Placed rule(float x, float width, float y) {
        return new Placed(new Rule(width, y), x, 0);
    }

    // 按 TeX 的 \delimiterfactor 与 \delimitershortfall 挑一个够高的分级字形，不做拉伸
    private int selectSize(float height) {
        float needed = Math.max(height * DELIMITER_FACTOR, height - DELIMITER_SHORTFALL * em);
        for (int i = 0; i < SIZED.length - 1; i++) {
            if (SIZED_HEIGHT[i] * em >= needed) {
                return i;
            }
        }
        return SIZED.length - 1;
    }

    private Box delimit(String open, Box content, String close) {
        float height = Math.max(ASCENT + DESCENT, content.ascent() + content.descent());
        int size = selectSize(height);
        float scale = Math.max(1, height * DELIMITER_FACTOR / (SIZED_HEIGHT[size] * em));
        boolean bar = open.equals("|");
        if (bar) {
            size = 0;
            scale = height / (SIZED_HEIGHT[0] * em);
        }
        Box left = new Scaled(text(SIZED[size], open), bar ? 1 : scale, scale);
        Box right = new Scaled(text(SIZED[size], close), bar ? 1 : scale, scale);
        float ascent = SIZED_ASCENT[size] * em * scale;
        float descent = SIZED_DESCENT[size] * em * scale;
        float baseline = -AXIS + (ascent - descent) / 2;
        return new Composite(
                left.width() + content.width() + right.width(),
                Math.max(content.ascent(), ascent - baseline),
                Math.max(content.descent(), descent + baseline),
                List.of(
                        new Placed(left, 0, baseline),
                        new Placed(content, left.width(), 0),
                        new Placed(right, left.width() + content.width(), baseline)));
    }

    private static Box script(Box base, Box script, boolean superscript) {
        Box small = new Scaled(script, 0.72F, 0.72F);
        float gap = superscript ? 1 : 0;
        float baseline =
                superscript ? -base.ascent() * 0.65F - small.descent() : base.descent() + small.ascent() * 0.2F;
        return new Composite(
                base.width() + gap + small.width(),
                Math.max(base.ascent(), small.ascent() - baseline),
                Math.max(base.descent(), small.descent() + baseline),
                List.of(new Placed(base, 0, 0), new Placed(small, base.width() + gap, baseline)));
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
        return new Composite(width, ascent, descent, children);
    }

    interface Box {
        float width();

        float ascent();

        float descent();

        void draw(GuiGraphics graphics);
    }

    private final class Rule implements Box {
        private final float width;
        private final float y;

        private Rule(float width, float y) {
            this.width = width;
            this.y = y;
        }

        @Override
        public float width() {
            return width;
        }

        @Override
        public float ascent() {
            return 0;
        }

        @Override
        public float descent() {
            return 0;
        }

        @Override
        public void draw(GuiGraphics graphics) {
            Matrix4f pose = graphics.pose().last().pose();
            float guiScale =
                    (float) Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
            float scaleX = Math.max(1, pose.m00() * guiScale);
            float scaleY = Math.max(1, pose.m11() * guiScale);
            float thickness = RULE_THICKNESS * em * scaleY;
            int rows = Math.max(1, Math.round(thickness));
            int alpha = Math.min(255, Math.round(255 * thickness / rows));
            graphics.pose().pushPose();
            // 对齐到设备像素行，避免横线在行的边界上抖动
            graphics.pose().translate(0, Math.round(y * scaleY) / scaleY, 0);
            graphics.pose().scale(1 / scaleX, 1 / scaleY, 1);
            graphics.fill(
                    0, -rows / 2, Math.round(width * scaleX), rows - rows / 2, (alpha << 24) | (COLOR & 0xFFFFFF));
            graphics.pose().popPose();
        }
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

    private record Placed(Box box, float x, float baseline) {}

    private record Composite(float width, float ascent, float descent, List<Placed> children) implements Box {
        @Override
        public void draw(GuiGraphics graphics) {
            for (Placed child : children) {
                graphics.pose().pushPose();
                graphics.pose().translate(child.x(), child.baseline(), 0);
                child.box().draw(graphics);
                graphics.pose().popPose();
            }
        }
    }
}
