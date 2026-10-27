package com.murthinext.ae2pr.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ae2pr;

// 机器界面的公共绘制：面板、分隔线、物品槽、流体罐与箭标
public final class GeneralMachineScreen {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 200;
    public static final int SLOT = 18;
    public static final int INV_X = 8;
    public static final int INV_Y = 122;
    public static final int HOTBAR_Y = 180;
    public static final int INV_COLS = 9;
    public static final int INV_ROWS = 3;
    public static final int TANK_WIDTH = 18;
    public static final int TANK_HEIGHT = 68;
    public static final int TANK_STEP = 22;
    public static final int TANK_INNER_WIDTH = 14;
    public static final int TANK_INNER_HEIGHT = 64;

    public static final int TOOLBAR_X = -22;
    public static final int TOOLBAR_Y = 2;
    public static final int TOOLBAR_ROW_STEP = 20;
    public static final int TOOLBAR_WIDTH = 20;
    public static final int UPGRADE_PANEL_X = 178;
    public static final int UPGRADE_PANEL_Y = 2;
    public static final int UPGRADE_SLOTS = 4;
    public static final int UPGRADE_SLOT_STEP = 20;

    private static final int SEPARATOR_Y = 21;
    private static final int SEPARATOR_LEFT = 7;
    private static final int SEPARATOR_RIGHT = 8;
    private static final int SEPARATOR_ACCENT_WIDTH = 12;
    private static final int COLOR_SEPARATOR = 0xFF2C3A46;

    public static final int ACCENT_CRYSTAL = 0xFF8ABBEF;
    public static final int ACCENT_CRYSTAL_DARK = 0x80294057;
    public static final int ACCENT_EMBER = 0xFFE6AF50;
    public static final int ACCENT_EMBER_DARK = 0x808A5A20;

    public static final int BAR_WIDTH = 6;
    public static final int BAR_HEIGHT = 18;

    // progress_bar.png 精灵表：竖向凹槽/填充在前，横向在后
    private static final int BAR_TEX_WIDTH = 26;
    private static final int BAR_TEX_HEIGHT = 36;
    private static final int BAR_GROOVE_V_U = 0;
    private static final int BAR_GROOVE_V_V = 0;
    private static final int BAR_FILL_V_U = 0;
    private static final int BAR_FILL_V_V = 20;
    private static final int BAR_GROOVE_H_U = 8;
    private static final int BAR_GROOVE_H_V = 0;
    private static final int BAR_FILL_H_U = 8;
    private static final int BAR_FILL_H_V = 20;

    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 12;

    private static final ResourceLocation PANEL_TEXTURE = texture("components/panel");
    private static final ResourceLocation SLOT_TEXTURE = texture("components/slot");
    private static final ResourceLocation TANK_TEXTURE = texture("components/tank");
    private static final ResourceLocation PROGRESS_BAR_TEXTURE = texture("components/progress_bar");
    private static final ResourceLocation ARROW_ITEM_DOWN = texture("components/arrow_item_down");
    private static final ResourceLocation ARROW_ITEM_UP = texture("components/arrow_item_up");
    private static final ResourceLocation ARROW_FLUID_DOWN = texture("components/arrow_fluid_down");
    private static final ResourceLocation ARROW_FLUID_UP = texture("components/arrow_fluid_up");

    private GeneralMachineScreen() {
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "textures/gui/" + path + ".png");
    }

    /**
     * 界面之外占据空间的区域，用于让配方查看器的物品列表避让。
     *
     * @param toolbarButtons 左侧工具栏的按钮格数
     * @param upgradePanel   是否绘制了右侧升级面板
     */
    public static List<Rect2i> exclusionAreas(int left, int top, int toolbarButtons, boolean upgradePanel) {
        List<Rect2i> areas = new ArrayList<>(2);
        areas.add(new Rect2i(left + TOOLBAR_X, top + TOOLBAR_Y, TOOLBAR_WIDTH,
                toolbarButtons * TOOLBAR_ROW_STEP + 2));
        if (upgradePanel) {
            areas.add(new Rect2i(left + UPGRADE_PANEL_X, top + UPGRADE_PANEL_Y, TOOLBAR_WIDTH,
                    UPGRADE_SLOTS * UPGRADE_SLOT_STEP + 2));
        }
        return areas;
    }

    // 面板与槽位

    public static void drawPanel(GuiGraphics graphics, int left, int top) {
        graphics.blit(PANEL_TEXTURE, left, top, 0, 0.0F, 0.0F, WIDTH, HEIGHT, WIDTH, HEIGHT);
    }

    public static void drawSeparator(GuiGraphics graphics, int left, int top, int accent, int accentDark) {
        int y = top + SEPARATOR_Y;
        graphics.fill(left + SEPARATOR_LEFT, y, left + WIDTH - SEPARATOR_RIGHT, y + 1, COLOR_SEPARATOR);
        graphics.fill(left + SEPARATOR_LEFT, y, left + SEPARATOR_LEFT + SEPARATOR_ACCENT_WIDTH, y + 1, accent);
        graphics.fill(left + SEPARATOR_LEFT, y + 1, left + SEPARATOR_LEFT + SEPARATOR_ACCENT_WIDTH, y + 2,
                accentDark);
    }

    public static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SLOT_TEXTURE, x - 1, y - 1, 0, 0.0F, 0.0F, SLOT, SLOT, SLOT, SLOT);
    }

    public static void drawInventorySlots(GuiGraphics graphics, int left, int top) {
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                drawSlot(graphics, left + INV_X + col * SLOT, top + INV_Y + row * SLOT);
            }
        }
        for (int col = 0; col < INV_COLS; col++) {
            drawSlot(graphics, left + INV_X + col * SLOT, top + HOTBAR_Y);
        }
    }

    public static void drawTank(GuiGraphics graphics, int x, int y) {
        graphics.blit(TANK_TEXTURE, x, y, 0, 0.0F, 0.0F, TANK_WIDTH, TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
    }

    public static void drawTankArrow(GuiGraphics graphics, int x, int y, boolean down) {
        int inner = TANK_WIDTH - 4;
        drawArrow(graphics, x + 2 + (inner - ARROW_WIDTH) / 2, y + 4, down, false);
    }

    public static void drawArrow(GuiGraphics graphics, int x, int y, boolean down, boolean quartz) {
        ResourceLocation texture;
        if (quartz) {
            texture = down ? ARROW_ITEM_DOWN : ARROW_ITEM_UP;
        } else {
            texture = down ? ARROW_FLUID_DOWN : ARROW_FLUID_UP;
        }
        graphics.blit(texture, x, y, 0, 0.0F, 0.0F, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
    }

    // 进度条与流体

    // AE2 机器风格进度条，默认纵向自下而上填充
    public static void drawProgressBar(GuiGraphics graphics, int x, int y, float progress) {
        drawProgressBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, progress, true);
    }

    /**
     * AE2 机器风格进度条：先画凹槽，再按 progress 取填充段贴图。progress 取 0~1。
     *
     * @param vertical {@code true} 自下而上，{@code false} 自左向右
     */
    public static void drawProgressBar(GuiGraphics graphics, int x, int y, int width, int height,
            float progress, boolean vertical) {
        int grooveU = vertical ? BAR_GROOVE_V_U : BAR_GROOVE_H_U;
        int grooveV = vertical ? BAR_GROOVE_V_V : BAR_GROOVE_H_V;
        graphics.blit(PROGRESS_BAR_TEXTURE, x, y, grooveU, grooveV, width, height,
                BAR_TEX_WIDTH, BAR_TEX_HEIGHT);

        int innerLength = vertical ? height - 2 : width - 2;
        int innerThickness = vertical ? width - 2 : height - 2;
        int filled = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * innerLength);
        if (filled <= 0 || innerThickness <= 0) {
            return;
        }
        int fillU = vertical ? BAR_FILL_V_U : BAR_FILL_H_U;
        int fillV = vertical ? BAR_FILL_V_V : BAR_FILL_H_V;
        if (vertical) {
            // 从底端截取，对应材质中的起始端
            fillV += innerLength - filled;
            graphics.blit(PROGRESS_BAR_TEXTURE, x + 1, y + 1 + innerLength - filled, fillU, fillV,
                    innerThickness, filled, BAR_TEX_WIDTH, BAR_TEX_HEIGHT);
        } else {
            graphics.blit(PROGRESS_BAR_TEXTURE, x + 1, y + 1, fillU, fillV,
                    filled, innerThickness, BAR_TEX_WIDTH, BAR_TEX_HEIGHT);
        }
    }

    public static void drawFluid(GuiGraphics graphics, FluidStack fluid, int x, int y, int innerWidth,
            int innerHeight, int capacity) {
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int fill = (int) Math.min(innerHeight,
                Math.max(1L, (long) fluid.getAmount() * innerHeight / capacity));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
        int fillTop = y + innerHeight - fill;
        graphics.enableScissor(x, fillTop, x + innerWidth, y + innerHeight);
        for (int ty = y + innerHeight - 16; ty + 16 > fillTop; ty -= 16) {
            for (int tx = x; tx < x + innerWidth; tx += 16) {
                graphics.blit(tx, ty, 0, 16, 16, sprite);
            }
        }
        graphics.disableScissor();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
