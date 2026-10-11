package com.murthinext.ae2pr.item.filter_cell;

import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEKey;
import appeng.api.storage.cells.ICellWorkbenchItem;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.world.item.ItemStack;

/**
 * 过滤元件：在元件工作台中配置过滤项，并对外提供静态过滤 API。
 */
public class FilterCellItem extends AEBaseItem implements ICellWorkbenchItem, FilterCell {

    public static final int CONFIG_SLOTS = 63;

    private static final int UPGRADE_SLOTS = 1;

    private static final String FUZZY_MODE_TAG = "FuzzyMode";

    public FilterCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack is) {
        return UpgradeInventories.forItem(is, UPGRADE_SLOTS);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack is) {
        // filter 为 null：不限键类型
        return CellConfig.create(null, is, CONFIG_SLOTS);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack is) {
        var tag = is.getTag();
        var name = tag != null ? tag.getString(FUZZY_MODE_TAG) : "";
        try {
            return FuzzyMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return FuzzyMode.IGNORE_ALL;
        }
    }

    @Override
    public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
        is.getOrCreateTag().putString(FUZZY_MODE_TAG, fzMode.name());
    }

    /**
     * 依据元件配置构建过滤表；未配置过滤项时白名单恒通过。
     */
    public static IPartitionList createFilter(ItemStack cell) {
        var builder = IPartitionList.builder();
        if (cell.getItem() instanceof FilterCellItem item) {
            if (item.getUpgrades(cell).isInstalled(AEItems.FUZZY_CARD)) {
                builder.fuzzyMode(item.getFuzzyMode(cell));
            }
            builder.addAll(item.getConfigInventory(cell).keySet());
        }
        return builder.build();
    }

    /**
     * 判断 AE 键是否通过过滤元件（白名单语义；未配置过滤项时恒通过）。
     */
    public static boolean matches(ItemStack cell, AEKey key) {
        if (key == null) {
            return false;
        }
        return createFilter(cell).matchesFilter(key, IncludeExclude.WHITELIST);
    }
}
