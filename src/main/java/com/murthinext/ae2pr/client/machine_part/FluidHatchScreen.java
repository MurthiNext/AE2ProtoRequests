package com.murthinext.ae2pr.client.machine_part;

import java.text.NumberFormat;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.block.machine_part.FluidHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.FluidHatchMenu;
import com.murthinext.ae2pr.client.MachineGui;
import com.murthinext.ae2pr.network.HatchTankClickPacket;

/**
 * 机器部件流体仓界面。
 */
public class FluidHatchScreen extends AbstractMachinePartScreen<FluidHatchMenu> {

    private static final int TANK_X0 = 20;
    private static final int TANK_Y = 34;
    private static final int TANK_STEP = MachineGui.TANK_STEP;

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    public FluidHatchScreen(FluidHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * 客户端同步入口，仅由同步包调用。
     */
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
        renderPanel(graphics);
        renderToolbar(graphics);
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            MachineGui.drawTank(graphics, leftPos + TANK_X0 + tank * TANK_STEP, topPos + TANK_Y);
            MachineGui.drawTankArrow(graphics, leftPos + TANK_X0 + tank * TANK_STEP, topPos + TANK_Y,
                    !menu.isOutputHatch());
            renderFluid(graphics, tank);
        }
    }

    /**
     * 左键取出、右键存入，由服务端菜单校验。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tank = hoveredTank((int) mouseX, (int) mouseY);
        if ((button == 0 || button == 1) && tank >= 0) {
            ModNetwork.sendToServer(new HatchTankClickPacket(menu.getBlockPos(), tank, button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

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

    private int hoveredTank(int mouseX, int mouseY) {
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            int x = leftPos + TANK_X0 + tank * TANK_STEP;
            int y = topPos + TANK_Y;
            if (mouseX >= x && mouseX < x + MachineGui.TANK_WIDTH
                    && mouseY >= y && mouseY < y + MachineGui.TANK_HEIGHT) {
                return tank;
            }
        }
        return -1;
    }

    private void renderFluid(GuiGraphics graphics, int tank) {
        MachineGui.drawFluid(graphics, clientFluid(tank),
                leftPos + TANK_X0 + 2 + tank * TANK_STEP, topPos + TANK_Y + 2,
                MachineGui.TANK_INNER_WIDTH, MachineGui.TANK_INNER_HEIGHT, menu.getCapacityPerTank());
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
