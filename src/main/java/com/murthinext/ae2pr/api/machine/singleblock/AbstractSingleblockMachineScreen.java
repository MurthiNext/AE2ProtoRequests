package com.murthinext.ae2pr.api.machine.singleblock;

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

import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.api.machine.client.MachineStyles;
import com.murthinext.ae2pr.api.machine.client.SideConfigButton;
import com.murthinext.ae2pr.api.machine.client.SideConfigPanel;
import com.murthinext.ae2pr.api.machine.side.SideConfig;

public abstract class AbstractSingleblockMachineScreen<M extends AbstractSingleblockMachineMenu<?>>
        extends AbstractMachineScreen<M> {

    // 面配置窗口弹在主界面与左侧工具栏之外
    private static final int PANEL_MARGIN = 4;

    @Nullable
    private SideConfigButton sideConfigButton;
    @Nullable
    private SideConfigPanel sideConfigPanel;
    private boolean sideConfigOpen;

    protected AbstractSingleblockMachineScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation guidePage() {
        return MachineStyles.guidePage(menu.definition());
    }

    @Override
    protected ItemLike upgradeMachine() {
        return menu.definition().block();
    }

    @Override
    protected int titleColor() {
        return MachineStyles.of(menu.definition()).titleColor();
    }

    @Override
    protected int separatorAccent() {
        return MachineStyles.of(menu.definition()).accent();
    }

    @Override
    protected int separatorAccentDark() {
        return MachineStyles.of(menu.definition()).accentDark();
    }

    // 指南 + 面配置
    @Override
    protected int toolbarButtons() {
        return 2;
    }

    @Override
    protected void init() {
        super.init();
        AbstractSingleblockMachineBlockEntity machine = menu.getMachine();
        SideConfig config = machine != null ? machine.getSideConfig() : new SideConfig();
        Direction facing = machine != null
                ? machine.getBlockState().getValue(AbstractSingleblockMachineBlock.FACING)
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
}
