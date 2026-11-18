package com.murthinext.ae2pr.api.machine.client;

import appeng.api.upgrades.Upgrades;
import appeng.core.localization.GuiText;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.nextui.client.ContainerChrome;
import com.murthinext.nextui.client.GuiExtraAreas;
import com.murthinext.nextui.client.GuiSidebar;
import com.murthinext.nextui.client.GuiSlots;
import com.murthinext.nextui.client.GuiTheme;
import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.layout.PlayerInventoryLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M>
        implements GuiExtraAreas {

    private static final int TOOLBAR_X = -22;
    private static final int SIDEBAR_Y = 2;
    private static final int UPGRADE_PANEL_GAP = 2;
    private static final int UPGRADE_SLOTS = 4;

    protected static final int ARROW_WIDTH = 10;
    protected static final int ARROW_HEIGHT = 12;

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
        areas.add(toolbar().bounds());
        if (upgradeMachine() != null) {
            areas.add(upgradePanel().bounds());
        }
        return areas;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !isInExtraArea(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    protected GuiSidebar toolbar() {
        return new GuiSidebar(leftPos + TOOLBAR_X, topPos + SIDEBAR_Y, toolbarButtons());
    }

    private GuiSidebar upgradePanel() {
        return new GuiSidebar(leftPos + imageWidth + UPGRADE_PANEL_GAP, topPos + SIDEBAR_Y, UPGRADE_SLOTS);
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
        GuiSidebar toolbar = toolbar();
        guideButton = new GuideButton(toolbar.cellX(), toolbar.cellY(0), () -> GuideOpener.open(guidePage()));
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
        toolbar().draw(graphics);
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

    protected int toolbarButtons() {
        return 1;
    }

    protected void renderUpgradeSlots(GuiGraphics graphics) {
        GuiSidebar panel = upgradePanel();
        panel.draw(graphics);
        for (int i = 0; i < UPGRADE_SLOTS; i++) {
            drawSlot(graphics, panel.cellX() + 1, panel.cellY(i) + 1);
        }
    }

    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return guideButton != null && guideButton.renderTooltipIfTarget(graphics, font, mouseX, mouseY);
    }

    protected boolean renderUpgradeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemLike machine = upgradeMachine();
        if (machine == null || !GuiExtraAreas.contains(upgradePanel().bounds(), mouseX, mouseY)) {
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
}
