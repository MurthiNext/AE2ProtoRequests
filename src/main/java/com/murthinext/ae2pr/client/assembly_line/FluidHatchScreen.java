package com.murthinext.ae2pr.client.assembly_line;

import java.text.NumberFormat;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
 * 机器部件流体仓界面。
 */
public class FluidHatchScreen extends AbstractMachinePartScreen<FluidHatchMenu> {

    private static final ResourceLocation TEXTURE_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_input_hatch.png");
    private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_output_hatch.png");
    private static final ResourceLocation TEXTURE_AEV_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/aev_input_hatch.png");
    private static final ResourceLocation TEXTURE_AEV_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/aev_output_hatch.png");

    /** 流体罐位置（与 GUI 贴图一致）：第 i 罐外框左上角，罐内填充区内缩 2px */
    private static final int TANK_X0 = 20;
    private static final int TANK_Y = 34;
    private static final int TANK_W = 18;
    private static final int TANK_H = 68;
    private static final int TANK_STEP = 22;
    private static final int TANK_INNER_W = 14;
    private static final int TANK_INNER_H = 64;

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    public FluidHatchScreen(FluidHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** 客户端同步入口：把服务端各罐流体与自动搬运开关写入本地方块实体（仅由同步包调用）。 */
    public static void applyFluidSync(BlockPos pos, List<FluidStack> fluids, boolean autoTransfer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof FluidHatchBlockEntity hatch) {
            for (int i = 0; i < fluids.size() && i < hatch.getTankCount(); i++) {
                hatch.applyClientFluid(i, fluids.get(i));
            }
            hatch.setAutoTransfer(autoTransfer);
        }
    }

    @Override
    protected boolean isOutput() {
        return menu.isOutputHatch();
    }

    @Override
    protected boolean autoTransferEnabled() {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch == null || hatch.isAutoTransfer();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderToolbar(graphics);
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            renderFluid(graphics, tank);
        }
    }

    private ResourceLocation texture() {
        boolean output = menu.isOutputHatch();
        if (menu.getTankCount() > 1) {
            return output ? TEXTURE_AEV_OUTPUT : TEXTURE_AEV_INPUT;
        }
        return output ? TEXTURE_OUTPUT : TEXTURE_INPUT;
    }

    /** 流体槽点击：左键取出、右键存入（由服务端菜单校验执行）。 */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tank = hoveredTank((int) mouseX, (int) mouseY);
        if ((button == 0 || button == 1) && tank >= 0) {
            ModNetwork.sendToServer(new HatchTankClickPacket(menu.getBlockPos(), tank, button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 罐区悬停：显示所存流体与数量；工具栏提示由基类统一处理。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        int tank = hoveredTank(mouseX, mouseY);
        if (tank >= 0) {
            FluidStack fluid = clientFluid(tank);
            graphics.renderComponentTooltip(font, List.of(
                    fluid.isEmpty() ? Component.translatable("gui.ae2pr.machine_part.tank.empty")
                            : fluid.getDisplayName(),
                    Component.translatable("gui.ae2pr.machine_part.tank.mb", NUMBER.format(fluid.getAmount()))),
                    mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 返回鼠标所在罐的序号，不在罐区时返回 -1。 */
    private int hoveredTank(int mouseX, int mouseY) {
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            int x = leftPos + TANK_X0 + tank * TANK_STEP;
            int y = topPos + TANK_Y;
            if (mouseX >= x && mouseX < x + TANK_W && mouseY >= y && mouseY < y + TANK_H) {
                return tank;
            }
        }
        return -1;
    }

    /** 按储量占比从底部向上平铺流体贴图；按罐内区域裁剪，避免溢出到边框。 */
    private void renderFluid(GuiGraphics graphics, int tank) {
        FluidStack fluid = clientFluid(tank);
        if (fluid.isEmpty()) {
            return;
        }
        int fill = (int) Math.min(TANK_INNER_H,
                Math.max(1L, (long) fluid.getAmount() * TANK_INNER_H / menu.getCapacityPerTank()));
        int x0 = leftPos + TANK_X0 + 2 + tank * TANK_STEP;
        int y0 = topPos + TANK_Y + 2;
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F, ((color >>> 24) & 0xFF) / 255.0F);
        int fillTop = y0 + TANK_INNER_H - fill;
        graphics.enableScissor(x0, fillTop, x0 + TANK_INNER_W, y0 + TANK_INNER_H);
        for (int ty = y0 + TANK_INNER_H - 16; ty + 16 > fillTop; ty -= 16) {
            for (int tx = x0; tx < x0 + TANK_INNER_W; tx += 16) {
                graphics.blit(tx, ty, 0, 16, 16, sprite);
            }
        }
        graphics.disableScissor();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private FluidStack clientFluid(int tank) {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch != null && tank < hatch.getTankCount() ? hatch.getTank(tank).getFluid() : FluidStack.EMPTY;
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
