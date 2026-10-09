package com.murthinext.ae2pr.client.assembly_line;

import java.text.NumberFormat;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.FluidHatchMenu;
import com.murthinext.ae2pr.network.HatchTankClickPacket;

/**
 * 赛特斯石英水晶输入/输出仓界面。
 */
public class FluidHatchScreen extends AbstractContainerScreen<FluidHatchMenu> {

    private static final ResourceLocation TEXTURE_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_input_hatch.png");
    private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_output_hatch.png");

    /** 罐内填充区（与 GUI 贴图一致） */
    private static final int TANK_X = 22;
    private static final int TANK_Y = 36;
    private static final int TANK_W = 24;
    private static final int TANK_H = 64;

    private static final int TITLE_X = 7;
    private static final int TITLE_Y = 9;
    private static final int INFO_X = 56;
    private static final int CAPACITY_Y = 40;

    private static final int COLOR_TITLE = 0x55FFFF;
    private static final int COLOR_GRAY = 0x7A8794;

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    /** 左侧工具栏位置（相对 GUI 左上角） */
    private static final int TOOLBAR_X = -22;
    private static final int TOOLBAR_Y = 2;

    @Nullable
    private AutoTransferButton autoTransferButton;

    public FluidHatchScreen(FluidHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    /** 客户端同步入口：把服务端罐内流体与自动搬运开关写入本地方块实体（仅由同步包调用）。 */
    public static void applyFluidSync(BlockPos pos, FluidStack fluid, boolean autoTransfer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof FluidHatchBlockEntity hatch) {
            hatch.applyClientFluid(fluid);
            hatch.setAutoTransfer(autoTransfer);
        }
    }

    @Override
    protected void init() {
        super.init();
        autoTransferButton = new AutoTransferButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1,
                menu.isOutputHatch() ? AutoTransferButton.Type.PUSH : AutoTransferButton.Type.PULL,
                this::autoTransferEnabled,
                () -> Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 0));
        addRenderableWidget(autoTransferButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(menu.isOutputHatch() ? TEXTURE_OUTPUT : TEXTURE_INPUT, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);
        AutoTransferButton.renderToolbar(graphics, leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, 1);
        renderFluid(graphics);
    }

    /** 流体槽点击：左键取出、右键存入（由服务端菜单校验执行）。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && isHoveringTank((int) mouseX, (int) mouseY)) {
            ModNetwork.sendToServer(new HatchTankClickPacket(menu.getBlockPos(), button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 罐区悬停：显示所存流体与数量。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (autoTransferButton != null && autoTransferButton.isHovered()) {
            boolean enabled = autoTransferEnabled();
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable(menu.isOutputHatch() ? "gui.ae2pr.machine_part.auto.push"
                            : "gui.ae2pr.machine_part.auto.pull"),
                    Component.translatable(enabled ? "gui.ae2pr.machine_part.auto.enabled"
                            : "gui.ae2pr.machine_part.auto.disabled"),
                    Component.translatable("gui.ae2pr.machine_part.auto.desc")),
                    mouseX, mouseY);
            return;
        }
        if (isHoveringTank(mouseX, mouseY)) {
            FluidStack fluid = clientFluid();
            if (!fluid.isEmpty()) {
                graphics.renderComponentTooltip(font, List.of(fluid.getDisplayName(),
                        Component.translatable("gui.ae2pr.machine_part.tank.mb", NUMBER.format(fluid.getAmount()))),
                        mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean isHoveringTank(int mouseX, int mouseY) {
        int x = leftPos + TANK_X;
        int y = topPos + TANK_Y;
        return mouseX >= x && mouseX < x + TANK_W && mouseY >= y && mouseY < y + TANK_H;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TITLE_X, TITLE_Y, COLOR_TITLE, false);
        graphics.drawString(font, Component.translatable("gui.ae2pr.machine_part.capacity.fluid",
                NUMBER.format(FluidHatchBlockEntity.CAPACITY / 1000),
                NUMBER.format(FluidHatchBlockEntity.TYPE_CAPACITY)),
                INFO_X, CAPACITY_Y, COLOR_GRAY, false);
    }

    /** 按储量占比从底部向上平铺流体贴图；按罐内区域裁剪，避免溢出到边框。 */
    private void renderFluid(GuiGraphics graphics) {
        FluidStack fluid = clientFluid();
        if (fluid.isEmpty()) {
            return;
        }
        int fill = (int) Math.min(TANK_H,
                Math.max(1L, (long) fluid.getAmount() * TANK_H / FluidHatchBlockEntity.CAPACITY));
        int x0 = leftPos + TANK_X;
        int y0 = topPos + TANK_Y;
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
        int fillTop = y0 + TANK_H - fill;
        graphics.enableScissor(x0, fillTop, x0 + TANK_W, y0 + TANK_H);
        for (int ty = y0 + TANK_H - 16; ty + 16 > fillTop; ty -= 16) {
            for (int tx = x0; tx < x0 + TANK_W; tx += 16) {
                graphics.blit(tx, ty, 0, 16, 16, sprite);
            }
        }
        graphics.disableScissor();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean autoTransferEnabled() {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch == null || hatch.isAutoTransfer();
    }

    private FluidStack clientFluid() {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch != null ? hatch.getTank().getFluid() : FluidStack.EMPTY;
    }

    @Nullable
    private FluidHatchBlockEntity clientHatch() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(menu.getBlockPos()) instanceof FluidHatchBlockEntity hatch) {
            return hatch;
        }
        return null;
    }
}
