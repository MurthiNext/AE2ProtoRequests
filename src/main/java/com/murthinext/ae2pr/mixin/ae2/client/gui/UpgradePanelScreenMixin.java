package com.murthinext.ae2pr.mixin.ae2.client.gui;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;
import com.murthinext.ae2pr.mixin.ae2.accessor.WidgetContainerAccessor;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.Upgrades;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.WidgetContainer;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.ItemLike;

// 所有受管机器界面：样式已声明 upgrades 锚点时补出升级面板，已有同名控件则复用
@Mixin(value = AEBaseScreen.class, priority = 500, remap = false)
public abstract class UpgradePanelScreenMixin {

    @Final
    @Shadow(remap = false)
    protected WidgetContainer widgets;

    @Final
    @Shadow(remap = false)
    protected ScreenStyle style;

    @Inject(method = "init()V", at = @At("HEAD"), remap = true)
    private void ae2pr$addUpgradesPanel(CallbackInfo ci) {
        // init 在窗口尺寸变化时会重复执行，同名控件存在即复用，第三方已建的也保留
        if (ae2pr$hasWidget("upgrades")) {
            return;
        }
        var menu = ((AEBaseScreen<?>) (Object) this).getMenu();
        var slots = menu.getSlots(SlotSemantics.UPGRADE);
        if (slots.isEmpty()) {
            return;
        }
        IUpgradeInventory upgrades = ae2pr$upgrades(slots);
        if (upgrades == null || !SuperStackSupport.isSupportedMachine(upgrades.getUpgradableItem())) {
            return;
        }
        // 附属界面可能没有声明 upgrades 锚点，此时保持原样
        try {
            this.style.getWidget("upgrades");
        } catch (IllegalStateException missingAnchor) {
            return;
        }
        ItemLike machine = upgrades.getUpgradableItem();
        this.widgets.add("upgrades", new UpgradesPanel(slots, () -> ae2pr$tooltip(machine)));
    }

    @Unique
    private static IUpgradeInventory ae2pr$upgrades(List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot
                    && appEngSlot.getInventory() instanceof IUpgradeInventory inventory) {
                return inventory;
            }
        }
        return null;
    }

    @Unique
    private boolean ae2pr$hasWidget(String id) {
        var accessor = (WidgetContainerAccessor) this.widgets;
        return accessor.ae2pr$getWidgets().containsKey(id) || accessor.ae2pr$getCompositeWidgets().containsKey(id);
    }

    @Unique
    private static List<Component> ae2pr$tooltip(ItemLike machine) {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        lines.addAll(Upgrades.getTooltipLinesForMachine(machine));
        return lines;
    }
}
