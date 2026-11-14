package com.murthinext.ae2pr.item.proto_terminal;

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
import net.minecraft.util.Mth;

import com.murthinext.ae2pr.ae2pr;

// 终端界面主要渲染
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
    private static final int DETAIL_X = TerminalStoryPanel.X;
    private static final int DETAIL_Y = TerminalStoryPanel.Y;
    private static final int DETAIL_WIDTH = TerminalStoryPanel.WIDTH;
    private static final int DETAIL_HEIGHT = TerminalStoryPanel.HEIGHT;
    private static final float DETAIL_SLANT = TerminalStoryPanel.SLANT;
    private static final int BACK_X = DETAIL_X + DETAIL_WIDTH - CONTROL_SIZE;
    private static final long DETAIL_ENTER_DURATION = 330;
    private static final long DETAIL_EXIT_DURATION = 240;
    private static final long TERMINAL_EXIT_DURATION = 260;
    // 父块读完后，子块连线的起始停顿
    private static final long POST_FINISH_DELAY = 340;
    // Ctrl 滚轮缩放的范围、步进与缓动时长
    private static final float ZOOM_STEP = 0.2F;
    private static final float DEFAULT_ZOOM = 0.7F;
    private static final float MIN_ZOOM = 0.25F;
    private static final float MAX_ZOOM = 3F;
    private static final long ZOOM_DURATION = 200;

    private final TerminalStoryTree story;
    private final TerminalAnimation animation;
    private final TerminalOpeningPixelRenderer openingPixels = new TerminalOpeningPixelRenderer(MAIN_X, MAIN_Y,
                                                                                                MAIN_WIDTH,
                                                                                                MAIN_HEIGHT);
    private final TerminalStarfieldRenderer starfield = new TerminalStarfieldRenderer(MAIN_X, MAIN_Y, MAIN_WIDTH,
                                                                                      MAIN_HEIGHT);
    private final List<Star> stars = new ArrayList<>();
    private final List<TerminalButton> nodeButtons = new ArrayList<>();
    // 每个剧情块连线动画相对终端展开起点的开始时间；父块读完后才登记
    private final Map<String, Long> revealAt = new HashMap<>();
    private long openingStarted = -1;
    private TerminalStoryPanel detail;
    private TerminalStoryRunner runner;
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

    private record Star(float x, float y, float phase, float speed, boolean cross) {}

    private TerminalScreen(TerminalStoryTree story) {
        super(Component.literal(TerminalTitleRenderer.TEXT));
        this.story = story;
        this.animation = new TerminalAnimation(story.nodes());
        Random random = new Random(0xAE2C1257L);
        for (int i = 0; i < 90; i++) {
            stars.add(new Star(MAIN_X + random.nextFloat() * MAIN_WIDTH, MAIN_Y + random.nextFloat() * MAIN_HEIGHT,
                               random.nextFloat() * 6.28F, 0.3F + random.nextFloat() * 0.6F, i % 11 == 0));
        }
        // 星空与初始缩放保持一致，后续缩放按同一基准换算
        starfield.zoom(DEFAULT_ZOOM, MAIN_X + MAIN_WIDTH / 2.0F, MAIN_Y + MAIN_HEIGHT / 2.0F);
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof TerminalScreen)) {
            TerminalFadeOutRenderer.clear();
            try {
                TerminalStoryTree story = TerminalStoryTree.load();
                TerminalStoryProgressCache.request(story.inventoryKeys());
                minecraft.setScreen(new TerminalScreen(story));
            } catch (Exception exception) {
                ae2pr.LOGGER.error("Failed to load proto terminal story", exception);
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(label("load_failed"));
                }
            }
        }
    }

    static void onProgressSynced() {
        if (Minecraft.getInstance().screen instanceof TerminalScreen screen) {
            screen.applyProgressReset();
        }
    }

    private void applyProgressReset() {
        if (detail != null) {
            showTree();
        }
        revealAt.clear();
        TerminalStoryProgress progress = TerminalStoryProgressCache.get();
        try {
            runner = new TerminalStoryRunner(story.program(), progress, TerminalStoryText.playerName());
            if (runner.programChanged()) {
                ae2pr.LOGGER.info("Proto terminal story program changed, rebuilding reading progress");
                if (minecraft.player != null) {
                    minecraft.player.sendSystemMessage(label("program_changed"));
                }
                TerminalStoryProgressCache.record(runner.snapshot(), null);
            }
        } catch (RuntimeException exception) {
            runner = null;
            ae2pr.LOGGER.error("Failed to restore proto terminal story", exception);
            if (minecraft.player != null) {
                minecraft.player.sendSystemMessage(label("load_failed"));
            }
        }
        updateVisibility();
    }

    @Override
    protected void init() {
        canvasScale = Math.max(
                0.1F, Math.min(1.5F, Math.min((width - 12.0F) / CANVAS_WIDTH, (height - 12.0F) / CANVAS_HEIGHT)));
        canvasLeft = (width - CANVAS_WIDTH * canvasScale) / 2;
        canvasTop = (height - CANVAS_HEIGHT * canvasScale) / 2;
        updateTreeClipBands();
        framebufferWidth = minecraft.getWindow().getWidth();
        framebufferHeight = minecraft.getWindow().getHeight();
        nodeButtons.clear();
        for (TerminalStoryTree.Node node : story.nodes()) {
            nodeButtons.add(addRenderableWidget(new TerminalButton(0, 0, NODE_WIDTH, NODE_HEIGHT, node.title(),
                                                                   button -> showNode(node))));
        }
        backButton = addRenderableWidget(new TerminalButton(BACK_X, DETAIL_Y, CONTROL_SIZE, TerminalButton.Icon.BACK,
                                                            label("back"), button -> closeDetail()));
        closeButton = addRenderableWidget(new TerminalButton(CLOSE_X, MAIN_Y, CONTROL_SIZE, TerminalButton.Icon.CLOSE,
                                                             label("close"), button -> closeTerminal()));
        updateNodePositions(0);
        updateVisibility();
    }

    private boolean isInteractive() {
        return terminalClosingStarted < 0 && openingStarted >= 0 &&
                Util.getMillis() - openingStarted >= TerminalAnimation.OPEN_DURATION;
    }

    // 平滑缩放
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
        panX = (float) Mth.clamp(zoomAnchorX - TREE_LEFT - (zoomAnchorX - TREE_LEFT - panX) / contentZoom * zoom, -4352,
                                 4352);
        panY = (float) Mth.clamp(zoomAnchorY - TREE_TOP - (zoomAnchorY - TREE_TOP - panY) / contentZoom * zoom, -4352,
                                 4352);
        starfield.zoom(zoom, zoomAnchorX, zoomAnchorY);
        contentZoom = zoom;
    }

    private void updateTreeClipBands() {
        double guiScale = minecraft.getWindow().getGuiScale();
        treeClipBands = TerminalParallelogramClip.bands((canvasLeft + MAIN_X * canvasScale) * guiScale,
                                                        (canvasTop + MAIN_Y * canvasScale) * guiScale,
                                                        MAIN_WIDTH * canvasScale * guiScale,
                                                        MAIN_HEIGHT * canvasScale * guiScale,
                                                        MAIN_SLANT * canvasScale * guiScale);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long now = Util.getMillis();
        if (openingStarted < 0) {
            openingStarted = now;
        }
        updateTransitions(now);
        updateZoom(now);
        if (detail != null && detailClosingStarted < 0 && terminalClosingStarted < 0) {
            detail.update(now);
        }
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
                if (detail != null) {
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
                graphics.drawManaged(()
                                             -> TerminalGraphicsRenderer.outline(graphics, MAIN_X, MAIN_Y, MAIN_WIDTH,
                                                                                 MAIN_HEIGHT, MAIN_SLANT,
                                                                                 TerminalGraphicsRenderer.BORDER));
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
        if (width != minecraft.getWindow().getGuiScaledWidth() ||
            height != minecraft.getWindow().getGuiScaledHeight() ||
            framebufferWidth != minecraft.getWindow().getWidth() ||
            framebufferHeight != minecraft.getWindow().getHeight()) {
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
        TerminalTitleRenderer.render(graphics, TITLE_X, MAIN_Y, TITLE_WIDTH, TITLE_HEIGHT, TITLE_SLANT, opacity,
                                     elapsed - TerminalAnimation.HEADER_START);
    }

    private void renderStarlight(GuiGraphics graphics, int x, int y, int panelWidth, int panelHeight, float slant,
                                 float seconds, float opacity) {
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
                float light = (0.08F + 0.19F * (0.5F + 0.5F * Mth.sin(seconds * star.speed() + star.phase()))) *
                        opacity;
                graphics.fill(px, py, px + 1, py + 1, TerminalGraphicsRenderer.alpha(0xB0CDED, light));
                if (star.cross() &&
                    TerminalGraphicsRenderer.contains(px - 2, py - 2, x, y, panelWidth, panelHeight, slant) &&
                    TerminalGraphicsRenderer.contains(px + 2, py + 2, x, y, panelWidth, panelHeight, slant)) {
                    int glow = TerminalGraphicsRenderer.alpha(0x77A7D3, light * 0.3F);
                    graphics.fill(px - 2, py, px + 3, py + 1, glow);
                    graphics.fill(px, py - 2, px + 1, py + 3, glow);
                }
            }
        });
    }

    private void renderTree(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, long elapsed) {
        double guiScale = minecraft.getWindow().getGuiScale();
        List<TerminalParallelogramClip.Band> clipBands = getClipBands(elapsed, guiScale);
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
                RenderSystem.enableScissor(previousScissor[0], previousScissor[1], previousScissor[2],
                                           previousScissor[3]);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }

    private List<TerminalParallelogramClip.Band> getClipBands(long elapsed, double guiScale) {
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
        return clipBands;
    }

    private void renderTreeBand(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, long elapsed,
                                float left, float top, float right, float bottom) {
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
                    if (TREE_TOP + panY + (Math.max(source.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom + 1 >=
                                top &&
                        TREE_TOP + panY + (Math.min(source.y(), node.y()) + NODE_HEIGHT / 2.0F) * contentZoom - 1 <
                                bottom) {
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
                                                      TerminalGraphicsRenderer.alpha(0xFF3B5D83, 1 - reveal),
                                                      Math.max(1, contentZoom));
                    }
                }
            }
        });
        boolean mouseInside = (detail == null || detailClosingStarted >= 0) && insideTree(mouseX, mouseY);
        for (TerminalButton button : nodeButtons) {
            if (button.getY() + button.getHeight() * contentZoom >= top && button.getY() < bottom &&
                button.getX() + button.getWidth() * contentZoom >= left && button.getX() < right) {
                button.render(graphics, mouseInside ? mouseX : -10000, mouseInside ? mouseY : -10000, partialTick);
            }
        }
    }

    private void renderConnection(GuiGraphics graphics, TerminalStoryTree.Node source, TerminalStoryTree.Node target,
                                  float progress) {
        float nodeSlant = TerminalGraphicsRenderer.slant(NODE_HEIGHT);
        float stepX = target.x() - source.x();
        float stepY = target.y() - source.y();
        boolean vertical = Math.abs(stepX) - NODE_WIDTH + nodeSlant < Math.abs(stepY) * nodeSlant / NODE_HEIGHT &&
                Math.abs(stepY) - NODE_HEIGHT >= Math.abs(stepX) * NODE_HEIGHT / nodeSlant;
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
        TerminalGraphicsRenderer.connection(graphics, x1, y1, x2, y2, nodeSlant, NODE_HEIGHT, vertical, progress,
                                            0xFF3B5D83, Math.max(1, contentZoom));
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
        int x = DETAIL_X + detailShift;
        TerminalGraphicsRenderer.panel(
                graphics, x + 5, DETAIL_Y + 6, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND, reveal * 0.4F), 0);
        TerminalGraphicsRenderer.panel(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                                       TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.DETAIL_BACKGROUND,
                                                                      reveal),
                                       TerminalGraphicsRenderer.alpha(TerminalGraphicsRenderer.BORDER, reveal));
        renderStarlight(graphics, x, DETAIL_Y, DETAIL_WIDTH, DETAIL_HEIGHT, DETAIL_SLANT,
                        (now - openingStarted) / 1000.0F, reveal * 0.7F);
        detail.render(graphics, now, reveal, detailShift, windowOpacity, mouseX, mouseY, detailClosingStarted < 0);
    }

    private void showNode(TerminalStoryTree.Node node) {
        detail = new TerminalStoryPanel(story, node, runner, isNodeUnlocked(node));
        detailStarted = Util.getMillis();
        detailClosingStarted = -1;
        detailOpacity = 0;
        detailShift = -18;
        panning = false;
        setFocused(backButton);
        updateVisibility();
    }

    void onUnlocksSynced() {
        if (detail != null && detail.awaitingUnlock() && isNodeUnlocked(detail.node())) {
            detail.unlock();
            detailStarted = Util.getMillis() - DETAIL_ENTER_DURATION;
            detailOpacity = 1;
            detailShift = 0;
        }
        updateVisibility();
    }

    // 连线与节点入场都相对本块自己的 revealAt 起点播放
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
        boolean showingDetail = detail != null && detailClosingStarted < 0;
        for (TerminalStoryTree.Node node : story.nodes()) {
            if (revealAt.containsKey(node.id()) || !isNodeVisible(node)) {
                continue;
            }
            boolean root = node.links().isEmpty();
            if (!root && (showingDetail || !TerminalStoryProgressCache.isSynced())) {
                continue;
            }
            revealAt.put(node.id(),
                         root ? animation.connectionStart(node)
                              : elapsed + POST_FINISH_DELAY +
                                         Math.min(animation.layer(node), 3) *
                                                 (TerminalAnimation.CONNECTION_DURATION +
                                                  TerminalAnimation.NODE_DURATION));
        }
    }

    private boolean isNodeVisible(TerminalStoryTree.Node node) {
        return runner != null &&
                (node.visibleWhen().inventoryKeys().isEmpty() || TerminalStoryProgressCache.areUnlocksSynced()) &&
                node.visibleWhen().matches(runner, TerminalStoryProgressCache::isUnlocked);
    }

    // 剧情块是否已解锁
    private boolean isNodeUnlocked(TerminalStoryTree.Node node) {
        return runner != null &&
                (node.availableWhen().inventoryKeys().isEmpty() || TerminalStoryProgressCache.areUnlocksSynced()) &&
                node.availableWhen().matches(runner, TerminalStoryProgressCache::isUnlocked);
    }

    private void showTree() {
        detail = null;
        detailClosingStarted = -1;
        detailOpacity = 0;
        setFocused(null);
        updateVisibility();
    }

    private void closeDetail() {
        if (detail == null || terminalClosingStarted >= 0) {
            return;
        }
        if (detailClosingStarted >= 0) {
            showTree();
            return;
        }
        detail.save();
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
        windowOpacity = terminalClosingStarted < 0
                ? 1
                : TerminalAnimation.fadeOut(now - terminalClosingStarted, TERMINAL_EXIT_DURATION);
        if (detail == null) {
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
        boolean showingDetail = detail != null && detailClosingStarted < 0;
        for (int i = 0; i < nodeButtons.size(); i++) {
            TerminalStoryTree.Node node = story.nodes().get(i);
            TerminalButton button = nodeButtons.get(i);
            float progress = nodeReveal(elapsed, node);
            boolean locked = progress > 0 && !isNodeUnlocked(node);
            button.setLocked(locked);
            button.visible = progress > 0;
            button.active = button.visible && !showingDetail && interactive && TerminalStoryProgressCache.isSynced() &&
                    runner != null;
        }
        backButton.visible = detail != null;
        backButton.active = detail != null && interactive;
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
        if (detail != null) {
            if (backButton.mouseClicked(x, y, button)) {
                return true;
            }
            if (detailClosingStarted < 0) {
                if (button == 0) {
                    detail.click(x, y, detailShift);
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
            if (detail != null) {
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
        if (detail != null && detailClosingStarted < 0 && terminalClosingStarted < 0 &&
            insideTree(localX(mouseX), localY(mouseY))) {
            detail.scroll(delta);
            return true;
        }
        if (detail == null && hasControlDown() && isInteractive()) {
            zoomContent(mouseX, mouseY, delta);
            return true;
        }
        return super.mouseScrolled(localX(mouseX), localY(mouseY), delta);
    }

    @Override
    public void onClose() {
        if (detail != null && detailClosingStarted < 0) {
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
        if (detail != null) {
            detail.save();
        }
        TerminalStoryProgressCache.stopWatching();
        super.removed();
    }
}
