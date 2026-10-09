package com.murthinext.ae2pr.client.assembly_line;

import java.text.NumberFormat;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
 * 赛特斯石英水晶输入/输出总线界面。
 */
public class ItemBusScreen extends AbstractContainerScreen<ItemBusMenu> {

    private static final ResourceLocation TEXTURE_INPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_input_bus.png");
    private static final ResourceLocation TEXTURE_OUTPUT = new ResourceLocation(ae2pr.MODID,
            "textures/gui/certus_quartz_crystal_output_bus.png");

    /** 存储区物品位（与 GUI 贴图一致） */
    private static final int STORAGE_X = 80;
    private static final int STORAGE_Y = 47;

    private static final int TEXT_X = 7;
    private static final int TITLE_Y = 9;

    private static final int COLOR_TITLE = 0x55FFFF;

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    /** 左侧工具栏位置（相对 GUI 左上角） */
    private static final int TOOLBAR_X = -22;
    private static final int TOOLBAR_Y = 2;

    @Nullable
    private AutoTransferButton autoTransferButton;

    public ItemBusScreen(ItemBusMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
    }

    /** 客户端同步入口：把服务端存储内容与自动搬运开关写入本地方块实体（仅由同步包调用）。 */
    public static void applyStackSync(BlockPos pos, ItemStack stack, boolean autoTransfer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof ItemBusBlockEntity bus) {
            bus.getStorage().setStackInSlot(0, stack);
            bus.setAutoTransfer(autoTransfer);
        }
    }

    @Override
    protected void init() {
        super.init();
        autoTransferButton = new AutoTransferButton(leftPos + TOOLBAR_X + 1, topPos + TOOLBAR_Y + 1,
                menu.isOutputBus() ? AutoTransferButton.Type.PUSH : AutoTransferButton.Type.PULL,
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
        graphics.blit(menu.isOutputBus() ? TEXTURE_OUTPUT : TEXTURE_INPUT, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);
        AutoTransferButton.renderToolbar(graphics, leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, 1);
        renderStoredItem(graphics);
        if (isHoveringStorage(mouseX, mouseY)) {
            // 与原生槽位一致：白色高亮叠加在物品之上
            AbstractContainerScreen.renderSlotHighlight(graphics, leftPos + STORAGE_X, topPos + STORAGE_Y, 0,
                    0x80FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, TEXT_X, TITLE_Y, COLOR_TITLE, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && isHoveringStorage((int) mouseX, (int) mouseY)) {
            ModNetwork.sendToServer(new BusSlotClickPacket(menu.getBlockPos(), button, hasShiftDown()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** 存储区悬停：按槽位惯例显示物品 tooltip。 */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (autoTransferButton != null && autoTransferButton.isHovered()) {
            boolean enabled = autoTransferEnabled();
            boolean output = menu.isOutputBus();
            graphics.renderComponentTooltip(font, List.of(
                    Component.translatable(output ? "gui.ae2pr.machine_part.auto.push"
                            : "gui.ae2pr.machine_part.auto.pull"),
                    Component.translatable(enabled ? "gui.ae2pr.machine_part.auto.enabled"
                            : "gui.ae2pr.machine_part.auto.disabled"),
                    Component.translatable("gui.ae2pr.machine_part.auto.desc")),
                    mouseX, mouseY);
            return;
        }
        if (isHoveringStorage(mouseX, mouseY)) {
            ItemStack stored = menu.getStoredStack();
            if (!stored.isEmpty()) {
                graphics.renderTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), stored),
                        stored.getTooltipImage(), stored, mouseX, mouseY);
            }
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    /** 绘制存储物品与数量（数量超宽时缩小，贴物品格右下角）。 */
    private void renderStoredItem(GuiGraphics graphics) {
        ItemStack stored = menu.getStoredStack();
        if (stored.isEmpty()) {
            return;
        }
        int x = leftPos + STORAGE_X;
        int y = topPos + STORAGE_Y;
        graphics.renderItem(stored, x, y);
        String text = String.valueOf(stored.getCount());
        float scale = Math.min(1.0F, 16.0F / font.width(text));
        graphics.pose().pushPose();
        graphics.pose().translate(x + 17.0F, y + 16.0F, 300.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, -font.width(text), -8, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private boolean isHoveringStorage(int mouseX, int mouseY) {
        int x = leftPos + STORAGE_X;
        int y = topPos + STORAGE_Y;
        return mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17;
    }

    private boolean autoTransferEnabled() {
        ItemBusBlockEntity bus = clientBus();
        return bus == null || bus.isAutoTransfer();
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
