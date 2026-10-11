package com.murthinext.ae2pr.client.proto_terminal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

/** 源始终端的平行四边形剧情树及图文展示层。 */
public final class ProtoTerminalScreen extends Screen {

    private static final int CANVAS_WIDTH = 548;
    private static final int CANVAS_HEIGHT = 340;
    private static final int MAIN_X = 4;
    private static final int MAIN_Y = 8;
    private static final int MAIN_WIDTH = 540;
    private static final int MAIN_HEIGHT = 324;
    private static final float MAIN_SLANT = CrystalNarrativeGraphics.slant(MAIN_HEIGHT);
    private static final int TITLE_HEIGHT = 34;
    private static final int TITLE_WIDTH = 171;
    private static final float TITLE_SLANT = CrystalNarrativeGraphics.slant(TITLE_HEIGHT);
    private static final float TITLE_X = MAIN_X + MAIN_SLANT - TITLE_SLANT;
    private static final int CONTROL_SIZE = 34;
    private static final int CLOSE_X = MAIN_X + MAIN_WIDTH - CONTROL_SIZE;
    private static final int TREE_LEFT = 52;
    private static final int TREE_TOP = 68;
    private static final int NODE_WIDTH = 144;
    private static final int NODE_HEIGHT = 46;
    private static final int DETAIL_X = 38;
    private static final int DETAIL_Y = 58;
    private static final int DETAIL_WIDTH = 472;
    private static final int DETAIL_HEIGHT = 242;
    private static final float DETAIL_SLANT = CrystalNarrativeGraphics.slant(DETAIL_HEIGHT);
    private static final int BACK_X = DETAIL_X + DETAIL_WIDTH - CONTROL_SIZE;
    // 详情页标题紧贴分隔线上方，分隔线与返回按钮底边齐平并缩短为短横线
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
    // Ctrl 滚轮缩放剧情内容与星空的范围与步进
    private static final float ZOOM_STEP = 0.2F;
    private static final float MIN_ZOOM = 0.5F;
    private static final float MAX_ZOOM = 3F;

    private final TerminalStoryTree story;
    private final TerminalAnimation animation;
    private final OpeningPixelStream openingPixels = new OpeningPixelStream(MAIN_WIDTH, MAIN_HEIGHT);
    private final TerminalStarfield starfield = new TerminalStarfield(MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT);
    private final List<Star> stars = new ArrayList<>();
    private final Map<String, TerminalStoryTree.Node> nodesById = new HashMap<>();
    private final List<NarrativeButton> nodeButtons = new ArrayList<>();
    private long openingStarted = -1;
    private TerminalStoryTree.Node selectedNode;
    private List<FormattedCharSequence> detailLines = List.of();
    private NarrativeButton backButton;
    private NarrativeButton closeButton;
    private long detailStarted;
    private long detailClosingStarted = -1;
    private long terminalClosingStarted = -1;
    private float detailOpacity;
    private int detailShift = -18;
    private float detailExitOpacity;
    private int detailExitShift;
    private float windowOpacity = 1;
    private List<ParallelogramClip.Band> treeClipBands = List.of();
    private double detailScroll;
    private float panX;
    private float panY;
    private boolean panning;
    private float contentZoom = 1;
    private float canvasScale;
    private float canvasLeft;
    private float canvasTop;
    private int framebufferWidth;
    private int framebufferHeight;

    private record Star(float x, float y, float phase, float speed, boolean cross) {
    }

    private ProtoTerminalScreen(TerminalStoryTree story) {
        super(Component.translatable("item.ae2pr.proto_terminal"));
        this.story = story;
        this.animation = new TerminalAnimation();
        for (TerminalStoryTree.Node node : story.nodes()) {
            nodesById.put(node.id(), node);
        }
        Random random = new Random(0xAE2C1257L);
        for (int i = 0; i < 90; i++) {
            stars.add(new Star(MAIN_X + random.nextFloat() * MAIN_WIDTH, MAIN_Y + random.nextFloat() * MAIN_HEIGHT,
                    random.nextFloat() * 6.28F,
                    0.3F + random.nextFloat() * 0.6F, i % 11 == 0));
        }
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ProtoTerminalScreen)) {
            TerminalFadeOutOverlay.clear();
            minecraft.setScreen(new ProtoTerminalScreen(TerminalStoryTree.load()));
        }
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
            nodeButtons.add(addRenderableWidget(new NarrativeButton(0, 0, NODE_WIDTH, NODE_HEIGHT,
                    node.title(), button -> showNode(node))));
        }
        backButton = addRenderableWidget(new NarrativeButton(BACK_X, DETAIL_Y, CONTROL_SIZE,
                NarrativeButton.Icon.BACK, label("back"), button -> closeDetail()));
        closeButton = addRenderableWidget(new NarrativeButton(CLOSE_X, MAIN_Y, CONTROL_SIZE,
                NarrativeButton.Icon.CLOSE, label("close"), button -> closeTerminal()));
        updateNodePositions(0);
        updateVisibility();
    }

    /** 当前是否已过入场动画且可以交互。 */
    private boolean isInteractive() {
        return terminalClosingStarted < 0 && openingStarted >= 0
                && Util.getMillis() - openingStarted >= TerminalAnimation.OPEN_DURATION;
    }

    /** 以鼠标位置为锚点缩放面板内的剧情树与星空。 */
    private void zoomContent(double mouseX, double mouseY, double amount) {
        float target = Mth.clamp(contentZoom + (float) amount * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        if (target == contentZoom) {
            return;
        }
        double canvasX = localX(mouseX);
        double canvasY = localY(mouseY);
        panX = (float) Mth.clamp(canvasX - TREE_LEFT - (canvasX - TREE_LEFT - panX) / contentZoom * target,
                -4352, 4352);
        panY = (float) Mth.clamp(canvasY - TREE_TOP - (canvasY - TREE_TOP - panY) / contentZoom * target,
                -4352, 4352);
        starfield.zoom(target, (float) canvasX, (float) canvasY);
        contentZoom = target;
        updateNodePositions(Util.getMillis() - openingStarted);
    }

    /** 重算剧情树的平行四边形裁剪带。 */
    private void updateTreeClipBands() {
        double guiScale = minecraft.getWindow().getGuiScale();
        treeClipBands = ParallelogramClip.bands((canvasLeft + MAIN_X * canvasScale) * guiScale,
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
        if (terminalClosingStarted >= 0 && windowOpacity <= 0) {
            return;
        }
        long elapsed = (terminalClosingStarted < 0 ? now : terminalClosingStarted) - openingStarted;
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
                float headerOpacity = animation.headerOpacity(elapsed);
                if (headerOpacity >= 4.0F / 255) {
                    CrystalNarrativeGraphics.panel(graphics, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT,
                            CrystalNarrativeGraphics.alpha(0xFF0D1D35, headerOpacity),
                            CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.BORDER, headerOpacity));
                    graphics.drawString(font, title, 53, 20,
                            CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.TEXT, headerOpacity), false);
                }
                if (selectedNode != null) {
                    CrystalNarrativeGraphics.panel(graphics, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT,
                            CrystalNarrativeGraphics.alpha(0x020712, detailOpacity * 0.72F), 0);
                    renderDetail(graphics, now, detailOpacity);
                    backButton.render(graphics, localMouseX, localMouseY, partialTick);
                }
                closeButton.render(graphics, localMouseX, localMouseY, partialTick);
                if (progress >= 1) {
                    graphics.drawManaged(() -> CrystalNarrativeGraphics.outline(graphics,
                            MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT, CrystalNarrativeGraphics.BORDER));
                }
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
        float scale = CrystalNarrativeGraphics.ease(progress);
        int panelWidth = Math.round(MAIN_WIDTH * scale);
        int panelHeight = Math.round(MAIN_HEIGHT * scale);
        int x = MAIN_X + (MAIN_WIDTH - panelWidth) / 2;
        int y = MAIN_Y + (MAIN_HEIGHT - panelHeight) / 2;
        float slant = CrystalNarrativeGraphics.slant(panelHeight);
        starfield.renderNebula(graphics, x, y, panelWidth, panelHeight, slant, seconds);
        openingPixels.render(graphics, x, y, panelWidth, panelHeight, slant, progress, seconds,
                starfield);
        starfield.renderStars(graphics, x, y, panelWidth, panelHeight, slant, seconds,
                CrystalNarrativeGraphics.ease(progress));
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
                if (!CrystalNarrativeGraphics.contains(px, py, x + 2, y + 2, panelWidth - 4, panelHeight - 4, slant)) {
                    continue;
                }
                float light = (0.08F + 0.19F * (0.5F + 0.5F
                        * Mth.sin(seconds * star.speed() + star.phase()))) * opacity;
                graphics.fill(px, py, px + 1, py + 1, CrystalNarrativeGraphics.alpha(0xB0CDED, light));
                if (star.cross()
                        && CrystalNarrativeGraphics.contains(px - 2, py - 2, x, y, panelWidth, panelHeight, slant)
                        && CrystalNarrativeGraphics.contains(px + 2, py + 2, x, y, panelWidth, panelHeight, slant)) {
                    int glow = CrystalNarrativeGraphics.alpha(0x77A7D3, light * 0.3F);
                    graphics.fill(px - 2, py, px + 3, py + 1, glow);
                    graphics.fill(px, py - 2, px + 1, py + 3, glow);
                }
            }
        });
    }

    private void renderTree(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, long elapsed) {
        float connectionProgress = animation.connectionProgress(elapsed);
        double guiScale = minecraft.getWindow().getGuiScale();
        List<ParallelogramClip.Band> clipBands = treeClipBands;
        if (elapsed < TerminalAnimation.OPEN_DURATION) {
            float scale = CrystalNarrativeGraphics.ease(animation.opening(elapsed));
            int panelWidth = Math.round(MAIN_WIDTH * scale);
            int panelHeight = Math.round(MAIN_HEIGHT * scale);
            int x = MAIN_X + (MAIN_WIDTH - panelWidth) / 2;
            int y = MAIN_Y + (MAIN_HEIGHT - panelHeight) / 2;
            clipBands = ParallelogramClip.bands((canvasLeft + x * canvasScale) * guiScale,
                    (canvasTop + y * canvasScale) * guiScale, panelWidth * canvasScale * guiScale,
                    panelHeight * canvasScale * guiScale,
                    CrystalNarrativeGraphics.slant(panelHeight) * canvasScale * guiScale);
        }
        boolean wasScissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int[] previousScissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, previousScissor);
        graphics.flush();
        try {
            for (ParallelogramClip.Band band : clipBands) {
                RenderSystem.enableScissor(band.left(), minecraft.getWindow().getHeight() - band.bottom(),
                        band.right() - band.left(), band.bottom() - band.top());
                float top = (float) (band.top() / guiScale - canvasTop) / canvasScale;
                float bottom = (float) (band.bottom() / guiScale - canvasTop) / canvasScale;
                float left = (float) (band.left() / guiScale - canvasLeft) / canvasScale;
                float right = (float) (band.right() / guiScale - canvasLeft) / canvasScale;
                renderTreeBand(graphics, mouseX, mouseY, partialTick, connectionProgress, left, top, right, bottom);
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
            float connectionProgress, float left, float top, float right, float bottom) {
        graphics.drawManaged(() -> {
            for (TerminalStoryTree.Node node : story.nodes()) {
                TerminalStoryTree.Node parent = node.parent() == null ? null : nodesById.get(node.parent());
                if (parent != null && connectionProgress > 0
                        && TREE_TOP + panY + (Math.max(parent.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom
                                + 1 >= top
                        && TREE_TOP + panY + (Math.min(parent.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom
                                - 1 < bottom) {
                    renderConnection(graphics, parent, node, connectionProgress);
                }
            }
        });
        boolean mouseInside = (selectedNode == null || detailClosingStarted >= 0) && insideTree(mouseX, mouseY);
        for (NarrativeButton button : nodeButtons) {
            if (button.getY() + button.getHeight() * contentZoom >= top && button.getY() < bottom
                    && button.getX() + button.getWidth() * contentZoom >= left && button.getX() < right) {
                button.render(graphics, mouseInside ? mouseX : -10000, mouseInside ? mouseY : -10000, partialTick);
            }
        }
    }

    private void renderConnection(GuiGraphics graphics, TerminalStoryTree.Node source,
            TerminalStoryTree.Node target, float progress) {
        boolean forward = target.x() >= source.x();
        float nodeSlant = CrystalNarrativeGraphics.slant(NODE_HEIGHT);
        float leftEdge = nodeSlant / 2;
        float rightEdge = NODE_WIDTH - nodeSlant / 2;
        float x1 = TREE_LEFT + panX + (source.x() + (forward ? rightEdge : leftEdge)) * contentZoom;
        float y1 = TREE_TOP + panY + (source.y() + NODE_HEIGHT / 2.0F) * contentZoom;
        float x2 = TREE_LEFT + panX + (target.x() + (forward ? leftEdge : rightEdge)) * contentZoom;
        float y2 = TREE_TOP + panY + (target.y() + NODE_HEIGHT / 2.0F) * contentZoom;
        CrystalNarrativeGraphics.connection(graphics, x1, y1, x2, y2,
                nodeSlant, NODE_HEIGHT, progress, 0xFF3B5D83, Math.max(1, contentZoom));
    }

    private void renderDetail(GuiGraphics graphics, long now, float reveal) {
        if (reveal < 4.0F / 255) {
            return;
        }
        int shift = detailShift;
        int x = DETAIL_X + shift;
        CrystalNarrativeGraphics.panel(graphics, x + 5, DETAIL_Y + 6, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.DETAIL_BACKGROUND, reveal * 0.4F), 0);
        CrystalNarrativeGraphics.panel(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.DETAIL_BACKGROUND, reveal),
                CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.BORDER, reveal));
        renderStarlight(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                (now - openingStarted) / 1000.0F, reveal * 0.7F);
        graphics.drawString(font, font.plainSubstrByWidth(selectedNode.title().getString(), 298),
                TEXT_LEFT + shift, DETAIL_TITLE_Y, CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.ACCENT, reveal),
                false);
        int border = CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.BORDER, reveal);
        int textColor = CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.TEXT, reveal);
        graphics.fill(TEXT_LEFT - 2 + shift, DETAIL_RULE_Y, TEXT_LEFT - 2 + DETAIL_RULE_WIDTH + shift,
                DETAIL_RULE_Y + 1, border);
        int textRight = selectedNode.image() == null ? TEXT_RIGHT : IMAGE_TEXT_RIGHT;
        clip(graphics, TEXT_LEFT + shift, TEXT_TOP, textRight + shift, TEXT_BOTTOM);
        for (int i = 0; i < detailLines.size(); i++) {
            int lineY = TEXT_TOP + i * 13 - (int) detailScroll;
            if (lineY >= TEXT_TOP - 12 && lineY < TEXT_BOTTOM) {
                graphics.drawString(font, detailLines.get(i), TEXT_LEFT + shift, lineY, textColor, false);
            }
        }
        graphics.disableScissor();
        if (selectedNode.image() != null) {
            float scale = Math.min(110.0F / selectedNode.imageWidth(), 138.0F / selectedNode.imageHeight());
            int imageWidth = Math.max(1, Math.round(selectedNode.imageWidth() * scale));
            int imageHeight = Math.max(1, Math.round(selectedNode.imageHeight() * scale));
            CrystalNarrativeGraphics.panel(graphics, 340 + shift, 102, 150, 157, CrystalNarrativeGraphics.slant(157),
                    CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.DETAIL_BACKGROUND, reveal), border);
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
            int thumbHeight = Math.max(12, textHeight * textHeight / (detailLines.size() * 13));
            int thumbY = TEXT_TOP + (int) ((textHeight - thumbHeight) * detailScroll / maxScroll);
            graphics.fill(textRight - 2 + shift, TEXT_TOP, textRight - 1 + shift, TEXT_BOTTOM, border);
            graphics.fill(textRight - 2 + shift, thumbY, textRight - 1 + shift, thumbY + thumbHeight,
                    CrystalNarrativeGraphics.alpha(CrystalNarrativeGraphics.ACCENT, reveal));
        }
    }

    private void showNode(TerminalStoryTree.Node node) {
        selectedNode = node;
        detailStarted = Util.getMillis();
        detailClosingStarted = -1;
        detailOpacity = 0;
        detailShift = -18;
        detailScroll = 0;
        int textRight = node.image() == null ? TEXT_RIGHT : IMAGE_TEXT_RIGHT;
        detailLines = font.split(node.text(), textRight - TEXT_LEFT - 10);
        panning = false;
        setFocused(backButton);
        updateVisibility();
        if (minecraft != null) {
            minecraft.getNarrator().sayNow(node.title().copy().append(". ").append(node.text()));
        }
    }

    private void showTree() {
        selectedNode = null;
        detailClosingStarted = -1;
        detailOpacity = 0;
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
                TerminalFadeOutOverlay.begin(this);
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
            detailOpacity = CrystalNarrativeGraphics.ease((now - detailStarted) / (float) DETAIL_ENTER_DURATION);
            detailShift = Math.round(-18 * (1 - detailOpacity));
        }
    }

    private void updateVisibility() {
        long now = Util.getMillis();
        long elapsed = openingStarted < 0 ? 0 : now - openingStarted;
        boolean interactive = isInteractive();
        boolean showingDetail = selectedNode != null && detailClosingStarted < 0;
        for (int i = 0; i < nodeButtons.size(); i++) {
            NarrativeButton button = nodeButtons.get(i);
            float progress = animation.nodeProgress(elapsed, i);
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
        for (int i = 0; i < nodeButtons.size(); i++) {
            TerminalStoryTree.Node node = story.nodes().get(i);
            NarrativeButton button = nodeButtons.get(i);
            float progress = animation.nodeProgress(elapsed, i);
            button.setScale(contentZoom);
            button.setX(TREE_LEFT + Math.round(panX + node.x() * contentZoom)
                    + Math.round(-24 * (1 - progress) * contentZoom));
            button.setY(TREE_TOP + Math.round(panY + node.y() * contentZoom));
            button.setOpacity(progress);
        }
    }

    private int maxDetailScroll() {
        return Math.max(0, detailLines.size() * 13 - (TEXT_BOTTOM - TEXT_TOP));
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
        return CrystalNarrativeGraphics.contains(x, y, MAIN_X, MAIN_Y, MAIN_WIDTH, MAIN_HEIGHT, MAIN_SLANT);
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
            if (backButton.mouseClicked(x, y, button) || detailClosingStarted < 0) {
                return true;
            }
        }
        if (CrystalNarrativeGraphics.contains(x, y, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT)) {
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
            detailScroll = Mth.clamp(detailScroll - delta * 26, 0, maxDetailScroll());
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
}
