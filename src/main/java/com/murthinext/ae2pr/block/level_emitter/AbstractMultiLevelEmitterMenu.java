package com.murthinext.ae2pr.block.level_emitter;

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
import appeng.menu.implementations.UpgradeableMenu;

import com.murthinext.ae2pr.item.filter_cell.FilterCell;

// 多条件发信器菜单的公共部分：过滤元件槽、快捷移动与设置同步
public abstract class AbstractMultiLevelEmitterMenu<T extends AbstractMultiLevelEmitterPart>
        extends UpgradeableMenu<T> {

    protected static final String ACTION_SET_COMBINE_MODE = "setCombineMode";

    @GuiSync(7)
    public CombineMode combineMode = CombineMode.OR;

    protected AbstractMultiLevelEmitterMenu(MenuType<?> menuType, int id, Inventory playerInventory, T host) {
        super(menuType, id, playerInventory, host);
        registerClientAction(ACTION_SET_COMBINE_MODE, CombineMode.class, this::setCombineMode);
    }

    // 客户端侧首次同步到菜单的取值
    protected void loadValues(GenericStack filter, long... values) {
        getHost().getConfig().setStack(0, filter);
    }

    public void setCombineMode(CombineMode mode) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_COMBINE_MODE, mode);
        } else {
            getHost().getConfigManager().putSetting(AbstractMultiLevelEmitterPart.COMBINE_MODE, mode);
        }
    }

    public AEKey getConfiguredFilter() {
        return getHost().getConfig().getKey(0);
    }

    public boolean supportsFuzzySearch() {
        return getHost().getConfigManager().hasSetting(Settings.FUZZY_MODE) && hasUpgrade(AEItems.FUZZY_CARD);
    }

    @Override
    protected void setupConfig() {
        addSlot(new EmitterConfigSlot(getHost().getConfig().createMenuWrapper(), getHost().getFilterCellInventory()),
                SlotSemantics.CONFIG);
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
                    if (moveItemStackTo(toMove, playerSlotStart(), playerSlotEnd(), false)) {
                        emitterSlot.clearFilterCell();
                        broadcastChanges();
                    }
                    return ItemStack.EMPTY;
                }
            }

            // 从玩家背包放入过滤元件
            if (source.container instanceof Inventory && source.getItem().getItem() instanceof FilterCell) {
                for (var configSlot : getSlots(SlotSemantics.CONFIG)) {
                    if (configSlot instanceof EmitterConfigSlot emitterSlot &&
                        emitterSlot.insertFromShiftClick(source)) {
                        broadcastChanges();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return super.quickMoveStack(player, idx);
    }

    private int playerSlotStart() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).container instanceof Inventory) {
                return i;
            }
        }
        return slots.size();
    }

    private int playerSlotEnd() {
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
        this.combineMode = cm.getSetting(AbstractMultiLevelEmitterPart.COMBINE_MODE);
    }
}
