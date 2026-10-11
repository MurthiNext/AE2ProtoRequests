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
 * ME 通式阈值发信器：类似 ExtendedAE 的阈值发信器，是一个"复位-置位"锁存器。
 * <p>
 * 当监控数量达到<b>上限</b>时开启红石信号，低于<b>下限</b>时关闭；介于两者之间保持原状态。
 * <p>
 * 与通式标准发信器相同，配置槽可与过滤元件共用：插入过滤元件后以其配置的键为触发项。
 */
public class MultiThresholdLevelEmitterPart extends AbstractLevelEmitterPart
        implements IConfigInvHost, ICraftingProvider, InternalInventoryHost {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = makeId("part/multi_threshold_level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = makeId("part/multi_threshold_level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = makeId("part/multi_threshold_level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = makeId("part/multi_threshold_level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = makeId(
            "part/multi_threshold_level_emitter_status_has_channel");

    public static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    /** 多触发项组合模式（仅在使用过滤元件时生效）。 */
    public static final Setting<CombineMode> COMBINE_MODE = new Setting<>("combine_mode", CombineMode.class);

    private static final int FILTER_SLOT = 0;

    private final ConfigInventory config = ConfigInventory.configTypes(1, this::configureWatchers);
    private final AppEngInternalInventory filterCell = new AppEngInternalInventory(this, 1, 1);

    private IStackWatcher storageWatcher;
    private IStackWatcher craftingWatcher;
    private long lastUpdateTick = -1;
    /** 当前监控到的数量（单配置项为数量/总和；过滤元件为逐项组合后的值）。 */
    private long currentValue;
    /** 下限阈值；上限使用 {@link #getReportingValue()}。 */
    private long lowerThreshold;
    /** 锁存器内部状态（不受红石模式反转影响，用于在上下限之间保持）。 */
    private boolean latchOn;

    private final IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            storageWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onStackChange(AEKey what, long amount) {
            if (usesFilterCell()) {
                throttleUpdate();
            } else if (what.equals(getConfiguredKey()) && !isUpgradedWith(AEItems.FUZZY_CARD)) {
                currentValue = amount;
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

    public MultiThresholdLevelEmitterPart(IPartItem<?> partItem) {
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

    /**
     * 高级过滤元件构建出的标签匹配谓词；未插入或未配置任何表达式时返回 null。
     */
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
            this.currentValue = computeAdvancedValue(grid, advanced);
        } else {
            var list = getFilterList();
            if (list != null) {
                this.currentValue = computeCombinedValue(grid, list);
            } else {
                this.currentValue = computeSingleValue(grid);
            }
        }
        this.updateState();
    }

    /** 未使用过滤元件时的取值逻辑（与标准发信器一致）。 */
    private long computeSingleValue(IGrid grid) {
        var stacks = grid.getStorageService().getCachedInventory();
        var myStack = getConfiguredKey();

        if (myStack == null) {
            long total = 0;
            for (var entry : stacks) {
                total += entry.getLongValue();
            }
            return total;
        } else if (isUpgradedWith(AEItems.FUZZY_CARD)) {
            long total = 0;
            var fuzzyMode = getConfigManager().getSetting(Settings.FUZZY_MODE);
            for (var entry : stacks.findFuzzy(myStack, fuzzyMode)) {
                total += entry.getLongValue();
            }
            return total;
        } else {
            return stacks.get(myStack);
        }
    }

    /** 逐项取值并按 AND/OR 组合：AND 取最小值、OR 取最大值。 */
    private long computeCombinedValue(IGrid grid, IPartitionList list) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;

        boolean first = true;
        long result = 0;
        for (AEKey key : list.getItems()) {
            long amount = amountFor(stacks, key);
            if (first) {
                result = amount;
                first = false;
            } else if (matchAll) {
                result = Math.min(result, amount);
            } else {
                result = Math.max(result, amount);
            }
        }
        return result;
    }

    /** 高级过滤元件：遍历网络中通过标签表达式的物品种类，按 AND/OR 取最小值/最大值。 */
    private long computeAdvancedValue(IGrid grid, Predicate<AEKey> filter) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;

        boolean first = true;
        long result = 0;
        for (var entry : stacks) {
            if (!filter.test(entry.getKey())) {
                continue;
            }
            long amount = entry.getLongValue();
            if (first) {
                result = amount;
                first = false;
            } else if (matchAll) {
                result = Math.min(result, amount);
            } else {
                result = Math.max(result, amount);
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

        // 复位-置位锁存：达到上限置位，低于下限复位，介于两者之间保持内部状态
        if (this.currentValue >= getReportingValue()) {
            this.latchOn = true;
        } else if (this.currentValue < this.lowerThreshold) {
            this.latchOn = false;
        }
        // 红石模式：LOW_SIGNAL 时反转输出（默认 HIGH_SIGNAL 不反转）
        boolean invert = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        return invert != this.latchOn;
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

    public long getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(long value) {
        this.lowerThreshold = value;
        getMainNode().ifPresent(this::updateReportingValue);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(ModMenus.MULTI_THRESHOLD_LEVEL_EMITTER.get(), player, MenuLocators.forPart(this));
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
        this.lowerThreshold = data.getLong("lowerThreshold");
        this.latchOn = data.getBoolean("latchOn");
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        config.writeToChildTag(data, "config");
        filterCell.writeToNBT(data, "filterCell");
        data.putLong("lowerThreshold", this.lowerThreshold);
        data.putBoolean("latchOn", this.latchOn);
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

    /** 过滤槽仅接受过滤元件。 */
    private static class FilterCellItemFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem() instanceof FilterCell;
        }
    }
}
