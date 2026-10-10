package com.murthinext.ae2pr.client.assembly_line;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.ItemBusMenu;
import com.murthinext.ae2pr.network.BusSlotClickPacket;

/**
 * 机器部件物品总线界面。
 */
public class ItemBusScreen extends AbstractMachinePartScreen<ItemBusMenu> {

    private static final ResourceLocation TEXTURE_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_input_bus.png");
    private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_output_bus.png");
    private static final ResourceLocation TEXTURE_AEV_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/aev_input_bus.png");
    private static final ResourceLocation TEXTURE_AEV_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/aev_output_bus.png");

    /** 存储区物品位（与 GUI 贴图一致）：多槽为一行四格、整体居中 */
    private static final int SLOT_Y = 47;
    private static final int SLOT_X_MULTI = 52;
    private static final int SLOT_X_SINGLE = 80;
    private static final int SLOT_STEP = 18;

    public ItemBusScreen(ItemBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    /** 客户端同步入口：把服务端各槽存储内容与自动搬运开关写入本地方块实体（仅由同步包调用）。 */
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
        graphics.blit(texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderToolbar(graphics);
        int hovered = hoveredSlot(mouseX, mouseY);
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            renderStoredItem(graphics, slot);
            if (slot == hovered) {
                // 与原生槽位一致：白色高亮叠加在物品之上
                renderSlotHighlight(graphics, slotX(slot), topPos + SLOT_Y, 0, 0x80FFFFFF);
            }
        }
    }

    private ResourceLocation texture() {
        boolean output = menu.isOutputBus();
        if (menu.getSlotCount() > 1) {
            return output ? TEXTURE_AEV_OUTPUT : TEXTURE_AEV_INPUT;
        }
        return output ? TEXTURE_OUTPUT : TEXTURE_INPUT;
    }

    private int slotX(int slot) {
        int x = menu.getSlotCount() > 1 ? SLOT_X_MULTI + slot * SLOT_STEP : SLOT_X_SINGLE;
        return leftPos + x;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int slot = hoveredSlot((int) mouseX, (int) mouseY);
        if ((button == 0 || button == 1) && slot >= 0) {
            ModNetwork.sendToServer(new BusSlotClickPacket(menu.getBlockPos(), slot, button, hasShiftDown()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 存储区悬停：按槽位惯例显示物品 tooltip；工具栏提示由基类统一处理。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (renderToolbarTooltip(graphics, mouseX, mouseY)) {
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

    /** 绘制存储物品与数量（数量超宽时缩小，贴物品格右下角）。 */
    private void renderStoredItem(GuiGraphics graphics, int slot) {
        ItemStack stored = menu.getStoredStack(slot);
        if (stored.isEmpty()) {
            return;
        }
        int x = slotX(slot);
        int y = topPos + SLOT_Y;
        graphics.renderItem(stored, x, y);
        String text = String.valueOf(stored.getCount());
        float scale = Math.min(1.0F, 16.0F / font.width(text));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17.0F, y + 16.0F, 300.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text), -8, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    /** 返回鼠标所在存储槽序号，不在存储区时返回 -1。 */
    private int hoveredSlot(int mouseX, int mouseY) {
        for (int slot = 0; slot < menu.getSlotCount(); slot++) {
            int x = slotX(slot);
            int y = topPos + SLOT_Y;
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                return slot;
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
