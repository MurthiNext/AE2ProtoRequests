package com.murthinext.ae2pr.block.level_emitter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
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
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.prioritylist.IPartitionList;

import com.murthinext.ae2pr.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.item.filter_cell.AdvancedFilterCellItem;
import com.murthinext.ae2pr.item.filter_cell.FilterCell;
import com.murthinext.ae2pr.item.filter_cell.FilterCellItem;

/**
 * ME 通式标准发信器：支持过滤元件配置多触发项。
 */
public class MultiLevelEmitterPart extends AbstractLevelEmitterPart
        implements IConfigInvHost, ICraftingProvider, InternalInventoryHost {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = makeId("part/multi_level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = makeId("part/multi_level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = makeId("part/multi_level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = makeId("part/multi_level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = makeId(
            "part/multi_level_emitter_status_has_channel");

    public static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    public static final Setting<CombineMode> COMBINE_MODE = new Setting<>("combine_mode", CombineMode.class);

    private static final int FILTER_SLOT = 0;

    private final ConfigInventory config = ConfigInventory.configTypes(1, this::configureWatchers);
    private final AppEngInternalInventory filterCell = new AppEngInternalInventory(this, 1, 1);

    private IStackWatcher storageWatcher;
    private IStackWatcher craftingWatcher;
    private long lastUpdateTick = -1;
    private boolean filteredOn;

    private final IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
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
                lastReportedValue = amount;
                updateState();
            } else {
                throttleUpdate();
            }
        }
    };

    private final ICraftingWatcherNode craftingWatcherNode = new ICraftingWatcherNode() {
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

    public MultiLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);

        getMainNode().addService(IStorageWatcherNode.class, stackWatcherNode);
        getMainNode().addService(ICraftingWatcherNode.class, craftingWatcherNode);
        getMainNode().addService(ICraftingProvider.class, this);

        getConfigManager().registerSetting(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        getConfigManager().registerSetting(COMBINE_MODE, CombineMode.OR);

        filterCell.setFilter(new FilterCellItemFilter());
    }

    private static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path);
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
    private AEKey getConfiguredKey() {
        return config.getKey(0);
    }

    @Nullable
    public ItemStack getFilterCellStack() {
        var stack = filterCell.getStackInSlot(FILTER_SLOT);
        return stack == null || stack.isEmpty() ? null : stack;
    }

    @Nullable
    private IPartitionList getFilterList() {
        var stack = getFilterCellStack();
        if (stack == null || !(stack.getItem() instanceof FilterCellItem)) {
            return null;
        }
        var list = FilterCellItem.createFilter(stack);
        return list.isEmpty() ? null : list;
    }

    @Nullable
    private Predicate<AEKey> getAdvancedFilter() {
        var stack = getFilterCellStack();
        if (stack == null || !(stack.getItem() instanceof AdvancedFilterCellItem)) {
            return null;
        }
        return AdvancedFilterCellItem.createPredicate(stack);
    }

    private boolean usesFilterCell() {
        return getFilterList() != null || getAdvancedFilter() != null;
    }

    private boolean isFilterFuzzy() {
        var stack = getFilterCellStack();
        return stack != null && stack.getItem() instanceof FilterCellItem item
                && item.getUpgrades(stack).isInstalled(AEItems.FUZZY_CARD);
    }

    private long amountFor(KeyCounter stacks, AEKey key) {
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

    private void throttleUpdate() {
        long currentTick = TickHandler.instance().getCurrentTick();
        if (currentTick != lastUpdateTick) {
            lastUpdateTick = currentTick;
            getMainNode().ifPresent(this::updateReportingValue);
        }
    }

    private void updateReportingValue(IGrid grid) {
        var advanced = getAdvancedFilter();
        if (advanced != null) {
            this.filteredOn = computeAdvancedOn(grid, advanced);
        } else {
            var list = getFilterList();
            if (list != null) {
                this.filteredOn = computeFilteredOn(grid, list);
            } else {
                this.filteredOn = false;
                updateSingleKeyValue(grid);
            }
        }
        this.updateState();
    }

    private void updateSingleKeyValue(IGrid grid) {
        var stacks = grid.getStorageService().getCachedInventory();
        var myStack = getConfiguredKey();

        if (myStack == null) {
            long total = 0;
            for (var entry : stacks) {
                total += entry.getLongValue();
            }
            this.lastReportedValue = total;
        } else if (isUpgradedWith(AEItems.FUZZY_CARD)) {
            long total = 0;
            var fuzzyMode = getConfigManager().getSetting(Settings.FUZZY_MODE);
            for (var entry : stacks.findFuzzy(myStack, fuzzyMode)) {
                total += entry.getLongValue();
            }
            this.lastReportedValue = total;
        } else {
            this.lastReportedValue = stacks.get(myStack);
        }
    }

    /**
     * 逐项判定并按 AND/OR 组合。
     */
    private boolean computeFilteredOn(IGrid grid, IPartitionList list) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean lowSignal = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;
        long threshold = getReportingValue();

        boolean result = matchAll;
        for (AEKey key : list.getItems()) {
            long amount = amountFor(stacks, key);
            boolean satisfied = lowSignal == (amount < threshold);
            if (matchAll) {
                if (!satisfied) {
                    return false;
                }
            } else if (satisfied) {
                return true;
            }
        }
        return result;
    }

    /**
     * 高级过滤元件：按标签表达式筛选后逐项判定。
     */
    private boolean computeAdvancedOn(IGrid grid, Predicate<AEKey> filter) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean lowSignal = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;
        long threshold = getReportingValue();

        boolean result = matchAll;
        for (var entry : stacks) {
            if (!filter.test(entry.getKey())) {
                continue;
            }
            long amount = entry.getLongValue();
            boolean satisfied = lowSignal == (amount < threshold);
            if (matchAll) {
                if (!satisfied) {
                    return false;
                }
            } else if (satisfied) {
                return true;
            }
        }
        return result;
    }

    @Override
    protected boolean isLevelEmitterOn() {
        if (isClientSide()) {
            return super.isLevelEmitterOn();
        }
        if (!getMainNode().isActive()) {
            return false;
        }
        if (hasDirectOutput()) {
            return getDirectOutput();
        }
        if (usesFilterCell()) {
            return this.filteredOn;
        }
        return super.isLevelEmitterOn();
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
        if (isUpgradedWith(AEItems.CRAFTING_CARD)
                && getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE) == YesNo.YES) {
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
            if (key != null) {
                return Set.of(key);
            }
        }
        return Set.of();
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
            MenuOpener.open(ModMenus.MULTI_LEVEL_EMITTER.get(), player, MenuLocators.forPart(this));
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
        if (this.isActive() && this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_ON : MODEL_OFF_ON;
        } else {
            return this.isLevelEmitterOn() ? MODEL_ON_OFF : MODEL_OFF_OFF;
        }
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        config.readFromChildTag(data, "config");
        filterCell.readFromNBT(data, "filterCell");
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        config.writeToChildTag(data, "config");
        filterCell.writeToNBT(data, "filterCell");
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

    /**
     * 过滤槽仅接受过滤元件。
     */
    private static class FilterCellItemFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterCell;
        }
    }
}
