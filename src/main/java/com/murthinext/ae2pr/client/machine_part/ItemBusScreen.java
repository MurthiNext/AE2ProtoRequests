package com.murthinext.ae2pr.client.machine_part;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusMenu;
import com.murthinext.ae2pr.block.machine_part.MachinePartFluidStorage;
import com.murthinext.ae2pr.block.machine_part.MachinePartLayout;
import com.murthinext.ae2pr.client.CustomIngredientSource;
import com.murthinext.ae2pr.client.MachineGui;
import com.murthinext.ae2pr.menu.DisplaySlot;
import com.murthinext.ae2pr.network.BusSlotClickPacket;
import com.murthinext.ae2pr.network.HatchTankClickPacket;

/**
 * 机器部件物品总线界面。
 */
public class ItemBusScreen extends AbstractMachinePartScreen<ItemBusMenu> implements CustomIngredientSource {

    private static final int ARROW_X = 83;
    private static final int ARROW_Y = 27;
    private static final int DUAL_SLOT_X = 98;
    private static final int DUAL_TANK_X = 42;
    private static final int TANK_Y = 34;

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    public ItemBusScreen(ItemBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /**
     * 客户端同步入口，仅由同步包调用。
     */
    public static void applyStackSync(BlockPos pos, List<ItemStack> stacks, boolean autoTransfer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof ItemBusBlockEntity bus) {
            for (int i = 0; i < stacks.size() && i < bus.getSlotCount(); i++) {
                bus.getStorage().setStackInSlot(i, stacks.get(i));
            }
            bus.setAutoTransfer(autoTransfer);
        }
    }

    @Override
    protected boolean isOutput() {
        return menu.isOutputBus();
    }

    @Override
    public List<Area> getCustomAreas() {
        MachinePartFluidStorage storage = menu.getFluidStorage();
        if (storage == null) {
            return List.of();
        }
        List<Area> areas = new ArrayList<>(storage.getTankCount());
        for (int tank = 0; tank < storage.getTankCount(); tank++) {
            FluidStack fluid = storage.getTank(tank).getFluid();
            if (fluid.isEmpty()) {
                continue;
            }
            areas.add(Area.ofFluid(fluid, new Rect2i(leftPos + DUAL_TANK_X + tank * MachineGui.TANK_STEP,
                    topPos + TANK_Y, MachineGui.TANK_WIDTH, MachineGui.TANK_HEIGHT)));
        }
        return areas;
    }

    @Override
    protected boolean autoTransferEnabled() {
        ItemBusBlockEntity bus = clientBus();
        return bus == null || bus.isAutoTransfer();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(graphics);
        renderToolbar(graphics);
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            MachineGui.drawSlot(graphics, slotX(slot), slotY(slot));
        }
        MachinePartFluidStorage storage = menu.getFluidStorage();
        MachineGui.drawArrow(graphics, leftPos + (storage != null ? DUAL_SLOT_X + 12 : ARROW_X),
                topPos + ARROW_Y, !menu.isOutputBus(), true);
        if (storage != null) {
            for (int tank = 0; tank < storage.getTankCount(); tank++) {
                int x = leftPos + DUAL_TANK_X + tank * MachineGui.TANK_STEP;
                int y = topPos + TANK_Y;
                MachineGui.drawTank(graphics, x, y);
                MachineGui.drawTankArrow(graphics, x, y, !menu.isOutputBus());
                MachineGui.drawFluid(graphics, storage.getTank(tank).getFluid(), x + 2, y + 2,
                        MachineGui.TANK_INNER_WIDTH, MachineGui.TANK_INNER_HEIGHT, storage.getCapacityPerTank());
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // 储量文本由界面绘制，槽位本身只画图标
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            ItemStack stored = menu.getStoredStack(slot);
            if (stored.isEmpty()) {
                continue;
            }
            String text = String.valueOf(stored.getCount());
            int x = slotX(slot) - leftPos;
            int y = slotY(slot) - topPos;
            float scale = Math.min(1.0F, 16.0F / font.width(text));
            graphics.pose().pushPose();
            graphics.pose().translate(x + 17.0F, y + 16.0F, 300.0F);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, -font.width(text), -8, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
    }

    private int slotX(int slot) {
        return leftPos + MachinePartLayout.itemSlotX(menu.getFluidStorage() != null, menu.getSlotCount(), slot);
    }

    private int slotY(int slot) {
        return topPos + MachinePartLayout.itemSlotY(menu.getFluidStorage() != null, slot);
    }
    
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
    protected void slotClicked(@Nullable Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (!(slot instanceof DisplaySlot)) {
            super.slotClicked(slot, slotIdx, mouseButton, clickType);
            return;
        }
        int index = menu.getStorageSlotIndex(slotIdx);
        if (index < 0) {
            return;
        }
        if (clickType == ClickType.CLONE) {
            ModNetwork.sendToServer(new BusSlotClickPacket(menu.getBlockPos(), index, mouseButton, false, true));
        } else if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
            ModNetwork.sendToServer(new BusSlotClickPacket(menu.getBlockPos(), index, mouseButton,
                    clickType == ClickType.QUICK_MOVE, false));
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY)) {
            return;
        }
        int tank = hoveredTank(mouseX, mouseY);
        MachinePartFluidStorage storage = menu.getFluidStorage();
        if (tank >= 0 && storage != null) {
            var fluid = storage.getTank(tank).getFluid();
            graphics.renderComponentTooltip(font, List.of(
                    fluid.isEmpty() ? Component.translatable("gui.ae2pr.machine_part.tank.empty")
                            : fluid.getDisplayName(),
                    Component.translatable("gui.ae2pr.machine_part.tank.mb", NUMBER.format(fluid.getAmount()))),
                    mouseX, mouseY);
            return;
        }
        // 展示槽的物品提示由原版槽位渲染
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private int hoveredTank(int mouseX, int mouseY) {
        MachinePartFluidStorage storage = menu.getFluidStorage();
        if (storage != null) {
            for (int tank = 0; tank < storage.getTankCount(); tank++) {
                int x = leftPos + DUAL_TANK_X + tank * MachineGui.TANK_STEP;
                int y = topPos + TANK_Y;
                if (mouseX >= x && mouseX < x + MachineGui.TANK_WIDTH
                        && mouseY >= y && mouseY < y + MachineGui.TANK_HEIGHT) {
                    return tank;
                }
            }
        }
        return -1;
    }

    @Nullable
    private ItemBusBlockEntity clientBus() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null
                && minecraft.level.getBlockEntity(menu.getBlockPos()) instanceof ItemBusBlockEntity bus) {
            return bus;
        }
        return null;
    }
}
