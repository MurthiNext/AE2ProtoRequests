package com.murthinext.ae2pr.item.proto_terminal;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.item.proto_terminal.TerminalStoryPlayback.Entry;
import com.murthinext.ae2pr.item.proto_terminal.TerminalStoryRunner.Choice;

final class TerminalStoryPanel {

    static final int X = 38;
    static final int Y = 58;
    static final int WIDTH = 472;
    static final int HEIGHT = 242;
    static final float SLANT = TerminalGraphicsRenderer.slant(HEIGHT);

    private static final int TITLE_Y = 75;
    private static final int RULE_Y = 91;
    private static final int RULE_WIDTH = 102;
    private static final int TEXT_LEFT = 72;
    private static final int TEXT_TOP = 105;
    private static final int TEXT_BOTTOM = 280;
    private static final int TEXT_RIGHT = 478;
    private static final int IMAGE_TEXT_RIGHT = 320;
    private static final int LINE_HEIGHT = 13;
    private static final int CHOICE_HEIGHT = 20;
    private static final int CHOICE_GAP = 5;
    private static final int CHOICE_MAX_WIDTH = 244;
    private static final long CHOICE_REVEAL_DURATION = 360;
    private static final long CHOICE_REVEAL_STAGGER = 38;
    private static final float ANSWER_ENTER_DISTANCE = 32;
    private static final long SPEAKER_FADE_DURATION = 220;
    private static final long LINE_COLOR_DURATION = 420;

    private final TerminalStoryTree story;
    private final TerminalStoryTree.Node node;
    private final TerminalStoryRunner runner;
    private final Font font = Minecraft.getInstance().font;
    private final List<TextRow> rows = new ArrayList<>();
    private final List<OptionHit> optionHits = new ArrayList<>();
    private List<String> lockRows = List.of();
    private TerminalStoryPlayback playback;
    private Entry lastEntry;
    private boolean failureShown;
    private int textHeight;
    private double scroll;
    private boolean autoScroll = true;

    private record TextRow(String text, int speakerGlyphs, int y, Entry entry, boolean current) {}

    private record OptionHit(int left, int top, int right, int bottom, Choice choice) {

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    TerminalStoryPanel(TerminalStoryTree story, TerminalStoryTree.Node node, TerminalStoryRunner runner,
                       boolean unlocked) {
        this.story = story;
        this.node = node;
        this.runner = runner;
        if (unlocked) {
            unlock();
        } else {
            lockRows = unlockTip()
                               .stream()
                               .flatMap(tip -> TerminalStoryText.wrap(font, tip, TEXT_RIGHT - TEXT_LEFT - 10).stream())
                               .toList();
            textHeight = lockRows.size() * LINE_HEIGHT;
        }
        Minecraft.getInstance().getNarrator().sayNow(isLocked() && !lockRows.isEmpty()
                                                             ? Component.literal(lockRows.get(0))
                                                             : node.title());
    }

    TerminalStoryTree.Node node() {
        return node;
    }

    boolean awaitingUnlock() {
        return playback == null;
    }

    private boolean isLocked() {
        return playback == null || playback.failed();
    }

    void unlock() {
        lockRows = List.of();
        rows.clear();
        optionHits.clear();
        scroll = 0;
        autoScroll = true;
        playback = new TerminalStoryPlayback(
                runner, node, text -> Minecraft.getInstance().getNarrator().sayNow(Component.literal(text)));
        updateRows();
    }

    void update(long now) {
        if (playback != null) {
            playback.update(now);
            updateRows();
        }
    }

    private void updateRows() {
        if (playback.failed()) {
            if (!failureShown) {
                failureShown = true;
                optionHits.clear();
                lockRows = TerminalStoryText.wrap(font, label("load_failed").getString(), TEXT_RIGHT - TEXT_LEFT - 10);
                textHeight = lockRows.size() * LINE_HEIGHT;
            }
            return;
        }
        if (lastEntry != playback.current()) {
            lastEntry = playback.current();
            autoScroll = true;
        }
        rows.clear();
        int width = textRight() - TEXT_LEFT - 10;
        List<Entry> log = playback.log();
        int last = log.size() - 1;
        int y = 0;
        for (int i = 0; i < log.size(); i++) {
            if (i > 0) {
                y += 6;
            }
            Entry entry = log.get(i);
            int visible = i == last && playback.typing() ? playback.typedGlyphs() : entry.length();
            String text = entry.plain();
            int cursor = 0;
            for (String wrapped : entry.wrapped(font, width)) {
                int start = text.indexOf(wrapped, cursor);
                cursor = start + wrapped.length();
                int remaining = visible - start;
                if (remaining <= 0) {
                    break;
                }
                String shown = wrapped.length() <= remaining ? wrapped : wrapped.substring(0, remaining);
                int speakerGlyphs = Mth.clamp(entry.prefixLength() - start, 0, shown.length());
                rows.add(new TextRow(shown, speakerGlyphs, y, entry, i == last));
                y += LINE_HEIGHT;
            }
        }
        textHeight = y;
        scroll = autoScroll ? maxScroll() : Mth.clamp(scroll, 0, maxScroll());
    }

    void save() {
        if (playback != null) {
            playback.save();
        }
    }

    void click(double x, double y, int shift) {
        if (isLocked()) {
            return;
        }
        for (OptionHit hit : optionHits) {
            if (hit.contains(x, y)) {
                playback.choose(hit.choice());
                optionHits.clear();
                return;
            }
        }
        if (TerminalGraphicsRenderer.contains(x, y, X + shift, Y, WIDTH, HEIGHT, SLANT)) {
            playback.advance();
        }
    }

    void scroll(double delta) {
        scroll = Mth.clamp(scroll - delta * 26, 0, maxScroll());
        autoScroll = scroll >= maxScroll();
    }

    void render(GuiGraphics graphics, long now, float reveal, int shift, float windowOpacity, int mouseX, int mouseY,
                boolean interactive) {
        boolean locked = isLocked();
        if (!locked) {
            graphics.drawString(font, font.plainSubstrByWidth(node.title().getString(), 298), TEXT_LEFT + shift,
                                TITLE_Y, TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, reveal),
                                false);
        }
        int border = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, reveal);
        graphics.fill(TEXT_LEFT - 2 + shift, RULE_Y, TEXT_LEFT - 2 + RULE_WIDTH + shift, RULE_Y + 1, border);
        int right = locked ? TEXT_RIGHT : textRight();
        var pose = graphics.pose().last().pose();
        Vector3f topLeft = pose.transformPosition(new Vector3f(TEXT_LEFT + shift, TEXT_TOP, 0));
        Vector3f bottomRight = pose.transformPosition(new Vector3f(right + shift, TEXT_BOTTOM, 0));
        graphics.enableScissor((int) topLeft.x, (int) topLeft.y, Mth.ceil(bottomRight.x), Mth.ceil(bottomRight.y));
        try {
            if (locked) {
                renderLockTip(graphics, reveal, shift);
            } else {
                renderRows(graphics, now, reveal, shift);
            }
        } finally {
            graphics.disableScissor();
        }
        if (!locked && node.image() != null) {
            renderImage(graphics, reveal, shift, windowOpacity, border);
        }
        renderScrollbar(graphics, reveal, shift, right, border,
                        locked ? TerminalGraphicsRenderer.LOCKED_ACCENT : TerminalGraphicsRenderer.ACCENT);
        if (locked) {
            return;
        }
        if (playback.waitingForAdvance()) {
            float blink = 0.55F + 0.45F * Mth.sin(now / 260.0F);
            int markerX = right - 16 + shift;
            int markerY = TEXT_BOTTOM - 12 + Math.round(Mth.sin(now / 260.0F) * 1.5F);
            int color = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, reveal * blink);
            TerminalGraphicsRenderer.line(graphics, markerX, markerY, markerX + 4, markerY + 4, color);
            TerminalGraphicsRenderer.line(graphics, markerX + 8, markerY, markerX + 4, markerY + 4, color);
        }
        renderOptions(graphics, now, shift, reveal, mouseX, mouseY, interactive);
    }

    private void renderRows(GuiGraphics graphics, long now, float reveal, int shift) {
        for (TextRow row : rows) {
            int lineY = TEXT_TOP + row.y() - (int) scroll;
            if (lineY < TEXT_TOP - LINE_HEIGHT || lineY >= TEXT_BOTTOM) {
                continue;
            }
            Entry entry = row.entry();
            float past = row.current() ? 0
                    : entry.pastStarted() < 0
                    ? 1
                    : TerminalGraphicsRenderer.ease((now - entry.pastStarted()) / (float) LINE_COLOR_DURATION);
            int color = TerminalGraphicsRenderer.mix(TerminalGraphicsRenderer.TEXT, TerminalGraphicsRenderer.ACCENT,
                                                     past);
            float enter = entry.enteredAt() < 0
                    ? 1
                    : TerminalGraphicsRenderer.ease((now - entry.enteredAt()) /
                                                    (float) TerminalStoryPlayback.ANSWER_ENTER_DURATION);
            if (reveal * enter < 4.0F / 255) {
                continue;
            }
            graphics.pose().pushPose();
            try {
                graphics.pose().translate((1 - enter) * ANSWER_ENTER_DISTANCE, 0, 0);
                String speaker = row.text().substring(0, row.speakerGlyphs());
                float speakerFade = entry.speakerStarted() < 0
                        ? 1
                        : TerminalGraphicsRenderer.ease((now - entry.speakerStarted()) / (float) SPEAKER_FADE_DURATION);
                if (!speaker.isEmpty() && reveal * enter * speakerFade >= 4.0F / 255) {
                    graphics.drawString(font, speaker, TEXT_LEFT + shift, lineY,
                                        TerminalGraphicsRenderer.alpha(color, reveal * enter * speakerFade), false);
                }
                graphics.drawString(font, row.text().substring(row.speakerGlyphs()),
                                    TEXT_LEFT + shift + font.width(speaker), lineY,
                                    TerminalGraphicsRenderer.alpha(color, reveal * enter), false);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private void renderLockTip(GuiGraphics graphics, float reveal, int shift) {
        int y = 0;
        for (String row : lockRows) {
            int lineY = TEXT_TOP + y - (int) scroll;
            if (lineY >= TEXT_TOP - LINE_HEIGHT && lineY < TEXT_BOTTOM) {
                graphics.drawString(font, row, TEXT_LEFT + shift, lineY,
                                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.LOCKED_TEXT, reveal),
                                    false);
            }
            y += LINE_HEIGHT;
        }
    }

    private void renderImage(GuiGraphics graphics, float reveal, int shift, float windowOpacity, int border) {
        float scale = Math.min(110.0F / node.imageWidth(), 138.0F / node.imageHeight());
        int width = Math.max(1, Math.round(node.imageWidth() * scale));
        int height = Math.max(1, Math.round(node.imageHeight() * scale));
        TerminalGraphicsRenderer.panel(
                graphics, 340 + shift, 102, 150, 157, TerminalGraphicsRenderer.slant(157),
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND, reveal), border);
        graphics.setColor(1, 1, 1, reveal * windowOpacity);
        try {
            graphics.blit(node.image(), 361 + shift + (110 - width) / 2, 110 + (138 - height) / 2, width, height, 0, 0,
                          node.imageWidth(), node.imageHeight(), node.imageWidth(), node.imageHeight());
        } finally {
            graphics.setColor(1, 1, 1, windowOpacity);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, float reveal, int shift, int right, int border, int accent) {
        int maxScroll = maxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int height = TEXT_BOTTOM - TEXT_TOP;
        int thumbHeight = Math.max(12, height * height / Math.max(1, textHeight));
        int thumbY = TEXT_TOP + (int) ((height - thumbHeight) * scroll / maxScroll);
        graphics.fill(right - 2 + shift, TEXT_TOP, right - 1 + shift, TEXT_BOTTOM, border);
        graphics.fill(right - 2 + shift, thumbY, right - 1 + shift, thumbY + thumbHeight,
                      TerminalGraphicsRenderer.alpha(accent, reveal));
    }

    private void renderOptions(GuiGraphics graphics, long now, int shift, float reveal, int mouseX, int mouseY,
                               boolean interactive) {
        optionHits.clear();
        if (playback.typing() || !interactive) {
            return;
        }
        List<Choice> choices = playback.choices();
        int right = X + WIDTH - 22;
        int bottom = Y + HEIGHT - 14;
        for (int i = 0; i < choices.size(); i++) {
            Choice option = choices.get(i);
            float progress = playback.choiceStarted() < 0
                    ? 1
                    : Mth.clamp((now - playback.choiceStarted() - Math.min(i, 5) * CHOICE_REVEAL_STAGGER) /
                                        (float) CHOICE_REVEAL_DURATION,
                                0, 1);
            if (progress <= 0) {
                continue;
            }
            float appear = TerminalGraphicsRenderer.ease(progress);
            String text = TerminalStoryText.plainOf(option.text());
            int width = Mth.clamp(font.width(text) + 26, 56, CHOICE_MAX_WIDTH);
            int x = right - width + Math.round((1 - appear) * 12) + shift;
            int y = bottom - CHOICE_HEIGHT - i * (CHOICE_HEIGHT + CHOICE_GAP);
            boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + CHOICE_HEIGHT;
            TerminalStoryChoiceRenderer.render(graphics, font, x, y, width, CHOICE_HEIGHT,
                                               font.plainSubstrByWidth(text, width - 20), hovered, reveal, progress,
                                               now, option.index());
            optionHits.add(new OptionHit(x, y, x + width, y + CHOICE_HEIGHT, option));
        }
    }

    private int textRight() {
        return node.image() == null ? TEXT_RIGHT : IMAGE_TEXT_RIGHT;
    }

    private int maxScroll() {
        return Math.max(0, textHeight - (TEXT_BOTTOM - TEXT_TOP));
    }

    private List<String> unlockTip() {
        if (node.tip() != null && !node.tip().isBlank()) {
            return List.of(Component.translatable(node.tip()).getString());
        }
        return List.of(label("locked.requires").getString(), conditionText(node.availableWhen()));
    }

    private String conditionText(TerminalStoryCondition condition) {
        if (condition instanceof TerminalStoryCondition.Group group) {
            String separator = label(group.all() ? "locked.and" : "locked.or").getString();
            return String.join(separator,
                               group.conditions().stream().map(child -> "(" + conditionText(child) + ")").toList());
        }
        if (condition instanceof TerminalStoryCondition.Not not) {
            return Component.translatable("gui.ae2pr.proto_terminal.locked.not", conditionText(not.condition()))
                    .getString();
        }
        if (condition instanceof TerminalStoryCondition.Completed completed) {
            String name = story.nodes()
                                  .stream()
                                  .filter(candidate -> candidate.id().equals(completed.id()))
                                  .map(candidate -> candidate.title().getString())
                                  .findFirst()
                                  .orElse(completed.id());
            return Component.translatable("gui.ae2pr.proto_terminal.locked.completed", name).getString();
        }
        if (condition instanceof TerminalStoryCondition.Variable variable) {
            String operator = switch (variable.operator()) {
                case "at_least" -> "≥";
                case "at_most" -> "≤";
                default -> "=";
            };
            return variable.name() + " " + operator + " " + variable.expected();
        }
        if (condition instanceof TerminalStoryCondition.Inventory inventory) {
            TerminalStoryUnlockCondition unlock = TerminalStoryUnlockCondition.parse(inventory.key());
            return Component
                    .translatable("gui.ae2pr.proto_terminal.locked.condition", unlockName(unlock), unlock.count())
                    .getString();
        }
        return label("locked.requires").getString();
    }

    private static Component unlockName(TerminalStoryUnlockCondition unlock) {
        if (unlock.tag()) {
            return Component.translatable("gui.ae2pr.proto_terminal.locked.tag", unlock.id().toString());
        }
        Item item = ForgeRegistries.ITEMS.getValue(unlock.id());
        return item == null ? Component.literal(unlock.id().toString()) : new ItemStack(item).getHoverName();
    }

    private static Component label(String suffix) {
        return Component.translatable("gui.ae2pr.proto_terminal." + suffix);
    }
}
