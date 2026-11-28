package com.murthinext.ae2pr.item.proto_terminal.screen;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.item.proto_terminal.story.Progress;
import com.murthinext.ae2pr.item.proto_terminal.story.Progress.Line;
import com.murthinext.ae2pr.item.proto_terminal.story.Progress.NodeProgress;
import com.murthinext.ae2pr.item.proto_terminal.story.ProgressCache;
import com.murthinext.ae2pr.item.proto_terminal.story.Runner;
import com.murthinext.ae2pr.item.proto_terminal.story.Runner.Choice;
import com.murthinext.ae2pr.item.proto_terminal.story.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;

final class Playback {

    static final long OPTION_ENTER_DURATION = 320;

    private final Runner runner;
    private final String nodeId;
    private final Consumer<String> narrator;
    private final List<Entry> log = new ArrayList<>();
    private final List<Entry> logView = Collections.unmodifiableList(log);
    private NodeProgress progress;
    private List<Choice> choices = List.of();
    private long choiceStarted = -1;
    private PendingLine pendingLine;
    private boolean typing;
    private int typedGlyphs;
    private long typingStarted;
    private boolean failed;

    // held 为真表示这句话要等玩家点击，否则在 revealAt 之后自动出现
    private record PendingLine(Line line, long revealAt) {

        static PendingLine held(Line line) {
            return new PendingLine(line, -1);
        }

        boolean isHeld() {
            return revealAt < 0;
        }
    }

    static final class Entry {

        private final Text text;
        private final String prefix;
        private List<String> wrapped = List.of();
        private int wrappedWidth = -1;
        private final long enteredAt;
        private final long speakerStarted;
        private long pastStarted = -1;

        private Entry(Line line, long now) {
            text = Text.of(line.text());
            prefix = Text.plainOf(line.prefix());
            enteredAt = line.option() ? now : -1;
            speakerStarted = now;
        }

        List<String> wrapped(Font font, int width) {
            if (wrappedWidth != width) {
                wrapped = Text.wrap(font, plain(), width);
                wrappedWidth = width;
            }
            return wrapped;
        }

        int length() {
            return prefixLength() + text.length();
        }

        String plain() {
            return prefix + text.plain();
        }

        int prefixLength() {
            return prefix.length();
        }

        long enteredAt() {
            return enteredAt;
        }

        long speakerStarted() {
            return speakerStarted;
        }

        long pastStarted() {
            return pastStarted;
        }

        int glyphsAt(long elapsedMillis) {
            return prefixLength() + text.glyphsAt(elapsedMillis / 1000.0);
        }

        long durationAt(int glyphs) {
            return Math.round(text.durationAt(glyphs - prefixLength()) * 1000);
        }
    }

    Playback(Runner runner, Tree.Node node, Consumer<String> narrator) {
        this.runner = runner;
        this.nodeId = node.id();
        this.narrator = narrator;
        try {
            long now = Util.getMillis();
            progress = runner.open(node.id(), node.entry());
            for (Line line : progress.log) {
                log.add(new Entry(line, -1));
            }
            var playback = progress.playback;
            Entry current = current();
            typedGlyphs = current == null ? 0 : Mth.clamp(playback.typed, current.prefixLength(), current.length());
            typing = current != null && playback.typing && typedGlyphs < current.length();
            if (typing) {
                typingStarted = now - current.durationAt(typedGlyphs);
            }
            if (playback.pendingLine != null) {
                pendingLine = playback.held
                        ? PendingLine.held(playback.pendingLine)
                        : new PendingLine(playback.pendingLine, now + Mth.clamp(playback.delay, 0, 60000));
            }
            if (log.isEmpty() && pendingLine == null && !progress.finished) {
                revealNext(now);
            } else {
                refreshChoicesAndCompletion(now);
            }
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    List<Entry> log() {
        return logView;
    }

    Entry current() {
        return log.isEmpty() ? null : log.get(log.size() - 1);
    }

    List<Choice> choices() {
        return choices;
    }

    long choiceStarted() {
        return choiceStarted;
    }

    boolean typing() {
        return typing;
    }

    int typedGlyphs() {
        return typedGlyphs;
    }

    boolean failed() {
        return failed;
    }

    boolean waitingForAdvance() {
        return !failed
                && !typing
                && choices.isEmpty()
                && (pendingLine == null ? runner.canContinue() : pendingLine.isHeld());
    }

    void update(long now) {
        if (failed) {
            return;
        }
        try {
            if (pendingLine != null && !pendingLine.isHeld() && now >= pendingLine.revealAt()) {
                startLine(pendingLine.line(), now);
            }
            Entry current = current();
            if (typing && current != null) {
                typedGlyphs = Math.min(current.length(), Math.max(typedGlyphs, current.glyphsAt(now - typingStarted)));
                if (typedGlyphs >= current.length()) {
                    finishTyping(now);
                }
            }
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    void advance() {
        if (failed || pendingLine != null && !pendingLine.isHeld()) {
            return;
        }
        try {
            long now = Util.getMillis();
            if (typing) {
                finishTyping(now);
            } else if (pendingLine != null) {
                startLine(pendingLine.line(), now);
            } else if (waitingForAdvance()) {
                revealNext(now);
            }
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    void choose(Choice choice) {
        if (failed || typing || !choices.contains(choice)) {
            return;
        }
        try {
            choices = List.of();
            choiceStarted = -1;
            runner.choose(choice.id());
            revealNext(Util.getMillis());
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    void save() {
        if (failed || progress == null) {
            return;
        }
        try {
            persist(Util.getMillis());
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    private void persist(long now) {
        var playback = progress.playback;
        playback.typing = typing;
        playback.typed = typedGlyphs;
        playback.pendingLine = pendingLine == null ? null : pendingLine.line();
        playback.held = pendingLine != null && pendingLine.isHeld();
        playback.delay =
                pendingLine == null || playback.held ? 0 : (int) Mth.clamp(pendingLine.revealAt() - now, 0L, 60000L);
        ProgressCache.record(runner.snapshot(), nodeId);
    }

    private void revealNext(long now) {
        typing = false;
        choices = List.of();
        Line line = runner.advance();
        if (line != null) {
            if (line.delay() > 0) {
                pendingLine = new PendingLine(line, now + line.delay());
                persist(now);
            } else {
                startLine(line, now);
            }
            return;
        }
        refreshChoicesAndCompletion(now);
        persist(now);
    }

    private void startLine(Line line, long now) {
        pendingLine = null;
        if (!log.isEmpty()) {
            current().pastStarted = now;
        }
        Entry entry = new Entry(line, now);
        log.add(entry);
        runner.append(line);
        if (log.size() > Progress.MAX_LOG_LINES) {
            log.remove(0);
        }
        typing = !line.option();
        typedGlyphs = typing ? entry.prefixLength() : entry.length();
        typingStarted = now;
        narrator.accept(entry.plain());
        persist(now);
        if (line.option()) {
            queueAfterOption(now);
        }
    }

    /**
     * 选项之后的下一句话：带 {@code reply} 标签或显式 delay 时按 delay 自己出现，
     * 否则先取下来挂起，等玩家点击才显示。
     */
    private void queueAfterOption(long now) {
        Line line = runner.advance();
        if (line == null) {
            refreshChoicesAndCompletion(now);
            persist(now);
            return;
        }
        pendingLine =
                line.reply() || line.delay() > 0 ? new PendingLine(line, now + line.delay()) : PendingLine.held(line);
        persist(now);
    }

    private void finishTyping(long now) {
        typing = false;
        typedGlyphs = current() == null ? 0 : current().length();
        refreshChoicesAndCompletion(now);
        persist(now);
    }

    private void refreshChoicesAndCompletion(long now) {
        if (typing || pendingLine != null) {
            return;
        }
        List<Choice> available = runner.choices();
        if (!available.equals(choices)) {
            choices = available;
            choiceStarted = now;
        }
        if (!progress.finished && choices.isEmpty() && !runner.canContinue()) {
            runner.finish();
        }
    }

    private void fail(RuntimeException exception) {
        ae2pr.LOGGER.error("Failed to play proto terminal story node {}", nodeId, exception);
        failed = true;
        choices = List.of();
        typing = false;
        pendingLine = null;
    }
}
