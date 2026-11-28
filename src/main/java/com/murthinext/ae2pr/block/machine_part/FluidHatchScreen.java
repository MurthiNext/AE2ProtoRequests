package com.murthinext.ae2pr.block.machine_part;

import com.murthinext.ae2pr.api.machine.client.CustomIngredientSource;
import com.murthinext.ae2pr.api.machine.client.FluidStackDisplay;
import com.murthinext.ae2pr.block.machine_part.network.HatchTankClickPacket;
import com.murthinext.ae2pr.core.network.ModNetwork;
import com.murthinext.nextui.client.GuiFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class FluidHatchScreen extends AbstractMachinePartScreen<FluidHatchMenu> implements CustomIngredientSource {

    private static final int TANK_X0 = 20;
    private static final int TANK_Y = 34;

    public FluidHatchScreen(FluidHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    // 客户端同步入口，仅由同步包调用
    public static void applyFluidSync(
            BlockPos pos, List<FluidStack> fluids, boolean autoTransfer, boolean recipeIsolated) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof FluidHatchBlockEntity hatch) {
            for (int i = 0; i < fluids.size() && i < hatch.getTankCount(); i++) {
                hatch.applyClientFluid(i, fluids.get(i));
            }
            hatch.setAutoTransfer(autoTransfer);
            hatch.setRecipeIsolated(recipeIsolated);
        } else if (minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof DualHatchBlockEntity dual) {
            var storage = dual.getFluidStorage();
            for (int i = 0; i < fluids.size() && i < storage.getTankCount(); i++) {
                storage.applyClientFluid(i, fluids.get(i));
            }
            dual.setAutoTransfer(autoTransfer);
            dual.setRecipeIsolated(recipeIsolated);
        }
    }

    @Override
    protected boolean isOutput() {
        return menu.isOutputHatch();
    }

    @Override
    public List<Area> getCustomAreas() {
        List<Area> areas = new ArrayList<>(menu.getTankCount());
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            FluidStack fluid = clientFluid(tank);
            if (fluid.isEmpty()) {
                continue;
            }
            areas.add(Area.ofFluid(fluid, new Rect2i(tankX(tank), topPos + TANK_Y, TANK_WIDTH, TANK_HEIGHT)));
        }
        return areas;
    }

    @Override
    protected boolean autoTransferEnabled() {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch == null || hatch.isAutoTransfer();
    }

    @Override
    protected boolean recipeIsolationEnabled() {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch != null && hatch.isRecipeIsolated();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            int x = tankX(tank);
            drawTank(graphics, x, topPos + TANK_Y);
            drawTankArrow(graphics, x, topPos + TANK_Y, !menu.isOutputHatch());
            renderFluid(graphics, tank);
        }
    }

    // 左键取出、右键存入，由服务端菜单校验
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
            FluidStackDisplay.renderTankTooltip(graphics, font, clientFluid(tank), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private int hoveredTank(int mouseX, int mouseY) {
        for (int tank = 0; tank < menu.getTankCount(); tank++) {
            int x = tankX(tank);
            int y = topPos + TANK_Y;
            if (mouseX >= x && mouseX < x + TANK_WIDTH && mouseY >= y && mouseY < y + TANK_HEIGHT) {
                return tank;
            }
        }
        return -1;
    }

    private int tankX(int tank) {
        int count = menu.getTankCount();
        int start = count > 2 ? (imageWidth - TANK_WIDTH - (count - 1) * TANK_STEP) / 2 : TANK_X0;
        return leftPos + start + tank * TANK_STEP;
    }

    private void renderFluid(GuiGraphics graphics, int tank) {
        GuiFluids.drawTankFill(
                graphics,
                clientFluid(tank),
                tankX(tank) + 2,
                topPos + TANK_Y + 2,
                TANK_INNER_WIDTH,
                TANK_INNER_HEIGHT,
                menu.getCapacityPerTank());
    }

    private FluidStack clientFluid(int tank) {
        FluidHatchBlockEntity hatch = clientHatch();
        return hatch != null && tank < hatch.getTankCount()
                ? hatch.getTank(tank).getFluid()
                : FluidStack.EMPTY;
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
