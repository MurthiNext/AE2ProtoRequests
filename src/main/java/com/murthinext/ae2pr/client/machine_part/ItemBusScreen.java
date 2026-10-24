package com.murthinext.ae2pr.client.machine_part;

import java.text.NumberFormat;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusMenu;
import com.murthinext.ae2pr.block.machine_part.MachinePartFluidStorage;
import com.murthinext.ae2pr.client.MachineGui;
import com.murthinext.ae2pr.network.BusSlotClickPacket;
import com.murthinext.ae2pr.network.HatchTankClickPacket;

/**
 * 机器部件物品总线界面。
 */
public class ItemBusScreen extends AbstractMachinePartScreen<ItemBusMenu> {

    private static final int SLOT_Y = 47;
    private static final int SLOT_X_MULTI = 52;
    private static final int SLOT_X_SINGLE = 80;
    private static final int SLOT_STEP = 18;
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
    protected boolean autoTransferEnabled() {
        ItemBusBlockEntity bus = clientBus();
        return bus == null || bus.isAutoTransfer();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPanel(graphics);
        renderToolbar(graphics);
        int hovered = hoveredSlot(mouseX, mouseY);
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            MachineGui.drawSlot(graphics, slotX(slot), slotY(slot));
            renderStoredItem(graphics, slot);
            if (slot == hovered) {
                // 高亮叠加在物品之上
                renderSlotHighlight(graphics, slotX(slot), slotY(slot), 0, 0x80FFFFFF);
            }
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

    private int slotX(int slot) {
        if (menu.getFluidStorage() != null) {
            return leftPos + DUAL_SLOT_X + slot % 2 * SLOT_STEP;
        }
        int x = menu.getSlotCount() > 1 ? SLOT_X_MULTI + slot * SLOT_STEP : SLOT_X_SINGLE;
        return leftPos + x;
    }

    private int slotY(int slot) {
        return topPos + SLOT_Y + (menu.getFluidStorage() != null ? slot / 2 * SLOT_STEP : 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tank = hoveredTank((int) mouseX, (int) mouseY);
        if ((button == 0 || button == 1) && tank >= 0) {
            ModNetwork.sendToServer(new HatchTankClickPacket(menu.getBlockPos(), tank, button));
            return true;
        }
        int slot = hoveredSlot((int) mouseX, (int) mouseY);
        if ((button == 0 || button == 1) && slot >= 0) {
            ModNetwork.sendToServer(new BusSlotClickPacket(menu.getBlockPos(), slot, button, hasShiftDown()));
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
        int slot = hoveredSlot(mouseX, mouseY);
        if (slot >= 0) {
            ItemStack stored = menu.getStoredStack(slot);
            if (!stored.isEmpty()) {
                graphics.renderTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), stored),
                        stored.getTooltipImage(), stored, mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderStoredItem(GuiGraphics graphics, int slot) {
        ItemStack stored = menu.getStoredStack(slot);
        if (stored.isEmpty()) {
            return;
        }
        int x = slotX(slot);
        int y = slotY(slot);
        graphics.renderItem(stored, x, y);
        String text = String.valueOf(stored.getCount());
        float scale = Math.min(1.0F, 16.0F / font.width(text));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17.0F, y + 16.0F, 300.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text), -8, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private int hoveredSlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            int x = slotX(slot);
            int y = slotY(slot);
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                return slot;
            }
        }
        return -1;
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
