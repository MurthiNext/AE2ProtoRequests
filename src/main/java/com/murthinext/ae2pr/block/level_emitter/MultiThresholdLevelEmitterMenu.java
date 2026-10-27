package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.implementations.UpgradeableMenu;

import com.murthinext.ae2pr.item.filter_cell.FilterCell;

public class MultiThresholdLevelEmitterMenu extends UpgradeableMenu<MultiThresholdLevelEmitterPart> {

    private static final String ACTION_SET_UPPER_VALUE = "setUpperValue";
    private static final String ACTION_SET_LOWER_VALUE = "setLowerValue";
    private static final String ACTION_SET_COMBINE_MODE = "setCombineMode";

    public static final MenuType<MultiThresholdLevelEmitterMenu> TYPE = MenuTypeBuilder
            .create(MultiThresholdLevelEmitterMenu::new, MultiThresholdLevelEmitterPart.class)
            .withMenuTitle(host -> Component.translatable("gui.ae2pr.multi_threshold_level_emitter"))
            .withInitialData((host, buffer) -> {
                GenericStack.writeBuffer(host.getConfig().getStack(0), buffer);
                buffer.writeVarLong(host.getReportingValue());
                buffer.writeVarLong(host.getLowerThreshold());
            }, (host, menu, buffer) -> {
                menu.getHost().getConfig().setStack(0, GenericStack.readBuffer(buffer));
                menu.currentUpper = buffer.readVarLong();
                menu.currentLower = buffer.readVarLong();
            })
            .build("multi_threshold_level_emitter");

    @GuiSync(7)
    public CombineMode combineMode = CombineMode.OR;

    private long currentUpper;
    private long currentLower;

    public MultiThresholdLevelEmitterMenu(MenuType<MultiThresholdLevelEmitterMenu> menuType, int id, Inventory ip,
            MultiThresholdLevelEmitterPart host) {
        super(menuType, id, ip, host);

        registerClientAction(ACTION_SET_UPPER_VALUE, Long.class, this::setUpperValue);
        registerClientAction(ACTION_SET_LOWER_VALUE, Long.class, this::setLowerValue);
        registerClientAction(ACTION_SET_COMBINE_MODE, CombineMode.class, this::setCombineMode);
    }

    public long getCurrentUpper() {
        return currentUpper;
    }

    public long getCurrentLower() {
        return currentLower;
    }

    public void setUpperValue(long value) {
        if (isClientSide()) {
            if (value != this.currentUpper) {
                this.currentUpper = value;
                sendClientAction(ACTION_SET_UPPER_VALUE, value);
            }
        } else {
            getHost().setReportingValue(value);
        }
    }

    public void setLowerValue(long value) {
        if (isClientSide()) {
            if (value != this.currentLower) {
                this.currentLower = value;
                sendClientAction(ACTION_SET_LOWER_VALUE, value);
            }
        } else {
            getHost().setLowerThreshold(value);
        }
    }

    public void setCombineMode(CombineMode mode) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COMBINE_MODE, mode);
        } else {
            getHost().getConfigManager().putSetting(MultiThresholdLevelEmitterPart.COMBINE_MODE, mode);
        }
    }

    @Override
    protected void setupConfig() {
        addSlot(new EmitterConfigSlot(getHost().getConfig().createMenuWrapper(),
                getHost().getFilterCellInventory()), SlotSemantics.CONFIG);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int idx) {
        if (!isClientSide() && idx >= 0 && idx < slots.size()) {
            var source = slots.get(idx);

            // 从配置槽取出过滤元件
            if (source instanceof EmitterConfigSlot emitterSlot) {
                var cell = emitterSlot.getFilterCellStack();
                if (cell != null) {
                    var toMove = cell.copy();
                    if (moveItemStackTo(toMove, ae2pr$playerSlotStart(), ae2pr$playerSlotEnd(), false)) {
                        emitterSlot.clearFilterCell();
                        broadcastChanges();
                    }
                    return ItemStack.EMPTY;
                }
            }

            // 从玩家背包放入过滤元件
            if (source.container instanceof Inventory
                    && source.getItem().getItem() instanceof FilterCell) {
                for (var configSlot : getSlots(SlotSemantics.CONFIG)) {
                    if (configSlot instanceof EmitterConfigSlot emitterSlot
                            && emitterSlot.insertFromShiftClick(source)) {
                        broadcastChanges();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return super.quickMoveStack(player, idx);
    }

    private int ae2pr$playerSlotStart() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).container instanceof Inventory) {
                return i;
            }
        }
        return slots.size();
    }

    private int ae2pr$playerSlotEnd() {
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i).container instanceof Inventory) {
                return i + 1;
            }
        }
        return slots.size();
    }

    @Override
    protected void loadSettingsFromHost(IConfigManager cm) {
        this.setCraftingMode(cm.getSetting(Settings.CRAFT_VIA_REDSTONE));
        if (cm.hasSetting(Settings.FUZZY_MODE)) {
            this.setFuzzyMode(cm.getSetting(Settings.FUZZY_MODE));
        }
        this.setRedStoneMode(cm.getSetting(Settings.REDSTONE_EMITTER));
        this.combineMode = cm.getSetting(MultiThresholdLevelEmitterPart.COMBINE_MODE);
    }

    public boolean supportsFuzzySearch() {
        return getHost().getConfigManager().hasSetting(Settings.FUZZY_MODE) && hasUpgrade(AEItems.FUZZY_CARD);
    }

    public AEKey getConfiguredFilter() {
        return getHost().getConfig().getKey(0);
    }
}
