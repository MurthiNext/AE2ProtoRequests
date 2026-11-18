package com.murthinext.ae2pr.block.machine_part;

import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.api.machine.client.AutoTransferButton;
import com.murthinext.ae2pr.registry.ModGuide;
import com.murthinext.nextui.client.GuiSidebar;
import com.murthinext.nextui.client.GuiTooltips;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMachinePartScreen<M extends AbstractContainerMenu> extends AbstractMachineScreen<M> {

    protected static final int TANK_WIDTH = 18;
    protected static final int TANK_HEIGHT = 68;
    protected static final int TANK_STEP = 22;
    protected static final int TANK_INNER_WIDTH = 14;
    protected static final int TANK_INNER_HEIGHT = 64;

    private static final ResourceLocation TANK_TEXTURE = texture("components/tank");

    @Nullable
    private AutoTransferButton autoTransferButton;

    @Nullable
    private RecipeIsolationButton recipeIsolationButton;

    protected AbstractMachinePartScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    protected abstract boolean isOutput();

    protected abstract boolean autoTransferEnabled();

    protected abstract boolean recipeIsolationEnabled();

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CHAMBERS_PAGE;
    }

    @Override
    protected int toolbarButtons() {
        return isOutput() ? 2 : 3;
    }

    @Override
    protected void init() {
        super.init();
        GuiSidebar toolbar = toolbar();
        autoTransferButton = new AutoTransferButton(
                toolbar.cellX(),
                toolbar.cellY(1),
                isOutput() ? AutoTransferButton.Type.PUSH : AutoTransferButton.Type.PULL,
                this::autoTransferEnabled,
                () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0));
        addRenderableWidget(autoTransferButton);
        recipeIsolationButton = null;
        if (!isOutput()) {
            recipeIsolationButton = new RecipeIsolationButton(
                    toolbar.cellX(),
                    toolbar.cellY(2),
                    this::recipeIsolationEnabled,
                    () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1));
            addRenderableWidget(recipeIsolationButton);
        }
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (recipeIsolationButton != null
                && recipeIsolationButton.renderTooltipIfTarget(graphics, font, mouseX, mouseY)) {
            return true;
        }
        if (autoTransferButton == null || !GuiTooltips.isTooltipTarget(autoTransferButton)) {
            return false;
        }
        GuiTooltips.render(
                graphics,
                font,
                List.of(
                        autoTransferButton.getMessage(),
                        Component.translatable(
                                autoTransferEnabled()
                                        ? "gui.ae2pr.machine_part.auto.enabled"
                                        : "gui.ae2pr.machine_part.auto.disabled")),
                mouseX,
                mouseY);
        return true;
    }

    protected void drawTank(GuiGraphics graphics, int x, int y) {
        graphics.blit(TANK_TEXTURE, x, y, 0, 0.0F, 0.0F, TANK_WIDTH, TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
    }

    protected void drawTankArrow(GuiGraphics graphics, int x, int y, boolean down) {
        int inner = TANK_WIDTH - 4;
        drawArrow(graphics, x + 2 + (inner - ARROW_WIDTH) / 2, y + 4, down, false);
    }
}
