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

/**
 * ME 通式标准发信器的菜单：配置槽与过滤元件共用。
 */
public class MultiLevelEmitterMenu extends UpgradeableMenu<MultiLevelEmitterPart> {

    private static final String ACTION_SET_REPORTING_VALUE = "setReportingValue";
    private static final String ACTION_SET_COMBINE_MODE = "setCombineMode";

    public static final MenuType<MultiLevelEmitterMenu> TYPE = MenuTypeBuilder
            .create(MultiLevelEmitterMenu::new, MultiLevelEmitterPart.class)
            .withMenuTitle(host -> Component.translatable("gui.ae2pr.multi_level_emitter"))
            .withInitialData((host, buffer) -> {
                GenericStack.writeBuffer(host.getConfig().getStack(0), buffer);
                buffer.writeVarLong(host.getReportingValue());
            }, (host, menu, buffer) -> {
                menu.getHost().getConfig().setStack(0, GenericStack.readBuffer(buffer));
                menu.currentValue = buffer.readVarLong();
            })
            .build("multi_level_emitter");

    @GuiSync(7)
    public CombineMode combineMode = CombineMode.OR;

    private long currentValue;

    public MultiLevelEmitterMenu(MenuType<MultiLevelEmitterMenu> menuType, int id, Inventory ip,
            MultiLevelEmitterPart host) {
        super(menuType, id, ip, host);

        registerClientAction(ACTION_SET_REPORTING_VALUE, Long.class, this::setValue);
        registerClientAction(ACTION_SET_COMBINE_MODE, CombineMode.class, this::setCombineMode);
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setValue(long initialValue) {
        if (isClientSide()) {
            if (initialValue != this.currentValue) {
                this.currentValue = initialValue;
                sendClientAction(ACTION_SET_REPORTING_VALUE, initialValue);
            }
        } else {
            getHost().setReportingValue(initialValue);
        }
    }

    public void setCombineMode(CombineMode mode) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COMBINE_MODE, mode);
        } else {
            getHost().getConfigManager().putSetting(MultiLevelEmitterPart.COMBINE_MODE, mode);
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
        this.combineMode = cm.getSetting(MultiLevelEmitterPart.COMBINE_MODE);
    }

    public boolean supportsFuzzySearch() {
        return getHost().getConfigManager().hasSetting(Settings.FUZZY_MODE) && hasUpgrade(AEItems.FUZZY_CARD);
    }

    public AEKey getConfiguredFilter() {
        return getHost().getConfig().getKey(0);
    }
}
