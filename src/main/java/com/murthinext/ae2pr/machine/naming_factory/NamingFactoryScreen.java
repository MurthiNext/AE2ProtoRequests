package com.murthinext.ae2pr.machine.naming_factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ItemLike;

import com.murthinext.ae2pr.api.machine.client.SideConfigButton;
import com.murthinext.ae2pr.api.machine.client.SideConfigPanel;
import com.murthinext.ae2pr.api.machine.side.SideConfig;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.registry.ModGuide;
import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;

public class NamingFactoryScreen extends AbstractMachineScreen<NamingFactoryMenu> {

    private static final int TEMPLATE_SLOT_X = 120;
    private static final int TEMPLATE_SLOT_Y = 50;
    private static final int INPUT_SLOT_X = 80;
    private static final int INPUT_SLOT_Y = 26;
    private static final int OUTPUT_SLOT_X = 80;
    private static final int OUTPUT_SLOT_Y = 74;
    private static final int PRESS_ARROW_X = 83;
    private static final int PRESS_ARROW_Y = 52;
    // 箭头右侧的竖向进度条
    private static final int PROGRESS_X = 96;
    private static final int PROGRESS_Y = 49;

    private static final int TEXT_X = 7;
    private static final int TITLE_Y = 9;
    private static final int NAME_Y = 103;

    // 面配置窗口弹在主界面与左侧工具栏之外
    private static final int PANEL_MARGIN = 4;

    private static final int COLOR_TITLE = 0x55FFFF;
    private static final int COLOR_VALUE = 0xACE9FF;
    private static final int COLOR_TEXT = 0xAAB8C6;
    private static final int COLOR_GRAY = 0x7A8794;

    @Nullable
    private SideConfigButton sideConfigButton;
    @Nullable
    private SideConfigPanel sideConfigPanel;
    private boolean sideConfigOpen;

    public NamingFactoryScreen(NamingFactoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.NAMING_FACTORY_PAGE;
    }

    @Override
    protected ItemLike upgradeMachine() {
        return ModMachines.NAMING_FACTORY.block();
    }

    @Override
    protected int toolbarButtons() {
        return 2;
    }

    @Override
    protected void init() {
        super.init();
        NamingFactoryBlockEntity blockEntity = menu.getBlockEntity();
        SideConfig config = blockEntity != null ? blockEntity.getSideConfig() : new SideConfig();
        Direction facing = blockEntity != null
                ? blockEntity.getBlockState().getValue(NamingFactoryBlock.FACING)
                : Direction.NORTH;
        sideConfigPanel = new SideConfigPanel(config, facing, this::pressSideConfigButton);
        sideConfigButton = new SideConfigButton(leftPos + TOOLBAR_X + 1,
                topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP,
                () -> sideConfigOpen, this::toggleSideConfig);
        addRenderableWidget(sideConfigButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (sideConfigOpen) {
            sideConfigPanel().render(graphics, font, panelX(), panelY(), mouseX, mouseY);
        }
    }

    // 面配置窗口独立于主界面，点击优先交给它处理
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sideConfigOpen && sideConfigPanel().mouseClicked(mouseX, mouseY, button, panelX(), panelY())) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // 面配置展开时 ESC 先收起窗口，而不是直接关闭界面
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (sideConfigOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            toggleSideConfig();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        List<Rect2i> areas = new ArrayList<>(super.getGuiExtraAreas());
        if (sideConfigOpen) {
            areas.add(new Rect2i(panelX(), panelY(), SideConfigPanel.WIDTH, SideConfigPanel.HEIGHT));
        }
        return areas;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        drawSlot(graphics, leftPos + TEMPLATE_SLOT_X, topPos + TEMPLATE_SLOT_Y);
        drawSlot(graphics, leftPos + INPUT_SLOT_X, topPos + INPUT_SLOT_Y);
        drawSlot(graphics, leftPos + OUTPUT_SLOT_X, topPos + OUTPUT_SLOT_Y);
        drawArrow(graphics, leftPos + PRESS_ARROW_X, topPos + PRESS_ARROW_Y, true, true);
        NamingFactoryBlockEntity blockEntity = menu.getBlockEntity();
        float progress = blockEntity != null ? blockEntity.getWorkProgress(partialTick) : 0;
        drawProgressBar(graphics, leftPos + PROGRESS_X, topPos + PROGRESS_Y, progress);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TEXT_X, TITLE_Y, COLOR_TITLE, false);

        drawSlotLabelAbove(graphics, "gui.ae2pr.naming_factory.template", 120, 50);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.input", 80, 26);
        drawSlotLabelLeft(graphics, "gui.ae2pr.naming_factory.output", 80, 74);

        NamingFactoryBlockEntity blockEntity = menu.getBlockEntity();
        String name = blockEntity != null ? blockEntity.getImprintName() : null;
        if (name == null) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.naming_factory.name.none"),
                    TEXT_X, NAME_Y, COLOR_GRAY, false);
        } else {
            String text = Component.translatable("gui.ae2pr.naming_factory.name", name).getString();
            graphics.drawString(font, clip(text, imageWidth - TEXT_X * 2), TEXT_X, NAME_Y, COLOR_VALUE, false);
        }
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        return sideConfigButton != null && sideConfigButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY);
    }

    private SideConfigPanel sideConfigPanel() {
        return Objects.requireNonNull(sideConfigPanel);
    }

    private int panelX() {
        return Math.max(2, leftPos + TOOLBAR_X - PANEL_MARGIN - SideConfigPanel.WIDTH);
    }

    private int panelY() {
        return topPos + TOOLBAR_Y;
    }

    private void toggleSideConfig() {
        sideConfigOpen = !sideConfigOpen;
    }

    private void pressSideConfigButton(int id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void drawSlotLabelAbove(GuiGraphics graphics, String key, int slotX, int slotY) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, slotX + (16 - font.width(label)) / 2, slotY - 10, COLOR_TEXT, false);
    }

    private void drawSlotLabelLeft(GuiGraphics graphics, String key, int slotX, int slotY) {
        Component label = Component.translatable(key);
        graphics.drawString(font, label, slotX - 4 - font.width(label), slotY + 4, COLOR_TEXT, false);
    }

    private String clip(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        return font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
    }
}
