package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.proto_terminal.TerminalStoryTree.Unlock;
import com.murthinext.ae2pr.logic.terminal.TerminalUnlockCondition;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 源始终端的剧情树与图文展示界面。
 */
public final class TerminalScreen extends Screen {

    private static final int CANVAS_WIDTH = 548;
    private static final int CANVAS_HEIGHT = 340;
    private static final int MAIN_X = 4;
    private static final int MAIN_Y = 8;
    private static final int MAIN_WIDTH = 540;
    private static final int MAIN_HEIGHT = 324;
    private static final float MAIN_SLANT = TerminalGraphicsRenderer.slant(MAIN_HEIGHT);
    private static final int TITLE_HEIGHT = 34;
    private static final int TITLE_WIDTH = 171;
    private static final float TITLE_SLANT = TerminalGraphicsRenderer.slant(TITLE_HEIGHT);
    private static final float TITLE_X = MAIN_X + MAIN_SLANT - TITLE_SLANT;
    private static final int CONTROL_SIZE = 34;
    private static final int CLOSE_X = MAIN_X + MAIN_WIDTH - CONTROL_SIZE;
    private static final int TREE_LEFT = 52;
    private static final int TREE_TOP = 68;
    private static final int NODE_WIDTH = 144;
    private static final int NODE_HEIGHT = 46;
    private static final int NODE_ENTER_DISTANCE = 24;
    private static final int DETAIL_X = 38;
    private static final int DETAIL_Y = 58;
    private static final int DETAIL_WIDTH = 472;
    private static final int DETAIL_HEIGHT = 242;
    private static final float DETAIL_SLANT = TerminalGraphicsRenderer.slant(DETAIL_HEIGHT);
    private static final int BACK_X = DETAIL_X + DETAIL_WIDTH - CONTROL_SIZE;
    // 详情页标题紧贴分隔线上方，分隔线缩短对齐返回按钮底边
    private static final int DETAIL_TITLE_Y = 75;
    private static final int DETAIL_RULE_Y = DETAIL_Y + CONTROL_SIZE - 1;
    private static final int DETAIL_RULE_WIDTH = 102;
    private static final int TEXT_LEFT = 72;
    private static final int TEXT_TOP = 105;
    private static final int TEXT_BOTTOM = 280;
    private static final int TEXT_RIGHT = 478;
    private static final int IMAGE_TEXT_RIGHT = 320;
    private static final long DETAIL_ENTER_DURATION = 330;
    private static final long DETAIL_EXIT_DURATION = 240;
    private static final long TERMINAL_EXIT_DURATION = 260;
    // 父块读完后，子块连线的起始停顿
    private static final long POST_FINISH_DELAY = 340;
    // 剧情推进：选项堆叠尺寸与弹出时长
    private static final int CHOICE_HEIGHT = 20;
    private static final int CHOICE_GAP = 5;
    private static final int CHOICE_MAX_WIDTH = 244;
    private static final long CHOICE_REVEAL_DURATION = 360;
    private static final long CHOICE_REVEAL_STAGGER = 38;
    private static final long ANSWER_ENTER_DURATION = 320;
    private static final float ANSWER_ENTER_DISTANCE = 32;
    private static final long SPEAKER_FADE_DURATION = 220;
    private static final long LINE_COLOR_DURATION = 420;
    private static final int LINE_HEIGHT = 13;
    // Ctrl 滚轮缩放的范围、步进与缓动时长
    private static final float ZOOM_STEP = 0.2F;
    private static final float DEFAULT_ZOOM = 0.7F;
    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 3F;
    private static final long ZOOM_DURATION = 200;

    private final TerminalStoryTree story;
    private final TerminalAnimation animation;
    private final TerminalOpeningPixelRenderer openingPixels = new TerminalOpeningPixelRenderer(MAIN_X, MAIN_Y,
            MAIN_WIDTH, MAIN_HEIGHT);
    private final TerminalStarfieldRenderer starfield = new TerminalStarfieldRenderer(MAIN_X, MAIN_Y,
            MAIN_WIDTH, MAIN_HEIGHT);
    private final List<Star> stars = new ArrayList<>();
    private final List<TerminalButton> nodeButtons = new ArrayList<>();
    // 每个剧情块连线动画相对终端展开起点的开始时间；父块读完后才登记
    private final Map<String, Long> revealAt = new HashMap<>();
    private long openingStarted = -1;
    private TerminalStoryTree.Node selectedNode;
    private boolean lockedNode;
    private final List<String> lockRows = new ArrayList<>();
    private final List<StoryLog> storyLog = new ArrayList<>();
    private final List<TextRow> storyRows = new ArrayList<>();
    private final Deque<StoryFrame> storyFrames = new ArrayDeque<>();
    private final Set<String> storyTags = new HashSet<>();
    private Map<TerminalStoryTree.Line, Set<Integer>> consumedChoices = new IdentityHashMap<>();
    private final List<OptionHit> optionHits = new ArrayList<>();
    private TerminalStoryTree.Line waitingBranch;
    private StoryLog waitingBranchRef;
    private PendingChoice pendingChoice;
    private PendingLine pendingLine;
    private boolean typing;
    // 当前剧情块是否已完整读完；写进存档用于解锁子块
    private boolean finished;
    private int typedGlyphs;
    private long typingStarted;
    private int storyTextHeight;
    private boolean autoScroll = true;
    private long choiceStarted = -1;
    private TerminalButton backButton;
    private TerminalButton closeButton;
    private long detailStarted;
    private long detailClosingStarted = -1;
    private long terminalClosingStarted = -1;
    private float detailOpacity;
    private int detailShift = -18;
    private float detailExitOpacity;
    private int detailExitShift;
    private float windowOpacity = 1;
    private List<TerminalParallelogramClip.Band> treeClipBands = List.of();
    private double detailScroll;
    private float panX;
    private float panY;
    private boolean panning;
    private float contentZoom = DEFAULT_ZOOM;
    private float zoomFrom = DEFAULT_ZOOM;
    private float zoomTarget = DEFAULT_ZOOM;
    private float zoomAnchorX;
    private float zoomAnchorY;
    private long zoomStarted;
    private float canvasScale;
    private float canvasLeft;
    private float canvasTop;
    private int framebufferWidth;
    private int framebufferHeight;

    private record Star(float x, float y, float phase, float speed, boolean cross) {
    }

    /**
     * 累积日志中的一条已读台词，按当前逐字进度缓存排版结果。
     */
    private static final class StoryLog {

        private final TerminalStoryTree.Line line;
        private final TerminalStoryText text;
        private final String prefix;
        private final List<Integer> path;
        private final int index;
        private final int answer;
        private final List<String> wrapped = new ArrayList<>();
        private int wrappedWidth = -1;
        private long enteredAt = -1;
        private long speakerStarted = -1;
        private long pastStarted = -1;

        StoryLog(TerminalStoryTree.Line line, List<Integer> path, int index) {
            this(line, path, index, -1);
        }

        StoryLog(TerminalStoryTree.Line line, List<Integer> path, int index, int answer) {
            this.line = line;
            this.text = TerminalStoryText.of(line.text());
            this.prefix = line.prefix();
            this.path = List.copyOf(path);
            this.index = index;
            this.answer = answer;
        }

        int answer() {
            return answer;
        }

        TerminalStoryTree.Line line() {
            return line;
        }

        List<Integer> path() {
            return path;
        }

        int index() {
            return index;
        }

        List<String> wrapped(Font font, int width) {
            if (wrappedWidth != width) {
                wrapped.clear();
                wrap(font, plain(), width, wrapped);
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

        /**
         * 已显示的字符数（含说话人前缀）；标记的停顿已计入。
         */
        int glyphsAt(long elapsedMillis) {
            return prefixLength() + text.glyphsAt(elapsedMillis / 1000.0);
        }

        /**
         * 显示到指定字符数所需的时间，用于存档恢复续播。
         */
        long durationAt(int glyphs) {
            return Math.round(text.durationAt(glyphs - prefixLength()) * 1000);
        }
    }

    private static void wrap(Font font, String text, int width, List<String> lines) {
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            String rest = paragraph;
            while (!rest.isEmpty()) {
                String piece = font.plainSubstrByWidth(rest, width);
                if (piece.isEmpty()) {
                    piece = rest.substring(0, 1);
                } else if (piece.length() < rest.length()) {
                    int space = piece.lastIndexOf(' ');
                    if (space > 0) {
                        piece = piece.substring(0, space);
                    }
                }
                lines.add(piece);
                rest = rest.substring(piece.length());
                while (rest.startsWith(" ")) {
                    rest = rest.substring(1);
                }
            }
        }
    }

    /**
     * 已排版的日志行。
     */
    private record TextRow(String text, int speakerGlyphs, int y, StoryLog log, boolean current) {
    }

    /**
     * 一段待播放的台词序列。
     */
    private static final class StoryFrame {

        private final List<TerminalStoryTree.Line> lines;
        private final List<Integer> path;
        private final TerminalStoryTree.Line branch;
        private final boolean returnToBranch;
        private int index;

        StoryFrame(List<TerminalStoryTree.Line> lines, List<Integer> path, TerminalStoryTree.Line branch,
                boolean returnToBranch) {
            this.lines = lines;
            this.path = List.copyOf(path);
            this.branch = branch;
            this.returnToBranch = returnToBranch;
        }
    }

    private record Option(int index, TerminalStoryTree.Choice choice) {
    }

    private record PendingChoice(TerminalStoryTree.Choice choice, StoryLog branchRef, int index, long continueAt) {
    }

    private record PendingLine(StoryLog entry, long revealAt) {
    }

    private record OptionHit(int left, int top, int right, int bottom, TerminalStoryTree.Choice choice) {

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    private TerminalScreen(TerminalStoryTree story) {
        super(Component.literal(TerminalTitleRenderer.TEXT));
        this.story = story;
        this.animation = new TerminalAnimation(story.nodes());
        Random random = new Random(0xAE2C1257L);
        for (int i = 0; i < 90; i++) {
            stars.add(new Star(MAIN_X + random.nextFloat() * MAIN_WIDTH, MAIN_Y + random.nextFloat() * MAIN_HEIGHT,
                    random.nextFloat() * 6.28F,
                    0.3F + random.nextFloat() * 0.6F, i % 11 == 0));
        }
        // 星空与初始缩放保持一致，后续缩放按同一基准换算
        starfield.zoom(DEFAULT_ZOOM, MAIN_X + MAIN_WIDTH / 2.0F, MAIN_Y + MAIN_HEIGHT / 2.0F);
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof TerminalScreen)) {
            TerminalFadeOutRenderer.clear();
            TerminalStoryTree story = TerminalStoryTree.load();
            TerminalStoryProgressCache.request(unlockKeys(story));
            minecraft.setScreen(new TerminalScreen(story));
        }
    }

    // 需要的解锁条件
    private static List<String> unlockKeys(TerminalStoryTree story) {
        List<String> keys = new ArrayList<>();
        for (TerminalStoryTree.Node node : story.nodes()) {
            for (Unlock unlock : node.unlocks()) {
                String key = TerminalUnlockCondition.key(unlock.id(), unlock.tag(), unlock.count());
                if (key.length() <= TerminalUnlockCondition.MAX_KEY_LENGTH && !keys.contains(key)) {
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    static void onProgressSynced(boolean reset) {
        if (Minecraft.getInstance().screen instanceof TerminalScreen screen) {
            if (reset) {
                screen.applyProgressReset();
            } else {
                screen.storyTags.addAll(TerminalStoryProgressCache.get().tags);
                screen.updateVisibility();
            }
        }
    }

    private void applyProgressReset() {
        storyTags.clear();
        storyTags.addAll(TerminalStoryProgressCache.get().tags);
        revealAt.clear();
        if (selectedNode != null) {
            showTree();
        }
        updateVisibility();
    }

    @Override
    protected void init() {
        canvasScale = Math.max(0.1F, Math.min(1.5F, Math.min((width - 12.0F) / CANVAS_WIDTH,
                (height - 12.0F) / CANVAS_HEIGHT)));
        canvasLeft = (width - CANVAS_WIDTH * canvasScale) / 2;
        canvasTop = (height - CANVAS_HEIGHT * canvasScale) / 2;
        updateTreeClipBands();
        framebufferWidth = minecraft.getWindow().getWidth();
        framebufferHeight = minecraft.getWindow().getHeight();
        nodeButtons.clear();
        for (TerminalStoryTree.Node node : story.nodes()) {
            nodeButtons.add(addRenderableWidget(new TerminalButton(0, 0, NODE_WIDTH, NODE_HEIGHT,
                    node.title(), button -> showNode(node))));
        }
        backButton = addRenderableWidget(new TerminalButton(BACK_X, DETAIL_Y, CONTROL_SIZE,
                TerminalButton.Icon.BACK, label("back"), button -> closeDetail()));
        closeButton = addRenderableWidget(new TerminalButton(CLOSE_X, MAIN_Y, CONTROL_SIZE,
                TerminalButton.Icon.CLOSE, label("close"), button -> closeTerminal()));
        updateNodePositions(0);
        updateVisibility();
    }

    private boolean isInteractive() {
        return terminalClosingStarted < 0 && openingStarted >= 0
                && Util.getMillis() - openingStarted >= TerminalAnimation.OPEN_DURATION;
    }

    /**
     * 平滑缩放。
     */
    private void zoomContent(double mouseX, double mouseY, double amount) {
        float target = Mth.clamp(zoomTarget + (float) amount * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        if (target == zoomTarget) {
            return;
        }
        zoomFrom = contentZoom;
        zoomStarted = Util.getMillis();
        zoomAnchorX = (float) localX(mouseX);
        zoomAnchorY = (float) localY(mouseY);
        zoomTarget = target;
    }

    private void updateZoom(long now) {
        if (contentZoom == zoomTarget) {
            return;
        }
        float progress = (now - zoomStarted) / (float) ZOOM_DURATION;
        float zoom = progress >= 1 ? zoomTarget
                : Mth.lerp(TerminalGraphicsRenderer.ease(progress), zoomFrom, zoomTarget);
        panX = (float) Mth.clamp(zoomAnchorX - TREE_LEFT
                - (zoomAnchorX - TREE_LEFT - panX) / contentZoom * zoom, -4352, 4352);
        panY = (float) Mth.clamp(zoomAnchorY - TREE_TOP
                - (zoomAnchorY - TREE_TOP - panY) / contentZoom * zoom, -4352, 4352);
        starfield.zoom(zoom, zoomAnchorX, zoomAnchorY);
        contentZoom = zoom;
    }

    private void updateTreeClipBands() {
        double guiScale = minecraft.getWindow().getGuiScale();
        treeClipBands = TerminalParallelogramClip.bands((canvasLeft + MAIN_X * canvasScale) * guiScale,
                (canvasTop + MAIN_Y * canvasScale) * guiScale, MAIN_WIDTH * canvasScale * guiScale,
                MAIN_HEIGHT * canvasScale * guiScale, MAIN_SLANT * canvasScale * guiScale);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (openingStarted < 0) {
            openingStarted = now;
        }
        updateTransitions(now);
        updateZoom(now);
        updateStory(now);
        if (terminalClosingStarted >= 0 && windowOpacity <= 0) {
            return;
        }
        long elapsed = (terminalClosingStarted < 0 ? now : terminalClosingStarted) - openingStarted;
        updateReveals(elapsed);
        float seconds = elapsed / 1000.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(canvasLeft, canvasTop, 0);
        graphics.pose().scale(canvasScale, canvasScale, 1);
        graphics.setColor(1, 1, 1, windowOpacity);
        try {
            float progress = animation.opening(elapsed);
            renderTerminal(graphics, progress, seconds);
            updateNodePositions(elapsed);
            closeButton.setOpacity(animation.headerOpacity(elapsed));
            updateVisibility();
            int localMouseX = (int) localX(mouseX);
            int localMouseY = (int) localY(mouseY);
            if (elapsed >= TerminalAnimation.CONTENT_START) {
                renderTree(graphics, localMouseX, localMouseY, partialTick, elapsed);
                renderTitle(graphics, elapsed);
                if (selectedNode != null) {
                    TerminalGraphicsRenderer.panel(graphics, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT,
                            TerminalGraphicsRenderer.alpha(0x020712, detailOpacity * 0.72F), 0);
                    renderDetail(graphics, now, detailOpacity, localMouseX, localMouseY);
                    backButton.render(graphics, localMouseX, localMouseY, partialTick);
                }
                closeButton.render(graphics, localMouseX, localMouseY, partialTick);
            }
            if (progress < 1) {
                openingPixels.renderFrontier(graphics, progress, seconds);
            } else {
                graphics.drawManaged(() -> TerminalGraphicsRenderer.outline(graphics,
                        MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT, TerminalGraphicsRenderer.BORDER));
            }
        } finally {
            graphics.setColor(1, 1, 1, 1);
            graphics.pose().popPose();
        }
    }

    boolean isFadeOutFinished() {
        return terminalClosingStarted >= 0 && Util.getMillis() - terminalClosingStarted >= TERMINAL_EXIT_DURATION;
    }

    void renderFadeOut(GuiGraphics graphics, float partialTick) {
        if (width != minecraft.getWindow().getGuiScaledWidth() || height != minecraft.getWindow().getGuiScaledHeight()
                || framebufferWidth != minecraft.getWindow().getWidth()
                || framebufferHeight != minecraft.getWindow().getHeight()) {
            resize(minecraft, minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        }
        render(graphics, -10000, -10000, partialTick);
    }

    private void renderTerminal(GuiGraphics graphics, float progress, float seconds) {
        if (progress < 1) {
            openingPixels.update(progress, seconds);
            openingPixels.renderSurface(graphics, starfield, seconds);
        } else {
            starfield.renderNebula(graphics, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT, seconds);
        }
        starfield.renderStars(graphics, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT, seconds,
                TerminalGraphicsRenderer.ease(progress), progress < 1 ? openingPixels : null);
    }

    private void renderTitle(GuiGraphics graphics, long elapsed) {
        float opacity = animation.headerOpacity(elapsed);
        if (opacity < 4.0F / 255) {
            return;
        }
        TerminalGraphicsRenderer.panel(graphics, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT,
                TerminalGraphicsRenderer.alpha(0xFF0D1D35, opacity),
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, opacity));
        TerminalTitleRenderer.render(graphics, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT,
                opacity, elapsed - TerminalAnimation.HEADER_START);
    }

    private void renderStarlight(GuiGraphics graphics, int x, int y, int panelWidth, int panelHeight,
            float slant, float seconds, float opacity) {
        if (panelWidth <= slant || panelHeight <= 0) {
            return;
        }
        graphics.drawManaged(() -> {
            for (Star star : stars) {
                int px = (int) star.x();
                int py = (int) star.y();
                if (!TerminalGraphicsRenderer.contains(px, py, x + 2, y + 2, panelWidth - 4, panelHeight - 4, slant)) {
                    continue;
                }
                float light = (0.08F + 0.19F * (0.5F + 0.5F
                        * Mth.sin(seconds * star.speed() + star.phase()))) * opacity;
                graphics.fill(px, py, px + 1, py + 1, TerminalGraphicsRenderer.alpha(0xB0CDED, light));
                if (star.cross()
                        && TerminalGraphicsRenderer.contains(px - 2, py - 2, x, y, panelWidth, panelHeight, slant)
                        && TerminalGraphicsRenderer.contains(px + 2, py + 2, x, y, panelWidth, panelHeight, slant)) {
                    int glow = TerminalGraphicsRenderer.alpha(0x77A7D3, light * 0.3F);
                    graphics.fill(px - 2, py, px + 3, py + 1, glow);
                    graphics.fill(px, py - 2, px + 1, py + 3, glow);
                }
            }
        });
    }

    private void renderTree(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, long elapsed) {
        double guiScale = minecraft.getWindow().getGuiScale();
        List<TerminalParallelogramClip.Band> clipBands = treeClipBands;
        if (elapsed < TerminalAnimation.OPEN_DURATION) {
            clipBands = new ArrayList<>();
            for (TerminalOpeningPixelRenderer.Band band : openingPixels.bands()) {
                int left = (int) Math.ceil((canvasLeft + band.left() * canvasScale) * guiScale - 0.5);
                int top = (int) Math.ceil((canvasTop + band.top() * canvasScale) * guiScale - 0.5);
                int right = (int) Math.ceil((canvasLeft + band.right() * canvasScale) * guiScale - 0.5);
                int bottom = (int) Math.ceil((canvasTop + band.bottom() * canvasScale) * guiScale - 0.5);
                if (right > left && bottom > top) {
                    clipBands.add(new TerminalParallelogramClip.Band(left, top, right, bottom));
                }
            }
        }
        boolean wasScissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] previousScissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previousScissor);
        graphics.flush();
        try {
            for (TerminalParallelogramClip.Band band : clipBands) {
                RenderSystem.enableScissor(band.left(), minecraft.getWindow().getHeight() - band.bottom(),
                        band.right() - band.left(), band.bottom() - band.top());
                float top = (float) (band.top() / guiScale - canvasTop) / canvasScale;
                float bottom = (float) (band.bottom() / guiScale - canvasTop) / canvasScale;
                float left = (float) (band.left() / guiScale - canvasLeft) / canvasScale;
                float right = (float) (band.right() / guiScale - canvasLeft) / canvasScale;
                renderTreeBand(graphics, mouseX, mouseY, partialTick, elapsed, left, top, right, bottom);
                graphics.flush();
            }
        } finally {
            if (wasScissored) {
                RenderSystem.enableScissor(previousScissor[0], previousScissor[1], previousScissor[2], previousScissor[3]);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }

    private void renderTreeBand(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
            long elapsed, float left, float top, float right, float bottom) {
        graphics.drawManaged(() -> {
            for (TerminalStoryTree.Node node : story.nodes()) {
                float progress = connectionProgress(elapsed, node);
                if (progress <= 0) {
                    continue;
                }
                boolean connected = false;
                for (TerminalStoryTree.Node source : animation.sources(node)) {
                    if (!isNodeVisible(source)) {
                        continue;
                    }
                    connected = true;
                    if (TREE_TOP + panY + (Math.max(source.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom + 1 >= top
                            && TREE_TOP + panY + (Math.min(source.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom
                                    - 1 < bottom) {
                        renderConnection(graphics, source, node, progress);
                    }
                }
                float reveal = nodeReveal(elapsed, node);
                if (!connected && reveal < 1) {
                    float y = treeY(node.y() + NODE_HEIGHT / 2.0F);
                    if (y + 1 >= top && y - 1 < bottom) {
                        float x2 = treeX(node.x() + TerminalGraphicsRenderer.slant(NODE_HEIGHT) / 2);
                        float edge = MAIN_X + MAIN_SLANT * (1 - (y - MAIN_Y) / MAIN_HEIGHT);
                        float x1 = Math.min(edge, x2 - 36 * contentZoom);
                        TerminalGraphicsRenderer.line(graphics, x1, y, Mth.lerp(progress, x1, x2), y,
                                TerminalGraphicsRenderer.alpha(0xFF3B5D83, 1 - reveal), Math.max(1, contentZoom));
                    }
                }
            }
        });
        boolean mouseInside = (selectedNode == null || detailClosingStarted >= 0) && insideTree(mouseX, mouseY);
        for (TerminalButton button : nodeButtons) {
            if (button.getY() + button.getHeight() * contentZoom >= top && button.getY() < bottom
                    && button.getX() + button.getWidth() * contentZoom >= left && button.getX() < right) {
                button.render(graphics, mouseInside ? mouseX : -10000, mouseInside ? mouseY : -10000, partialTick);
            }
        }
    }

    private void renderConnection(GuiGraphics graphics, TerminalStoryTree.Node source,
            TerminalStoryTree.Node target, float progress) {
        float nodeSlant = TerminalGraphicsRenderer.slant(NODE_HEIGHT);
        float stepX = target.x() - source.x();
        float stepY = target.y() - source.y();
        boolean vertical = Math.abs(stepX) - NODE_WIDTH + nodeSlant
                < Math.abs(stepY) * nodeSlant / NODE_HEIGHT
                && Math.abs(stepY) - NODE_HEIGHT >= Math.abs(stepX) * NODE_HEIGHT / nodeSlant;
        float x1;
        float y1;
        float x2;
        float y2;
        if (vertical) {
            x1 = treeX(source.x() + NODE_WIDTH / 2.0F);
            x2 = treeX(target.x() + NODE_WIDTH / 2.0F);
            y1 = treeY(stepY < 0 ? source.y() : source.y() + NODE_HEIGHT);
            y2 = treeY(stepY < 0 ? target.y() + NODE_HEIGHT : target.y());
        } else {
            boolean forward = stepX >= 0;
            float leftEdge = nodeSlant / 2;
            float rightEdge = NODE_WIDTH - nodeSlant / 2;
            x1 = treeX(source.x() + (forward ? rightEdge : leftEdge));
            y1 = treeY(source.y() + NODE_HEIGHT / 2.0F);
            x2 = treeX(target.x() + (forward ? leftEdge : rightEdge));
            y2 = treeY(target.y() + NODE_HEIGHT / 2.0F);
        }
        TerminalGraphicsRenderer.connection(graphics, x1, y1, x2, y2, nodeSlant, NODE_HEIGHT, vertical,
                progress, 0xFF3B5D83, Math.max(1, contentZoom));
    }
    private float treeX(float contentX) {
        return snap(TREE_LEFT + panX + contentX * contentZoom, deviceScale());
    }

    private float treeY(float contentY) {
        return snap(TREE_TOP + panY + contentY * contentZoom, deviceScale());
    }

    private void renderDetail(GuiGraphics graphics, long now, float reveal, int mouseX, int mouseY) {
        if (reveal < 4.0F / 255) {
            return;
        }
        int shift = detailShift;
        int x = DETAIL_X + shift;
        TerminalGraphicsRenderer.panel(graphics, x + 5, DETAIL_Y + 6, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND, reveal * 0.4F), 0);
        TerminalGraphicsRenderer.panel(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND, reveal),
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, reveal));
        renderStarlight(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                (now - openingStarted) / 1000.0F, reveal * 0.7F);
        if (!lockedNode) {
            graphics.drawString(font, font.plainSubstrByWidth(selectedNode.title().getString(), 298),
                    TEXT_LEFT + shift, DETAIL_TITLE_Y,
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, reveal), false);
        }
        int border = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, reveal);
        graphics.fill(TEXT_LEFT - 2 + shift, DETAIL_RULE_Y, TEXT_LEFT - 2 + DETAIL_RULE_WIDTH + shift,
                DETAIL_RULE_Y + 1, border);
        if (lockedNode) {
            renderLockTip(graphics, reveal, shift);
            return;
        }
        int textRight = selectedNode.image() == null ? TEXT_RIGHT : IMAGE_TEXT_RIGHT;
        clip(graphics, TEXT_LEFT + shift, TEXT_TOP, textRight + shift, TEXT_BOTTOM);
        int scroll = (int) detailScroll;
        for (TextRow row : storyRows) {
            int lineY = TEXT_TOP + row.y() - scroll;
            if (lineY >= TEXT_TOP - LINE_HEIGHT && lineY < TEXT_BOTTOM) {
                StoryLog log = row.log();
                float past = row.current() ? 0 : log.pastStarted < 0 ? 1
                        : TerminalGraphicsRenderer.ease((now - log.pastStarted) / (float) LINE_COLOR_DURATION);
                int color = TerminalGraphicsRenderer.mix(TerminalGraphicsRenderer.TEXT,
                        TerminalGraphicsRenderer.ACCENT, past);
                float enter = log.enteredAt < 0 ? 1
                        : TerminalGraphicsRenderer.ease((now - log.enteredAt) / (float) ANSWER_ENTER_DURATION);
                if (reveal * enter < 4.0F / 255) {
                    continue;
                }
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate((1 - enter) * ANSWER_ENTER_DISTANCE, 0, 0);
                    String speaker = row.text().substring(0, row.speakerGlyphs());
                    float speakerFade = log.speakerStarted < 0 ? 1
                            : TerminalGraphicsRenderer.ease((now - log.speakerStarted) / (float) SPEAKER_FADE_DURATION);
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
        graphics.disableScissor();
        if (selectedNode.image() != null) {
            float scale = Math.min(110.0F / selectedNode.imageWidth(), 138.0F / selectedNode.imageHeight());
            int imageWidth = Math.max(1, Math.round(selectedNode.imageWidth() * scale));
            int imageHeight = Math.max(1, Math.round(selectedNode.imageHeight() * scale));
            TerminalGraphicsRenderer.panel(graphics, 340 + shift, 102, 150, 157, TerminalGraphicsRenderer.slant(157),
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND, reveal), border);
            graphics.setColor(1, 1, 1, reveal * windowOpacity);
            try {
                graphics.blit(selectedNode.image(), 361 + shift + (110 - imageWidth) / 2, 110 + (138 - imageHeight) / 2,
                        imageWidth, imageHeight, 0, 0, selectedNode.imageWidth(), selectedNode.imageHeight(),
                        selectedNode.imageWidth(), selectedNode.imageHeight());
            } finally {
                graphics.setColor(1, 1, 1, windowOpacity);
            }
        }
        int maxScroll = maxDetailScroll();
        if (maxScroll > 0) {
            int textHeight = TEXT_BOTTOM - TEXT_TOP;
            int thumbHeight = Math.max(12, textHeight * textHeight / Math.max(1, storyTextHeight));
            int thumbY = TEXT_TOP + (int) ((textHeight - thumbHeight) * detailScroll / maxScroll);
            graphics.fill(textRight - 2 + shift, TEXT_TOP, textRight - 1 + shift, TEXT_BOTTOM, border);
            graphics.fill(textRight - 2 + shift, thumbY, textRight - 1 + shift, thumbY + thumbHeight,
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, reveal));
        }
        // 等待点击推进时，右下角闪烁下箭头
        if (!typing && waitingBranch == null && pendingChoice == null && pendingLine == null && hasPendingStory()) {
            float blink = 0.55F + 0.45F * Mth.sin(now / 260.0F);
            int markerX = textRight - 16 + shift;
            int markerY = TEXT_BOTTOM - 12 + Math.round(Mth.sin(now / 260.0F) * 1.5F);
            int color = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.ACCENT, reveal * blink);
            TerminalGraphicsRenderer.line(graphics, markerX, markerY, markerX + 4, markerY + 4, color);
            TerminalGraphicsRenderer.line(graphics, markerX + 8, markerY, markerX + 4, markerY + 4, color);
        }
        renderOptions(graphics, shift, reveal, mouseX, mouseY);
    }

    private void renderLockTip(GuiGraphics graphics, float reveal, int shift) {
        int scroll = (int) detailScroll;
        int border = TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, reveal);
        int textRight = TEXT_RIGHT;
        clip(graphics, TEXT_LEFT + shift, TEXT_TOP, textRight + shift, TEXT_BOTTOM);
        int y = 0;
        for (String row : lockRows) {
            int lineY = TEXT_TOP + y - scroll;
            if (lineY >= TEXT_TOP - LINE_HEIGHT && lineY < TEXT_BOTTOM) {
                graphics.drawString(font, row, TEXT_LEFT + shift, lineY,
                        TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.LOCKED_TEXT, reveal), false);
            }
            y += LINE_HEIGHT;
        }
        graphics.disableScissor();
        int maxScroll = maxDetailScroll();
        if (maxScroll > 0) {
            int textHeight = TEXT_BOTTOM - TEXT_TOP;
            int thumbHeight = Math.max(12, textHeight * textHeight / Math.max(1, storyTextHeight));
            int thumbY = TEXT_TOP + (int) ((textHeight - thumbHeight) * detailScroll / maxScroll);
            graphics.fill(textRight - 2 + shift, TEXT_TOP, textRight - 1 + shift, TEXT_BOTTOM, border);
            graphics.fill(textRight - 2 + shift, thumbY, textRight - 1 + shift, thumbY + thumbHeight,
                    TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.LOCKED_ACCENT, reveal));
        }
    }

    private void renderOptions(GuiGraphics graphics, int shift, float reveal, int mouseX, int mouseY) {
        optionHits.clear();
        if (waitingBranch == null || typing || detailClosingStarted >= 0) {
            return;
        }
        List<Option> options = availableOptions(waitingBranch);
        if (options.isEmpty()) {
            return;
        }
        long now = Util.getMillis();
        int right = DETAIL_X + DETAIL_WIDTH - 22;
        int bottom = DETAIL_Y + DETAIL_HEIGHT - 14;
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            float progress = choiceStarted < 0 ? 1 : Mth.clamp(
                    (now - choiceStarted - Math.min(i, 5) * CHOICE_REVEAL_STAGGER)
                            / (float) CHOICE_REVEAL_DURATION, 0, 1);
            if (progress <= 0) {
                continue;
            }
            float appear = TerminalGraphicsRenderer.ease(progress);
            String text = TerminalStoryText.plainOf(option.choice().text());
            int choiceWidth = Mth.clamp(font.width(text) + 26, 56, CHOICE_MAX_WIDTH);
            int x = right - choiceWidth + Math.round((1 - appear) * 12) + shift;
            int y = bottom - CHOICE_HEIGHT - i * (CHOICE_HEIGHT + CHOICE_GAP);
            boolean hovered = mouseX >= x && mouseX < x + choiceWidth && mouseY >= y && mouseY < y + CHOICE_HEIGHT;
            TerminalChoiceRenderer.render(graphics, font, x, y, choiceWidth, CHOICE_HEIGHT,
                    font.plainSubstrByWidth(text, choiceWidth - 20), hovered, reveal, progress, now, option.index());
            optionHits.add(new OptionHit(x, y, x + choiceWidth, y + CHOICE_HEIGHT, option.choice()));
        }
    }

    private void showNode(TerminalStoryTree.Node node) {
        selectedNode = node;
        lockedNode = !isNodeUnlocked(node);
        detailStarted = Util.getMillis();
        detailClosingStarted = -1;
        detailOpacity = 0;
        detailShift = -18;
        detailScroll = 0;
        storyLog.clear();
        storyRows.clear();
        storyFrames.clear();
        consumedChoices = new IdentityHashMap<>();
        optionHits.clear();
        waitingBranch = null;
        waitingBranchRef = null;
        pendingChoice = null;
        pendingLine = null;
        typing = false;
        autoScroll = true;
        choiceStarted = -1;
        finished = false;
        if (lockedNode) {
            resetLockRows(node);
        } else {
            restoreProgress(node);
        }
        panning = false;
        setFocused(backButton);
        updateVisibility();
        if (minecraft != null) {
            minecraft.getNarrator().sayNow(lockedNode && !lockRows.isEmpty() ? Component.literal(lockRows.get(0))
                    : node.title());
        }
    }

    private void resetLockRows(TerminalStoryTree.Node node) {
        lockRows.clear();
        int width = TEXT_RIGHT - TEXT_LEFT - 10;
        for (String tip : unlockTip(node)) {
            wrap(font, tip, width, lockRows);
        }
        typing = false;
        typedGlyphs = 0;
        storyTextHeight = lockRows.size() * LINE_HEIGHT;
        detailScroll = 0;
        autoScroll = true;
    }

    private void restoreProgress(TerminalStoryTree.Node node) {
        storyTags.addAll(TerminalStoryProgressCache.get().tags);
        TerminalStoryProgress.NodeProgress saved = TerminalStoryProgressCache.node(node.id());
        if (saved == null) {
            storyFrames.push(new StoryFrame(node.lines(), List.of(), null, false));
            revealNext();
            return;
        }
        finished = saved.finished;
        for (TerminalStoryProgress.LogRef ref : saved.log) {
            if (ref.choice >= 0) {
                TerminalStoryTree.Choice answer = resolveChoice(node.lines(), ref.path, ref.line, ref.choice);
                if (answer != null && !answer.silence()) {
                    storyLog.add(new StoryLog(new TerminalStoryTree.Line(Component.literal(playerName()),
                            answer.content(), 0, List.of(), List.of()), ref.path, ref.line, ref.choice));
                }
                continue;
            }
            TerminalStoryTree.Line line = resolveLine(node.lines(), ref.path, ref.line);
            if (line != null) {
                storyLog.add(new StoryLog(line, ref.path, ref.line));
            }
        }
        boolean framesValid = !saved.frames.isEmpty();
        for (TerminalStoryProgress.FrameState savedFrame : saved.frames) {
            StoryFrame frame = restoreFrame(node, savedFrame);
            if (frame == null) {
                framesValid = false;
                break;
            }
            storyFrames.push(frame);
        }
        if (!framesValid || storyFrames.isEmpty() || !storyFrames.peekLast().path.isEmpty()) {
            // 根帧缺失或已经播完：按读完处理，只展示日志
            framesValid = false;
            storyFrames.clear();
            StoryFrame done = new StoryFrame(node.lines(), List.of(), null, false);
            done.index = node.lines().size();
            storyFrames.push(done);
        }
        for (TerminalStoryProgress.ConsumedState consumed : saved.consumed) {
            TerminalStoryTree.Line branch = resolveLine(node.lines(), consumed.path, consumed.line);
            if (branch != null) {
                consumedChoices.put(branch, new HashSet<>(consumed.choices));
            }
        }
        StoryLog current = storyLog.isEmpty() ? null : storyLog.get(storyLog.size() - 1);
        int length = current == null ? 0 : current.length();
        int prefix = current == null ? 0 : current.prefixLength();
        typedGlyphs = Mth.clamp(saved.typed, prefix, length);
        typing = saved.typing && typedGlyphs < length;
        if (saved.pendingChoice != null && framesValid) {
            TerminalStoryProgress.LogRef ref = saved.pendingChoice;
            TerminalStoryTree.Choice choice = resolveChoice(node.lines(), ref.path, ref.line, ref.choice);
            TerminalStoryTree.Line branch = resolveLine(node.lines(), ref.path, ref.line);
            StoryLog branchRef = branch == null ? null : findLog(branch);
            if (choice != null && branchRef != null) {
                pendingChoice = new PendingChoice(choice, branchRef, ref.choice,
                        Util.getMillis() + Mth.clamp(saved.delay, 0, (int) ANSWER_ENTER_DURATION));
                typing = false;
                typedGlyphs = length;
            }
        }
        if (saved.pendingLine != null && framesValid) {
            TerminalStoryProgress.LogRef ref = saved.pendingLine;
            TerminalStoryTree.Line line = resolveLine(node.lines(), ref.path, ref.line);
            if (line != null) {
                pendingLine = new PendingLine(new StoryLog(line, ref.path, ref.line),
                        Util.getMillis() + Mth.clamp(saved.delay, 0, 60000));
                typing = false;
                typedGlyphs = length;
            }
        }
        if (typing) {
            // 从停下的字符继续，标记停顿按同一条时间轴重放
            typingStarted = Util.getMillis() - (long) (current == null ? 0 : current.durationAt(typedGlyphs));
        } else if (pendingChoice == null && pendingLine == null) {
            restoreWaitingBranch(node, saved);
        }
        if (!finished && !typing && waitingBranch == null && pendingChoice == null && pendingLine == null
                && !hasPendingStory()) {
            finished = true;
            persistStory();
        }
    }

    private void restoreWaitingBranch(TerminalStoryTree.Node node, TerminalStoryProgress.NodeProgress saved) {
        if (saved.branch == null) {
            return;
        }
        TerminalStoryTree.Line branch = resolveLine(node.lines(), saved.branch.path, saved.branch.line);
        if (branch == null || availableOptions(branch).isEmpty()) {
            return;
        }
        StoryLog ref = findLog(branch);
        if (ref == null) {
            return;
        }
        waitingBranch = branch;
        waitingBranchRef = ref;
        choiceStarted = Util.getMillis();
    }

    private StoryFrame restoreFrame(TerminalStoryTree.Node node, TerminalStoryProgress.FrameState state) {
        List<TerminalStoryTree.Line> lines = linesAt(node.lines(), state.path);
        if (lines == null) {
            return null;
        }
        StoryFrame frame;
        if (state.path.isEmpty()) {
            frame = new StoryFrame(node.lines(), List.of(), null, false);
        } else {
            int last = state.path.size() - 1;
            List<TerminalStoryTree.Line> parentLines = linesAt(node.lines(), state.path.subList(0, last - 1));
            if (parentLines == null) {
                return null;
            }
            int lineIndex = state.path.get(last - 1);
            int choiceIndex = state.path.get(last);
            if (lineIndex < 0 || lineIndex >= parentLines.size()) {
                return null;
            }
            TerminalStoryTree.Line branch = parentLines.get(lineIndex);
            if (choiceIndex < 0 || choiceIndex >= branch.choices().size()) {
                return null;
            }
            TerminalStoryTree.Choice choice = branch.choices().get(choiceIndex);
            frame = new StoryFrame(choice.lines(), state.path, branch, !choice.mainLine());
        }
        frame.index = Mth.clamp(state.index, 0, frame.lines.size());
        return frame;
    }

    private static List<TerminalStoryTree.Line> linesAt(List<TerminalStoryTree.Line> root, List<Integer> path) {
        List<TerminalStoryTree.Line> lines = root;
        for (int i = 0; i + 1 < path.size(); i += 2) {
            int lineIndex = path.get(i);
            if (lineIndex < 0 || lineIndex >= lines.size()) {
                return null;
            }
            List<TerminalStoryTree.Choice> choices = lines.get(lineIndex).choices();
            int choiceIndex = path.get(i + 1);
            if (choiceIndex < 0 || choiceIndex >= choices.size()) {
                return null;
            }
            lines = choices.get(choiceIndex).lines();
        }
        return lines;
    }

    private static TerminalStoryTree.Line resolveLine(List<TerminalStoryTree.Line> root, List<Integer> path, int line) {
        List<TerminalStoryTree.Line> lines = linesAt(root, path);
        return lines == null || line < 0 || line >= lines.size() ? null : lines.get(line);
    }

    private static TerminalStoryTree.Choice resolveChoice(List<TerminalStoryTree.Line> root, List<Integer> path,
            int line, int choice) {
        TerminalStoryTree.Line branch = resolveLine(root, path, line);
        return branch == null || choice < 0 || choice >= branch.choices().size() ? null
                : branch.choices().get(choice);
    }

    /**
     * 把当前演绎状态写入客户端缓存并上报服务端存档。
     */
    private void persistStory() {
        // 未解锁的剧情块不写进度，避免覆盖既有记录
        if (selectedNode == null || lockedNode) {
            return;
        }
        TerminalStoryProgress.NodeProgress state = new TerminalStoryProgress.NodeProgress();
        for (StoryLog entry : storyLog) {
            TerminalStoryProgress.LogRef ref = reference(entry);
            ref.choice = entry.answer();
            state.log.add(ref);
        }
        var iterator = storyFrames.descendingIterator();
        while (iterator.hasNext()) {
            StoryFrame frame = iterator.next();
            TerminalStoryProgress.FrameState frameState = new TerminalStoryProgress.FrameState();
            frameState.path = frame.path;
            frameState.index = frame.index;
            state.frames.add(frameState);
        }
        for (Map.Entry<TerminalStoryTree.Line, Set<Integer>> entry : consumedChoices.entrySet()) {
            StoryLog ref = findLog(entry.getKey());
            if (ref == null) {
                continue;
            }
            TerminalStoryProgress.ConsumedState consumed = new TerminalStoryProgress.ConsumedState();
            consumed.path = ref.path();
            consumed.line = ref.index();
            consumed.choices = new ArrayList<>(entry.getValue());
            state.consumed.add(consumed);
        }
        state.typing = typing && !storyLog.isEmpty();
        state.typed = storyLog.isEmpty() ? 0 : Math.min(typedGlyphs, storyLog.get(storyLog.size() - 1).length());
        state.finished = finished;
        state.branch = waitingBranchRef == null ? null : reference(waitingBranchRef);
        if (pendingChoice != null) {
            state.pendingChoice = reference(pendingChoice.branchRef());
            state.pendingChoice.choice = pendingChoice.index();
            state.delay = (int) Mth.clamp(pendingChoice.continueAt() - Util.getMillis(),
                    0L, ANSWER_ENTER_DURATION);
        } else if (pendingLine != null) {
            state.pendingLine = reference(pendingLine.entry());
            state.delay = (int) Mth.clamp(pendingLine.revealAt() - Util.getMillis(), 0L, 60000L);
        }
        TerminalStoryProgressCache.record(selectedNode.id(), state, storyTags);
    }

    private static TerminalStoryProgress.LogRef reference(StoryLog entry) {
        TerminalStoryProgress.LogRef ref = new TerminalStoryProgress.LogRef();
        ref.path = entry.path();
        ref.line = entry.index();
        return ref;
    }
    private StoryLog findLog(TerminalStoryTree.Line line) {
        for (StoryLog entry : storyLog) {
            if (entry.line() == line) {
                return entry;
            }
        }
        return null;
    }

    private void revealNext() {
        waitingBranch = null;
        waitingBranchRef = null;
        typing = false;
        while (!storyFrames.isEmpty()) {
            StoryFrame frame = storyFrames.peek();
            if (frame.index < frame.lines.size()) {
                TerminalStoryTree.Line line = frame.lines.get(frame.index++);
                if (!storyTags.containsAll(line.requires())) {
                    continue;
                }
                StoryLog entry = new StoryLog(line, frame.path, frame.index - 1);
                if (line.delay() > 0) {
                    pendingLine = new PendingLine(entry, Util.getMillis() + line.delay());
                    persistStory();
                } else {
                    startLine(entry);
                }
                return;
            }
            storyFrames.pop();
            if (frame.returnToBranch && frame.branch != null && !availableOptions(frame.branch).isEmpty()) {
                waitingBranch = frame.branch;
                waitingBranchRef = findLog(frame.branch);
                choiceStarted = Util.getMillis();
                persistStory();
                return;
            }
        }
        finished = true;
        persistStory();
    }

    private void startLine(StoryLog entry) {
        long now = Util.getMillis();
        pendingLine = null;
        entry.speakerStarted = now;
        if (!storyLog.isEmpty()) {
            storyLog.get(storyLog.size() - 1).pastStarted = now;
        }
        storyLog.add(entry);
        typing = true;
        typedGlyphs = entry.prefixLength();
        typingStarted = now;
        autoScroll = true;
        if (minecraft != null) {
            minecraft.getNarrator().sayNow(entry.line().display());
        }
        persistStory();
    }

    private void advanceStory() {
        if (lockedNode || pendingChoice != null || pendingLine != null) {
            return;
        }
        if (typing) {
            finishTyping();
            return;
        }
        if (waitingBranch == null) {
            revealNext();
        }
    }

    private void finishTyping() {
        typing = false;
        typedGlyphs = storyLog.isEmpty() ? 0 : storyLog.get(storyLog.size() - 1).length();
        updateWaitingBranch();
        checkFinished();
        persistStory();
    }

    private void checkFinished() {
        if (finished || typing || waitingBranch != null || pendingChoice != null || pendingLine != null) {
            return;
        }
        if (!hasPendingStory()) {
            finished = true;
        }
    }

    private void updateWaitingBranch() {
        if (typing || waitingBranch != null || pendingChoice != null || pendingLine != null || storyLog.isEmpty()) {
            return;
        }
        StoryLog entry = storyLog.get(storyLog.size() - 1);
        if (!availableOptions(entry.line()).isEmpty()) {
            waitingBranch = entry.line();
            waitingBranchRef = entry;
            choiceStarted = Util.getMillis();
        }
    }

    private void chooseOption(TerminalStoryTree.Choice choice) {
        TerminalStoryTree.Line branch = waitingBranch;
        StoryLog branchRef = waitingBranchRef;
        if (branch == null || branchRef == null) {
            return;
        }
        Option selected = null;
        for (Option option : availableOptions(branch)) {
            if (option.choice() == choice) {
                selected = option;
                break;
            }
        }
        if (selected == null) {
            return;
        }
        consumedChoices.computeIfAbsent(branch, key -> new HashSet<>()).add(selected.index());
        storyTags.addAll(choice.tags());
        waitingBranch = null;
        waitingBranchRef = null;
        choiceStarted = -1;
        optionHits.clear();
        appendAnswer(branchRef, selected);
        pendingChoice = new PendingChoice(choice, branchRef, selected.index(), Util.getMillis()
                + (choice.silence() ? 0 : ANSWER_ENTER_DURATION));
        persistStory();
    }

    private void continueChoice() {
        PendingChoice pending = pendingChoice;
        pendingChoice = null;
        TerminalStoryTree.Choice choice = pending.choice();
        StoryLog branchRef = pending.branchRef();
        TerminalStoryTree.Line branch = branchRef.line();
        if (!choice.lines().isEmpty()) {
            List<Integer> path = new ArrayList<>(branchRef.path());
            path.add(branchRef.index());
            path.add(pending.index());
            storyFrames.push(new StoryFrame(choice.lines(), path, branch, !choice.mainLine()));
            revealNext();
            return;
        }
        if (!choice.mainLine() && !availableOptions(branch).isEmpty()) {
            waitingBranch = branch;
            waitingBranchRef = branchRef;
            choiceStarted = Util.getMillis();
        } else {
            revealNext();
            return;
        }
        persistStory();
    }

    private void appendAnswer(StoryLog branchRef, Option option) {
        if (option.choice().silence()) {
            return;
        }
        TerminalStoryTree.Line answer = new TerminalStoryTree.Line(Component.literal(playerName()),
                option.choice().content(), 0, List.of(), List.of());
        long now = Util.getMillis();
        if (!storyLog.isEmpty()) {
            storyLog.get(storyLog.size() - 1).pastStarted = now;
        }
        StoryLog entry = new StoryLog(answer, branchRef.path(), branchRef.index(), option.index());
        entry.enteredAt = now;
        storyLog.add(entry);
        typedGlyphs = entry.length();
        autoScroll = true;
    }

    private String playerName() {
        return TerminalStoryText.playerName();
    }

    void onUnlocksSynced() {
        if (selectedNode != null && lockedNode && isNodeUnlocked(selectedNode)) {
            showUnlockedNode();
            return;
        }
        updateVisibility();
    }

    private void showUnlockedNode() {
        lockedNode = false;
        lockRows.clear();
        storyLog.clear();
        storyRows.clear();
        storyFrames.clear();
        consumedChoices = new IdentityHashMap<>();
        optionHits.clear();
        waitingBranch = null;
        waitingBranchRef = null;
        pendingChoice = null;
        pendingLine = null;
        typing = false;
        finished = false;
        detailScroll = 0;
        autoScroll = true;
        choiceStarted = -1;
        restoreProgress(selectedNode);
        // 跳过浮窗入场动画，内容立即完整显示
        detailStarted = Util.getMillis() - DETAIL_ENTER_DURATION;
        detailOpacity = 1;
        detailShift = 0;
        updateVisibility();
    }

    /**
     * 连线与节点入场都相对本块自己的 revealAt 起点播放。
     */
    private float connectionProgress(long elapsed, TerminalStoryTree.Node node) {
        if (!isNodeVisible(node)) {
            return 0;
        }
        Long start = revealAt.get(node.id());
        return start == null ? 0 : TerminalAnimation.connectionProgress(elapsed, start);
    }

    private float nodeReveal(long elapsed, TerminalStoryTree.Node node) {
        if (!isNodeVisible(node)) {
            return 0;
        }
        Long start = revealAt.get(node.id());
        return start == null ? 0 : TerminalAnimation.nodeProgress(elapsed, start);
    }

    /**
     * 父块读完后登记子块的入场起点：根节点沿用打开时的逐层时序，
     * 其余节点在回到剧情树后才开始连线动画。
     */
    private void updateReveals(long elapsed) {
        boolean showingDetail = selectedNode != null && detailClosingStarted < 0;
        for (TerminalStoryTree.Node node : story.nodes()) {
            if (revealAt.containsKey(node.id()) || !isNodeVisible(node)) {
                continue;
            }
            boolean root = node.parent() == null;
            if (!root && (showingDetail || !TerminalStoryProgressCache.isSynced())) {
                continue;
            }
            revealAt.put(node.id(), root ? animation.connectionStart(node)
                    : elapsed + POST_FINISH_DELAY + Math.min(animation.layer(node), 3)
                            * (TerminalAnimation.CONNECTION_DURATION + TerminalAnimation.NODE_DURATION));
        }
    }

    /**
     * 父块必须已解锁且被完整读完，子块才会出现；父块不存在时按根块处理。
     */
    private boolean isParentRead(TerminalStoryTree.Node node) {
        if (node.parent() == null) {
            return true;
        }
        TerminalStoryTree.Node parent = findNode(node.parent());
        if (parent == null) {
            return true;
        }
        TerminalStoryProgress.NodeProgress progress = TerminalStoryProgressCache.node(parent.id());
        // 旧存档没有 finished 标记时，只剩空帧即视为已读完
        if (progress == null || !(progress.finished || progress.frames.isEmpty())) {
            return false;
        }
        return isNodeUnlocked(parent);
    }

    private TerminalStoryTree.Node findNode(String id) {
        for (TerminalStoryTree.Node node : story.nodes()) {
            if (node.id().equals(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 剧情块是否已经出现：默认要求父块已解锁并读完，requires 不齐时仍出现但保持锁定。
     */
    private boolean isNodeVisible(TerminalStoryTree.Node node) {
        return isParentRead(node);
    }

    /**
     * 剧情块是否已解锁。
     */
    private boolean isNodeUnlocked(TerminalStoryTree.Node node) {
        if (!storyTags.containsAll(node.requires())) {
            return false;
        }
        for (Unlock unlock : node.unlocks()) {
            if (!TerminalStoryProgressCache.isUnlocked(
                    TerminalUnlockCondition.key(unlock.id(), unlock.tag(), unlock.count()))) {
                return false;
            }
        }
        return true;
    }

    private List<String> unlockTip(TerminalStoryTree.Node node) {
        if (node.tip() != null && !node.tip().isBlank()) {
            return List.of(node.tip());
        }
        List<String> tips = new ArrayList<>();
        if (!storyTags.containsAll(node.requires())) {
            tips.add(Component.translatable("gui.ae2pr.proto_terminal.locked.requires").getString());
        }
        for (Unlock unlock : node.unlocks()) {
            if (TerminalStoryProgressCache.isUnlocked(
                    TerminalUnlockCondition.key(unlock.id(), unlock.tag(), unlock.count()))) {
                continue;
            }
            tips.add(Component.translatable("gui.ae2pr.proto_terminal.locked.condition",
                    unlockName(unlock), unlock.count()).getString());
        }
        return List.copyOf(tips);
    }

    private static Component unlockName(Unlock unlock) {
        if (unlock.tag()) {
            return Component.translatable("gui.ae2pr.proto_terminal.locked.tag", unlock.id().toString());
        }
        Item item = ForgeRegistries.ITEMS.getValue(unlock.id());
        return item == null ? Component.literal(unlock.id().toString()) : new ItemStack(item).getHoverName();
    }

    private List<Option> availableOptions(TerminalStoryTree.Line line) {
        List<Option> options = new ArrayList<>();
        Set<Integer> used = consumedChoices.get(line);
        for (int i = 0; i < line.choices().size(); i++) {
            if ((used == null || !used.contains(i)) && storyTags.containsAll(line.choices().get(i).requires())) {
                options.add(new Option(i, line.choices().get(i)));
            }
        }
        return options;
    }

    private boolean hasPendingStory() {
        for (StoryFrame frame : storyFrames) {
            for (int i = frame.index; i < frame.lines.size(); i++) {
                if (storyTags.containsAll(frame.lines.get(i).requires())) {
                    return true;
                }
            }
            if (frame.returnToBranch && frame.branch != null && !availableOptions(frame.branch).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void updateStory(long now) {
        if (selectedNode == null || lockedNode) {
            return;
        }
        if (pendingChoice != null && now >= pendingChoice.continueAt()
                && detailClosingStarted < 0 && terminalClosingStarted < 0) {
            continueChoice();
        }
        if (pendingLine != null && now >= pendingLine.revealAt()
                && detailClosingStarted < 0 && terminalClosingStarted < 0) {
            startLine(pendingLine.entry());
        }
        StoryLog current = storyLog.isEmpty() ? null : storyLog.get(storyLog.size() - 1);
        if (typing && current != null) {
            typedGlyphs = Math.min(current.length(),
                    Math.max(typedGlyphs, current.glyphsAt(now - typingStarted)));
            if (typedGlyphs >= current.length()) {
                typing = false;
                updateWaitingBranch();
                checkFinished();
                persistStory();
            }
        }
        rebuildStoryRows();
    }

    private void rebuildStoryRows() {
        storyRows.clear();
        int width = (selectedNode != null && selectedNode.image() != null ? IMAGE_TEXT_RIGHT : TEXT_RIGHT)
                - TEXT_LEFT - 10;
        int last = storyLog.size() - 1;
        int y = 0;
        for (int i = 0; i < storyLog.size(); i++) {
            if (i > 0) {
                y += 6;
            }
            StoryLog log = storyLog.get(i);
            int visible = i == last && typing ? typedGlyphs : log.length();
            String text = log.plain();
            int cursor = 0;
            for (String wrapped : log.wrapped(font, width)) {
                int start = text.indexOf(wrapped, cursor);
                cursor = start + wrapped.length();
                int remaining = visible - start;
                if (remaining <= 0) {
                    break;
                }
                String shown = wrapped.length() <= remaining ? wrapped : wrapped.substring(0, remaining);
                int speakerGlyphs = Mth.clamp(log.prefixLength() - start, 0, shown.length());
                storyRows.add(new TextRow(shown, speakerGlyphs, y, log, i == last));
                y += LINE_HEIGHT;
            }
        }
        storyTextHeight = y;
        int maxScroll = maxDetailScroll();
        detailScroll = autoScroll ? maxScroll : Mth.clamp(detailScroll, 0, maxScroll);
    }

    private void showTree() {
        selectedNode = null;
        lockedNode = false;
        lockRows.clear();
        detailClosingStarted = -1;
        detailOpacity = 0;
        storyLog.clear();
        storyRows.clear();
        storyFrames.clear();
        optionHits.clear();
        waitingBranch = null;
        pendingChoice = null;
        pendingLine = null;
        typing = false;
        setFocused(null);
        updateVisibility();
    }

    private void closeDetail() {
        if (selectedNode == null || terminalClosingStarted >= 0) {
            return;
        }
        if (detailClosingStarted >= 0) {
            showTree();
            return;
        }
        // 未解锁块没有可写的进度
        if (!lockedNode) {
            persistStory();
        }
        detailClosingStarted = Util.getMillis();
        detailExitOpacity = detailOpacity;
        detailExitShift = detailShift;
        panning = false;
        setFocused(null);
        if (detailExitOpacity <= 0) {
            showTree();
            return;
        }
        updateVisibility();
    }

    private void closeTerminal() {
        if (terminalClosingStarted < 0) {
            terminalClosingStarted = Util.getMillis();
            if (openingStarted < 0) {
                openingStarted = terminalClosingStarted;
            }
            panning = false;
            updateVisibility();
            minecraft.setScreen(null);
            if (minecraft.screen == this) {
                terminalClosingStarted = -1;
                windowOpacity = 1;
                updateVisibility();
            } else if (minecraft.screen == null) {
                TerminalFadeOutRenderer.begin(this);
            }
        }
    }

    private void updateTransitions(long now) {
        windowOpacity = terminalClosingStarted < 0 ? 1
                : TerminalAnimation.fadeOut(now - terminalClosingStarted, TERMINAL_EXIT_DURATION);
        if (selectedNode == null) {
            return;
        }
        if (detailClosingStarted >= 0) {
            float opacity = TerminalAnimation.fadeOut(now - detailClosingStarted, DETAIL_EXIT_DURATION);
            float progress = 1 - opacity;
            detailOpacity = detailExitOpacity * opacity;
            detailShift = Math.max(-28, detailExitShift - Math.round(18 * progress));
            if (now - detailClosingStarted >= DETAIL_EXIT_DURATION) {
                showTree();
            }
        } else {
            detailOpacity = TerminalGraphicsRenderer.ease((now - detailStarted) / (float) DETAIL_ENTER_DURATION);
            detailShift = Math.round(-18 * (1 - detailOpacity));
        }
    }

    private void updateVisibility() {
        long now = Util.getMillis();
        long elapsed = openingStarted < 0 ? 0 : now - openingStarted;
        boolean interactive = isInteractive();
        boolean showingDetail = selectedNode != null && detailClosingStarted < 0;
        for (int i = 0; i < nodeButtons.size(); i++) {
            TerminalStoryTree.Node node = story.nodes().get(i);
            TerminalButton button = nodeButtons.get(i);
            float progress = nodeReveal(elapsed, node);
            boolean locked = progress > 0 && !isNodeUnlocked(node);
            button.setLocked(locked);
            button.visible = progress > 0;
            button.active = button.visible && !showingDetail && interactive;
        }
        backButton.visible = selectedNode != null;
        backButton.active = selectedNode != null && interactive;
        backButton.setX(BACK_X + detailShift);
        backButton.setOpacity(detailOpacity);
        float headerOpacity = animation.headerOpacity(elapsed);
        closeButton.visible = headerOpacity > 0;
        closeButton.active = closeButton.visible && interactive;
    }

    private void updateNodePositions(long elapsed) {
        // 剧情块与连线统一吸附到物理像素网格，保持描边亚像素相位恒定
        float deviceScale = deviceScale();
        for (int i = 0; i < nodeButtons.size(); i++) {
            TerminalStoryTree.Node node = story.nodes().get(i);
            TerminalButton button = nodeButtons.get(i);
            float progress = nodeReveal(elapsed, node);
            float x = snap(TREE_LEFT + panX + (node.x() - NODE_ENTER_DISTANCE * (1 - progress)) * contentZoom,
                    deviceScale);
            float y = snap(TREE_TOP + panY + node.y() * contentZoom, deviceScale);
            button.setScale(contentZoom);
            button.setX(Math.round(x));
            button.setY(Math.round(y));
            // 用亚像素偏移补回取整与网格吸附的残差，节点与连线共用同一套坐标
            button.setOffset(x - button.getX(), y - button.getY());
            button.setOpacity(progress);
        }
    }

    private float deviceScale() {
        return canvasScale * (float) minecraft.getWindow().getGuiScale();
    }

    private static float snap(float value, float deviceScale) {
        return Math.round(value * deviceScale) / deviceScale;
    }

    private int maxDetailScroll() {
        return Math.max(0, storyTextHeight - (TEXT_BOTTOM - TEXT_TOP));
    }

    private void clip(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.enableScissor((int) (canvasLeft + left * canvasScale), (int) (canvasTop + top * canvasScale),
                (int) Math.ceil(canvasLeft + right * canvasScale), (int) Math.ceil(canvasTop + bottom * canvasScale));
    }

    private static Component label(String suffix) {
        return Component.translatable("gui.ae2pr.proto_terminal." + suffix);
    }

    private double localX(double x) {
        return (x - canvasLeft) / canvasScale;
    }

    private double localY(double y) {
        return (y - canvasTop) / canvasScale;
    }

    private boolean insideTree(double x, double y) {
        return TerminalGraphicsRenderer.contains(x, y, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = localX(mouseX);
        double y = localY(mouseY);
        if (!isInteractive()) {
            return true;
        }
        if (closeButton.mouseClicked(x, y, button)) {
            return true;
        }
        if (selectedNode != null) {
            if (backButton.mouseClicked(x, y, button)) {
                return true;
            }
            if (detailClosingStarted < 0) {
                if (button == 0) {
                    for (OptionHit hit : optionHits) {
                        if (hit.contains(x, y)) {
                            chooseOption(hit.choice());
                            return true;
                        }
                    }
                    if (TerminalGraphicsRenderer.contains(x, y, DETAIL_X + detailShift, DETAIL_Y,
                            DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT)) {
                        advanceStory();
                    }
                }
                return true;
            }
        }
        if (TerminalGraphicsRenderer.contains(x, y, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT)) {
            return true;
        }
        if (!insideTree(x, y)) {
            return backButton.mouseClicked(x, y, button);
        }
        if (super.mouseClicked(x, y, button)) {
            if (selectedNode != null) {
                setFocused(backButton);
            }
            return true;
        }
        if (button == 0 && insideTree(x, y)) {
            panning = true;
            setFocused(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning && button == 0) {
            panX = Mth.clamp(panX + (float) (dragX / canvasScale), -4352, 4352);
            panY = Mth.clamp(panY + (float) (dragY / canvasScale), -4352, 4352);
            updateNodePositions(Util.getMillis() - openingStarted);
            return true;
        }
        return super.mouseDragged(localX(mouseX), localY(mouseY), button, dragX / canvasScale, dragY / canvasScale);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        panning = false;
        return super.mouseReleased(localX(mouseX), localY(mouseY), button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(localX(mouseX), localY(mouseY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (selectedNode != null && detailClosingStarted < 0 && terminalClosingStarted < 0
                && insideTree(localX(mouseX), localY(mouseY))) {
            int maxScroll = maxDetailScroll();
            detailScroll = Mth.clamp(detailScroll - delta * 26, 0, maxScroll);
            autoScroll = detailScroll >= maxScroll;
            return true;
        }
        if (selectedNode == null && hasControlDown() && isInteractive()) {
            zoomContent(mouseX, mouseY, delta);
            return true;
        }
        return super.mouseScrolled(localX(mouseX), localY(mouseY), delta);
    }

    @Override
    public void onClose() {
        if (selectedNode != null && detailClosingStarted < 0) {
            closeDetail();
        } else {
            closeTerminal();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        persistStory();
        TerminalStoryProgressCache.stopWatching();
        super.removed();
    }

    /**
     * 终端标题、剧情块与连线的入场时序。
     */
    private static final class TerminalAnimation {

        private static final long OPEN_DURATION = 900;
        private static final long CONTENT_START = 250;
        private static final long HEADER_START = OPEN_DURATION - 150;
        private static final long HEADER_DURATION = 160;
        private static final long NODE_DURATION = 240;
        private static final long CONNECTION_DURATION = 180;
        private final Map<String, Entrance> entrances = new HashMap<>();

        private record Entrance(int layer, List<TerminalStoryTree.Node> sources) {
        }

        TerminalAnimation(List<TerminalStoryTree.Node> nodes) {
            Map<String, TerminalStoryTree.Node> byId = new HashMap<>();
            Map<String, List<TerminalStoryTree.Node>> neighbors = new HashMap<>();
            for (TerminalStoryTree.Node node : nodes) {
                byId.put(node.id(), node);
                neighbors.put(node.id(), new ArrayList<>());
            }
            for (TerminalStoryTree.Node node : nodes) {
                TerminalStoryTree.Node parent = byId.get(node.parent());
                if (parent != null && parent != node && !neighbors.get(node.id()).contains(parent)) {
                    neighbors.get(node.id()).add(parent);
                    neighbors.get(parent.id()).add(node);
                }
            }
            List<TerminalStoryTree.Node> ordered = new ArrayList<>(nodes);
            ordered.sort(Comparator.comparingInt(TerminalStoryTree.Node::x)
                    .thenComparingInt(TerminalStoryTree.Node::position)
                    .thenComparingInt(TerminalStoryTree.Node::y)
                    .thenComparing(TerminalStoryTree.Node::id));
            int column = 0;
            int columnLayer = 0;
            int lastLayer = -1;
            for (TerminalStoryTree.Node node : ordered) {
                if (entrances.isEmpty() || column != node.x()) {
                    column = node.x();
                    columnLayer = lastLayer + 1;
                }
                int layer = columnLayer;
                List<TerminalStoryTree.Node> sources = new ArrayList<>();
                for (TerminalStoryTree.Node neighbor : neighbors.get(node.id())) {
                    Entrance previous = entrances.get(neighbor.id());
                    if (previous != null) {
                        layer = Math.max(layer, previous.layer() + 1);
                        sources.add(neighbor);
                    }
                }
                entrances.put(node.id(), new Entrance(layer, List.copyOf(sources)));
                lastLayer = Math.max(lastLayer, layer);
            }
        }

        List<TerminalStoryTree.Node> sources(TerminalStoryTree.Node node) {
            return entrances.get(node.id()).sources();
        }

        float opening(long elapsed) {
            return Mth.clamp(elapsed / (float) OPEN_DURATION, 0, 1);
        }

        float headerOpacity(long elapsed) {
            return TerminalGraphicsRenderer.ease((elapsed - HEADER_START) / (float) HEADER_DURATION);
        }

        long connectionStart(TerminalStoryTree.Node node) {
            return CONTENT_START + layer(node) * (CONNECTION_DURATION + NODE_DURATION);
        }

        int layer(TerminalStoryTree.Node node) {
            return entrances.get(node.id()).layer();
        }

        static float connectionProgress(long elapsed, long start) {
            return Mth.clamp((elapsed - start) / (float) CONNECTION_DURATION, 0, 1);
        }

        static float nodeProgress(long elapsed, long start) {
            return TerminalGraphicsRenderer.ease(
                    (elapsed - start - CONNECTION_DURATION) / (float) NODE_DURATION);
        }

        static float fadeOut(long elapsed, long duration) {
            return 1 - TerminalGraphicsRenderer.ease(elapsed / (float) Math.max(1, duration));
        }
    }

    /**
     * 按帧缓冲像素生成平行四边形裁剪带。
     */
    private static final class TerminalParallelogramClip {

        private record Band(int left, int top, int right, int bottom) {
        }

        private TerminalParallelogramClip() {
        }

        static List<Band> bands(double x, double y, double width, double height, double slant) {
            if (width <= slant || height <= 0) {
                return List.of();
            }
            List<Band> bands = new ArrayList<>();
            int top = (int) Math.ceil(y - 0.5);
            int bottom = (int) Math.ceil(y + height - 0.5);
            for (int row = top; row < bottom; row++) {
                double edge = x + slant * (1 - (row + 0.5 - y) / height);
                int left = (int) Math.ceil(edge - 0.5);
                int right = (int) Math.ceil(edge + width - slant - 0.5);
                if (right <= left) {
                    continue;
                }
                if (!bands.isEmpty()) {
                    Band previous = bands.get(bands.size() - 1);
                    if (previous.left() == left && previous.right() == right && previous.bottom() == row) {
                        bands.set(bands.size() - 1, new Band(left, previous.top(), right, row + 1));
                        continue;
                    }
                }
                bands.add(new Band(left, row, right, row + 1));
            }
            return List.copyOf(bands);
        }
    }

    /**
     * 关闭屏幕后播放的终端淡出画面。
     */
    @Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class TerminalFadeOutRenderer {

        private static TerminalScreen fadingScreen;

        private TerminalFadeOutRenderer() {
        }

        static void begin(TerminalScreen screen) {
            fadingScreen = screen;
        }

        static void clear() {
            fadingScreen = null;
        }

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            if (fadingScreen == null) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null || minecraft.screen != null || fadingScreen.isFadeOutFinished()) {
                clear();
                return;
            }
            fadingScreen.renderFadeOut(event.getGuiGraphics(), event.getPartialTick());
        }

        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && fadingScreen != null && fadingScreen.isFadeOutFinished()) {
                clear();
            }
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            clear();
        }
    }
}
