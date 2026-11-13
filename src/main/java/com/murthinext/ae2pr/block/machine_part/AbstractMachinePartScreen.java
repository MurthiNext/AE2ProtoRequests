package com.murthinext.ae2pr.block.machine_part;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.api.machine.client.AutoTransferButton;
import com.murthinext.ae2pr.api.machine.client.GuiTooltips;
import com.murthinext.ae2pr.registry.ModGuide;

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
        autoTransferButton = new AutoTransferButton(
                leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1 + TOOLBAR_ROW_STEP,
                isOutput() ? AutoTransferButton.Type.PUSH : AutoTransferButton.Type.PULL, this::autoTransferEnabled,
                () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0));
        addRenderableWidget(autoTransferButton);
        recipeIsolationButton = null;
        if (!isOutput()) {
            recipeIsolationButton = new RecipeIsolationButton(
                    leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1 + 2 * TOOLBAR_ROW_STEP,
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
        if (recipeIsolationButton != null &&
            recipeIsolationButton.renderTooltipIfHovered(graphics, font, mouseX, mouseY)) {
            return true;
        }
        if (autoTransferButton == null || !autoTransferButton.isHovered()) {
            return false;
        }
        GuiTooltips.render(graphics, font,
                           List.of(Component.translatable(isOutput() ? "gui.ae2pr.machine_part.auto.push"
                                                                     : "gui.ae2pr.machine_part.auto.pull"),
                                   Component.translatable(autoTransferEnabled()
                                                                  ? "gui.ae2pr.machine_part.auto.enabled"
                                                                  : "gui.ae2pr.machine_part.auto.disabled")),
                           mouseX, mouseY);
        return true;
    }

    protected void drawTank(GuiGraphics graphics, int x, int y) {
        graphics.blit(TANK_TEXTURE, x, y, 0, 0.0F, 0.0F, TANK_WIDTH, TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
    }

    protected void drawTankArrow(GuiGraphics graphics, int x, int y, boolean down) {
        int inner = TANK_WIDTH - 4;
        drawArrow(graphics, x + 2 + (inner - ARROW_WIDTH) / 2, y + 4, down, false);
    }

    protected void drawFluid(GuiGraphics graphics, FluidStack fluid, int x, int y, int innerWidth, int innerHeight,
                             int capacity) {
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        int fill = (int) Math.min(innerHeight, Math.max(1L, (long) fluid.getAmount() * innerHeight / capacity));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                            .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                          ((color >>> 24) & 0xFF) / 255.0F);
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
