package com.murthinext.ae2pr.block.level_emitter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Setting;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingWatcherNode;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.definitions.AEItems;
import appeng.helpers.IConfigInvHost;
import appeng.hooks.ticking.TickHandler;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.prioritylist.IPartitionList;

import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;
import com.murthinext.ae2pr.item.filter_cell.FilterCell;
import com.murthinext.ae2pr.item.filter_cell.FilterCellItem;

// 多条件发信器的公共部分：过滤元件、仓储/合成监听、模型与持久化
public abstract class AbstractMultiLevelEmitterPart extends AbstractLevelEmitterPart
        implements IConfigInvHost, ICraftingProvider, InternalInventoryHost {

    public static final Setting<CombineMode> COMBINE_MODE = new Setting<>("combine_mode", CombineMode.class);

    private static final int FILTER_SLOT = 0;

    private final ConfigInventory config = ConfigInventory.configTypes(1, this::configureWatchers);
    private final AppEngInternalInventory filterCell = new AppEngInternalInventory(this, 1, 1);

    private IStackWatcher storageWatcher;
    private IStackWatcher craftingWatcher;
    private long lastUpdateTick = -1;

    protected AbstractMultiLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);

        // 过滤元件模式下统一节流重算
        IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
            @Override
            public void updateWatcher(IStackWatcher newWatcher) {
                storageWatcher = newWatcher;
                configureWatchers();
            }

            @Override
            public void onStackChange(AEKey what, long amount) {
                if (usesFilterCell()) {
                    // 过滤元件模式下统一节流重算
                    throttleUpdate();
                } else if (what.equals(getConfiguredKey()) && !isUpgradedWith(AEItems.FUZZY_CARD)) {
                    applySingleKeyAmount(amount);
                    updateState();
                } else {
                    throttleUpdate();
                }
            }
        };
        getMainNode().addService(IStorageWatcherNode.class, stackWatcherNode);
        ICraftingWatcherNode craftingWatcherNode = new ICraftingWatcherNode() {
            @Override
            public void updateWatcher(IStackWatcher newWatcher) {
                craftingWatcher = newWatcher;
                configureWatchers();
            }

            @Override
            public void onRequestChange(AEKey what) {
                updateState();
            }

            @Override
            public void onCraftableChange(AEKey what) {
            }
        };
        getMainNode().addService(ICraftingWatcherNode.class, craftingWatcherNode);
        getMainNode().addService(ICraftingProvider.class, this);

        getConfigManager().registerSetting(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        getConfigManager().registerSetting(COMBINE_MODE, CombineMode.OR);

        filterCell.setFilter(new FilterCellItemFilter());
    }

    // 未接通、未通电、已接通三种状态下的开/关模型
    protected abstract IPartModel modelFor(boolean on, boolean powered, boolean connected);

    // 按当前过滤条件刷新存量统计
    protected abstract void refreshValue(IGrid grid);

    // 单键快速路径下拿到的存量
    protected abstract void applySingleKeyAmount(long amount);

    protected abstract void openMenu(Player player);

    protected void readExtraNbt(CompoundTag data) {
    }

    protected void writeExtraNbt(CompoundTag data) {
    }

    @Override
    protected int getUpgradeSlots() {
        // 模糊卡 + 合成卡
        return 2;
    }

    @Override
    public void upgradesChanged() {
        this.configureWatchers();
    }

    @Nullable
    protected final AEKey getConfiguredKey() {
        return config.getKey(0);
    }

    @Nullable
    public final ItemStack getFilterCellStack() {
        var stack = filterCell.getStackInSlot(FILTER_SLOT);
        return stack == null || stack.isEmpty() ? null : stack;
    }

    @Nullable
    protected final IPartitionList getFilterList() {
        var stack = getFilterCellStack();
        if (stack == null || !(stack.getItem() instanceof FilterCellItem)) {
            return null;
        }
        var list = FilterCellItem.createFilter(stack);
        return list.isEmpty() ? null : list;
    }

    @Nullable
    protected final Predicate<AEKey> getAdvancedFilter() {
        var stack = getFilterCellStack();
        if (stack == null || !(stack.getItem() instanceof AdvancedFilterCellItem)) {
            return null;
        }
        return AdvancedFilterCellItem.createPredicate(stack);
    }

    protected final boolean usesFilterCell() {
        return getFilterList() != null || getAdvancedFilter() != null;
    }

    protected final boolean isFilterFuzzy() {
        var stack = getFilterCellStack();
        return stack != null && stack.getItem() instanceof FilterCellItem item
                && item.getUpgrades(stack).isInstalled(AEItems.FUZZY_CARD);
    }

    protected final long amountFor(KeyCounter stacks, AEKey key) {
        if (isFilterFuzzy()) {
            var stack = getFilterCellStack();
            var fuzzyMode = ((FilterCellItem) stack.getItem()).getFuzzyMode(stack);
            long total = 0;
            for (var entry : stacks.findFuzzy(key, fuzzyMode)) {
                total += entry.getLongValue();
            }
            return total;
        }
        return stacks.get(key);
    }

    // 单键模式下的存量：未配置取全部，装了模糊卡按模糊匹配
    protected final long singleKeyAmount(IGrid grid) {
        var stacks = grid.getStorageService().getCachedInventory();
        var key = getConfiguredKey();
        if (key == null) {
            long total = 0;
            for (var entry : stacks) {
                total += entry.getLongValue();
            }
            return total;
        }
        if (isUpgradedWith(AEItems.FUZZY_CARD)) {
            long total = 0;
            var fuzzyMode = getConfigManager().getSetting(Settings.FUZZY_MODE);
            for (var entry : stacks.findFuzzy(key, fuzzyMode)) {
                total += entry.getLongValue();
            }
            return total;
        }
        return stacks.get(key);
    }

    private void throttleUpdate() {
        long currentTick = TickHandler.instance().getCurrentTick();
        if (currentTick != lastUpdateTick) {
            lastUpdateTick = currentTick;
            getMainNode().ifPresent(this::updateReportingValue);
        }
    }

    protected void updateReportingValue(IGrid grid) {
        refreshValue(grid);
        updateState();
    }

    @Override
    protected boolean hasDirectOutput() {
        return isUpgradedWith(AEItems.CRAFTING_CARD);
    }

    @Override
    protected boolean getDirectOutput() {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }

        var advanced = getAdvancedFilter();
        if (advanced != null) {
            for (var entry : grid.getStorageService().getCachedInventory()) {
                if (advanced.test(entry.getKey()) && grid.getCraftingService().isRequesting(entry.getKey())) {
                    return true;
                }
            }
            return false;
        }

        var list = getFilterList();
        if (list != null) {
            for (AEKey key : list.getItems()) {
                if (grid.getCraftingService().isRequesting(key)) {
                    return true;
                }
            }
            return false;
        }

        var key = getConfiguredKey();
        if (key != null) {
            return grid.getCraftingService().isRequesting(key);
        }
        return grid.getCraftingService().isRequestingAny();
    }

    @Override
    public Set<AEKey> getEmitableItems() {
        if (!isUpgradedWith(AEItems.CRAFTING_CARD)
                || getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE) != YesNo.YES) {
            return Set.of();
        }
        var advanced = getAdvancedFilter();
        if (advanced != null) {
            var grid = getMainNode().getGrid();
            if (grid == null) {
                return Set.of();
            }
            var keys = new HashSet<AEKey>();
            for (var entry : grid.getStorageService().getCachedInventory()) {
                if (advanced.test(entry.getKey())) {
                    keys.add(entry.getKey());
                }
            }
            return keys;
        }
        var list = getFilterList();
        if (list != null) {
            var keys = new HashSet<AEKey>();
            for (AEKey key : list.getItems()) {
                keys.add(key);
            }
            return keys;
        }
        var key = getConfiguredKey();
        return key != null ? Set.of(key) : Set.of();
    }

    @Override
    protected void onReportingValueChanged() {
        getMainNode().ifPresent(this::updateReportingValue);
    }

    @Override
    protected void configureWatchers() {
        var list = getFilterList();
        var advanced = getAdvancedFilter();

        if (this.storageWatcher != null) {
            this.storageWatcher.reset();
        }
        if (this.craftingWatcher != null) {
            this.craftingWatcher.reset();
        }

        ICraftingProvider.requestUpdate(getMainNode());

        if (isUpgradedWith(AEItems.CRAFTING_CARD)) {
            if (this.craftingWatcher != null) {
                if (advanced != null) {
                    this.craftingWatcher.setWatchAll(true);
                } else if (list != null) {
                    for (AEKey key : list.getItems()) {
                        this.craftingWatcher.add(key);
                    }
                } else if (getConfiguredKey() == null) {
                    this.craftingWatcher.setWatchAll(true);
                } else {
                    this.craftingWatcher.add(getConfiguredKey());
                }
            }
        } else {
            if (this.storageWatcher != null) {
                if (advanced != null) {
                    this.storageWatcher.setWatchAll(true);
                } else if (list != null) {
                    if (isFilterFuzzy()) {
                        this.storageWatcher.setWatchAll(true);
                    } else {
                        for (AEKey key : list.getItems()) {
                            this.storageWatcher.add(key);
                        }
                    }
                } else if (isUpgradedWith(AEItems.FUZZY_CARD) || getConfiguredKey() == null) {
                    this.storageWatcher.setWatchAll(true);
                } else {
                    this.storageWatcher.add(getConfiguredKey());
                }
            }

            getMainNode().ifPresent(this::updateReportingValue);
        }

        updateState();
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            openMenu(player);
        }
        return true;
    }

    @Override
    public ConfigInventory getConfig() {
        return config;
    }

    public InternalInventory getFilterCellInventory() {
        return filterCell;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return List.of();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return false;
    }

    @Override
    public boolean isBusy() {
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return modelFor(this.isLevelEmitterOn(), this.isPowered(), this.isActive() && this.isPowered());
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        config.readFromChildTag(data, "config");
        filterCell.readFromNBT(data, "filterCell");
        readExtraNbt(data);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        config.writeToChildTag(data, "config");
        filterCell.writeToNBT(data, "filterCell");
        writeExtraNbt(data);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        var stack = filterCell.getStackInSlot(FILTER_SLOT);
        if (stack != null && !stack.isEmpty()) {
            drops.add(stack.copy());
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        filterCell.clear();
    }

    @Override
    public void saveChanges() {
        getHost().markForSave();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        getHost().markForSave();
        configureWatchers();
    }

    // 过滤槽仅接受过滤元件
    private static class FilterCellItemFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterCell;
        }
    }
}
