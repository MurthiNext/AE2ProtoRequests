package com.murthinext.ae2pr.api.machine.client;

import appeng.api.upgrades.Upgrades;
import appeng.core.localization.GuiText;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.nextui.client.ContainerChrome;
import com.murthinext.nextui.client.GuiSlots;
import com.murthinext.nextui.client.GuiTheme;
import com.murthinext.nextui.layout.PlayerInventoryLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements GuiExtraAreas {

    protected static final int TOOLBAR_X = -22;
    protected static final int TOOLBAR_Y = 2;
    protected static final int TOOLBAR_ROW_STEP = 20;
    private static final int TOOLBAR_WIDTH = 20;
    private static final int UPGRADE_PANEL_X = 178;
    private static final int UPGRADE_PANEL_Y = 2;
    private static final int UPGRADE_SLOTS = 4;
    private static final int UPGRADE_SLOT_STEP = 20;

    private static final int BAR_WIDTH = 6;
    private static final int BAR_HEIGHT = 18;
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
    protected static final int ARROW_WIDTH = 10;
    protected static final int ARROW_HEIGHT = 12;

    private static final ResourceLocation PROGRESS_BAR_TEXTURE = texture("components/progress_bar");
    private static final ResourceLocation ARROW_ITEM_DOWN = texture("components/arrow_item_down");
    private static final ResourceLocation ARROW_ITEM_UP = texture("components/arrow_item_up");
    private static final ResourceLocation ARROW_FLUID_DOWN = texture("components/arrow_fluid_down");
    private static final ResourceLocation ARROW_FLUID_UP = texture("components/arrow_fluid_up");

    @Nullable
    private GuideButton guideButton;

    protected AbstractMachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = ContainerChrome.DEFAULT_WIDTH;
        this.imageHeight = ContainerChrome.DEFAULT_HEIGHT;
    }

    protected static ResourceLocation texture(String path) {
        return Ids.of("textures/gui/" + path + ".png");
    }

    protected abstract ResourceLocation guidePage();

    @Nullable
    protected ItemLike upgradeMachine() {
        return null;
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        List<Rect2i> areas = new ArrayList<>(2);
        areas.add(new Rect2i(
                leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, TOOLBAR_WIDTH, toolbarButtons() * TOOLBAR_ROW_STEP + 2));
        if (upgradeMachine() != null) {
            areas.add(new Rect2i(
                    leftPos + UPGRADE_PANEL_X,
                    topPos + UPGRADE_PANEL_Y,
                    TOOLBAR_WIDTH,
                    UPGRADE_SLOTS * UPGRADE_SLOT_STEP + 2));
        }
        return areas;
    }

    protected GuiTheme theme() {
        return GuiTheme.DEFAULT;
    }

    protected int topExtension() {
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        topPos += topExtension() / 2;
        guideButton =
                new GuideButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1, () -> GuideOpener.open(guidePage()));
        addRenderableWidget(guideButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ContainerChrome.drawBackground(
                graphics, leftPos, topPos, imageWidth, imageHeight, PlayerInventoryLayout.STANDARD, theme());
        renderToolbar(graphics);
        if (upgradeMachine() != null) {
            renderUpgradeSlots(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ContainerChrome.drawTitle(graphics, font, title, theme());
    }

    protected void drawSlot(GuiGraphics graphics, int x, int y) {
        GuiSlots.draw(graphics, x, y);
    }

    protected void drawArrow(GuiGraphics graphics, int x, int y, boolean down, boolean item) {
        ResourceLocation texture;
        if (item) {
            texture = down ? ARROW_ITEM_DOWN : ARROW_ITEM_UP;
        } else {
            texture = down ? ARROW_FLUID_DOWN : ARROW_FLUID_UP;
        }
        graphics.blit(texture, x, y, 0, 0.0F, 0.0F, ARROW_WIDTH, ARROW_HEIGHT, ARROW_WIDTH, ARROW_HEIGHT);
    }

    protected void drawProgressBar(GuiGraphics graphics, int x, int y, float progress) {
        drawProgressBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, progress, true);
    }

    protected void drawProgressBar(
            GuiGraphics graphics, int x, int y, int width, int height, float progress, boolean vertical) {
        int grooveU = vertical ? BAR_GROOVE_V_U : BAR_GROOVE_H_U;
        int grooveV = vertical ? BAR_GROOVE_V_V : BAR_GROOVE_H_V;
        graphics.blit(PROGRESS_BAR_TEXTURE, x, y, grooveU, grooveV, width, height, BAR_TEX_WIDTH, BAR_TEX_HEIGHT);

        int innerLength = vertical ? height - 2 : width - 2;
        int innerThickness = vertical ? width - 2 : height - 2;
        int filled = Math.round(Mth.clamp(progress, 0.0F, 1.0F) * innerLength);
        if (filled <= 0 || innerThickness <= 0) {
            return;
        }
        int fillU = vertical ? BAR_FILL_V_U : BAR_FILL_H_U;
        int fillV = vertical ? BAR_FILL_V_V : BAR_FILL_H_V;
        if (vertical) {
            fillV += innerLength - filled;
            graphics.blit(
                    PROGRESS_BAR_TEXTURE,
                    x + 1,
                    y + 1 + innerLength - filled,
                    fillU,
                    fillV,
                    innerThickness,
                    filled,
                    BAR_TEX_WIDTH,
                    BAR_TEX_HEIGHT);
        } else {
            graphics.blit(
                    PROGRESS_BAR_TEXTURE,
                    x + 1,
                    y + 1,
                    fillU,
                    fillV,
                    filled,
                    innerThickness,
                    BAR_TEX_WIDTH,
                    BAR_TEX_HEIGHT);
        }
    }

    protected void renderToolbar(GuiGraphics graphics) {
        AutoTransferButton.renderToolbar(graphics, leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, toolbarButtons());
    }

    protected int toolbarButtons() {
        return 1;
    }

    protected void renderUpgradeSlots(GuiGraphics graphics) {
        int x = leftPos + UPGRADE_PANEL_X;
        int y = topPos + UPGRADE_PANEL_Y;
        AutoTransferButton.renderToolbar(graphics, x, y, UPGRADE_SLOTS);
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            drawSlot(graphics, x + 2, y + 2 + i * UPGRADE_SLOT_STEP);
        }
    }

    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return guideButton != null && guideButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY);
    }

    protected boolean renderUpgradeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemLike machine = upgradeMachine();
        if (machine == null || !isHoveringUpgradePanel(mouseX, mouseY)) {
            return false;
        }
        Slot slot = getSlotUnderMouse();
        if (slot != null && !slot.getItem().isEmpty()) {
            return false;
        }
        List<Component> lines = new ArrayList<>(Upgrades.getTooltipLinesForMachine(machine));
        if (lines.isEmpty()) {
            return false;
        }
        lines.add(0, GuiText.CompatibleUpgrades.text());
        GuiTooltips.render(graphics, font, lines, mouseX, mouseY);
        return true;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderCustomTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    // 供子类追加自定义提示，返回 true 表示已处理
    protected boolean renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return renderToolbarTooltip(graphics, mouseX, mouseY) || renderUpgradeTooltip(graphics, mouseX, mouseY);
    }

    private boolean isHoveringUpgradePanel(int mouseX, int mouseY) {
        int x = leftPos + UPGRADE_PANEL_X;
        int y = topPos + UPGRADE_PANEL_Y;
        int height = UPGRADE_SLOTS * UPGRADE_SLOT_STEP + 2;
        return mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + height;
    }
}
