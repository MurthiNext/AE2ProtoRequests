package com.murthinext.ae2pr.mixin;

import com.murthinext.ae2pr.mixin.accessor.AEBaseMenuInvoker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;

import appeng.client.gui.implementations.CellWorkbenchScreen;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.CellWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 元件工作台界面：高级过滤元件用白/黑名单输入框替换 63 格配置区。
 */
@Mixin(value = CellWorkbenchScreen.class, remap = false)
public abstract class CellWorkbenchScreenMixin extends UpgradeableScreen<CellWorkbenchMenu> {

    private static final int ae2pr$FIELD_WIDTH = 128;
    private static final int ae2pr$FIELD_X = 10;
    private static final int ae2pr$WHITELIST_Y = 44;
    private static final int ae2pr$BLACKLIST_Y = 82;

    @Unique
    private AETextField ae2pr$whitelistField;
    @Unique
    private AETextField ae2pr$blacklistField;
    @Unique
    private boolean ae2pr$advanced;
    @Unique
    private String ae2pr$observedWhite = "";
    @Unique
    private String ae2pr$observedBlack = "";
    @Unique
    private String ae2pr$sentWhite = "";
    @Unique
    private String ae2pr$sentBlack = "";

    public CellWorkbenchScreenMixin(CellWorkbenchMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ae2pr$initFields(CellWorkbenchMenu menu, Inventory playerInventory, Component title, ScreenStyle style,
            CallbackInfo ci) {
        var white = new AETextField(this.style, this.font, 0, 0, ae2pr$FIELD_WIDTH, 14);
        white.setMaxLength(256);
        white.setBordered(false);
        white.setVisible(false);
        white.setPlaceholder(Component.literal("minecraft:logs"));
        white.setResponder(this::ae2pr$onWhitelistChanged);
        this.ae2pr$whitelistField = white;

        var black = new AETextField(this.style, this.font, 0, 0, ae2pr$FIELD_WIDTH, 14);
        black.setMaxLength(256);
        black.setBordered(false);
        black.setVisible(false);
        black.setPlaceholder(Component.literal("minecraft:planks"));
        black.setResponder(this::ae2pr$onBlacklistChanged);
        this.ae2pr$blacklistField = black;
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void ae2pr$updateFields(CallbackInfo ci) {
        var white = this.ae2pr$whitelistField;
        var black = this.ae2pr$blacklistField;
        if (white == null || black == null) {
            return;
        }

        // 窗口缩放会重建控件列表，按需补挂
        if (!this.children().contains(white)) {
            this.addRenderableWidget(white);
        }
        if (!this.children().contains(black)) {
            this.addRenderableWidget(black);
        }

        var cell = this.menu.getWorkbenchItem();
        boolean advanced = cell.getItem() instanceof AdvancedFilterCellItem;
        if (advanced != this.ae2pr$advanced) {
            this.ae2pr$advanced = advanced;
            this.setSlotsHidden(SlotSemantics.CONFIG, advanced);
        }

        white.setVisible(advanced);
        black.setVisible(advanced);
        if (!advanced) {
            return;
        }

        white.setX(this.getGuiLeft() + ae2pr$FIELD_X + 2);
        white.setY(this.getGuiTop() + ae2pr$WHITELIST_Y + 2);
        black.setX(this.getGuiLeft() + ae2pr$FIELD_X + 2);
        black.setY(this.getGuiTop() + ae2pr$BLACKLIST_Y + 2);

        ae2pr$adopt(cell, white, black);
        int normalColor = this.style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();
        int errorColor = this.style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();
        white.setTextColor(AdvancedFilterCellItem.isValid(white.getValue()) ? normalColor : errorColor);
        black.setTextColor(AdvancedFilterCellItem.isValid(black.getValue()) ? normalColor : errorColor);
    }

    /**
     * 仅当元件 NBT 相对上次观测发生变化时才回填，避免服务端回显滞后覆盖玩家输入。
     */
    @Unique
    private void ae2pr$adopt(ItemStack cell, AETextField white, AETextField black) {
        var whiteValue = AdvancedFilterCellItem.getWhitelist(cell);
        var blackValue = AdvancedFilterCellItem.getBlacklist(cell);

        if (!whiteValue.equals(this.ae2pr$observedWhite)) {
            this.ae2pr$observedWhite = whiteValue;
            if (!white.isFocused() && !white.getValue().equals(whiteValue)) {
                this.ae2pr$sentWhite = whiteValue;
                white.setValue(whiteValue);
            }
        }

        if (!blackValue.equals(this.ae2pr$observedBlack)) {
            this.ae2pr$observedBlack = blackValue;
            if (!black.isFocused() && !black.getValue().equals(blackValue)) {
                this.ae2pr$sentBlack = blackValue;
                black.setValue(blackValue);
            }
        }
    }

    @Unique
    private void ae2pr$onWhitelistChanged(String text) {
        if (text.equals(this.ae2pr$sentWhite)) {
            return;
        }
        this.ae2pr$sentWhite = text;
        ae2pr$send(AdvancedFilterCellItem.ACTION_SET_WHITELIST, text);
    }

    @Unique
    private void ae2pr$onBlacklistChanged(String text) {
        if (text.equals(this.ae2pr$sentBlack)) {
            return;
        }
        this.ae2pr$sentBlack = text;
        ae2pr$send(AdvancedFilterCellItem.ACTION_SET_BLACKLIST, text);
    }

    @Unique
    private void ae2pr$send(String action, String value) {
        if (this.menu instanceof AEBaseMenuInvoker invoker) {
            invoker.ae2pr$sendClientAction(action, value);
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        if (!this.ae2pr$advanced) {
            return;
        }

        int color = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        guiGraphics.drawString(font, Component.translatable("gui.ae2pr.advanced_filter_cell.whitelist"),
                ae2pr$FIELD_X, ae2pr$WHITELIST_Y - 10, color, false);
        guiGraphics.drawString(font, Component.translatable("gui.ae2pr.advanced_filter_cell.blacklist"),
                ae2pr$FIELD_X, ae2pr$BLACKLIST_Y - 10, color, false);

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(ae2pr$FIELD_X, 112, 0);
        pose.scale(1, 1, 1);
        guiGraphics.drawString(font, Component.translatable("gui.ae2pr.advanced_filter_cell.hint"), 0, 0,
                0xFF808080, false);
        pose.popPose();
    }
}
