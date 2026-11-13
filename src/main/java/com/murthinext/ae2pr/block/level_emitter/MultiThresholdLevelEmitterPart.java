package com.murthinext.ae2pr.block.level_emitter;

import java.util.function.Predicate;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.util.prioritylist.IPartitionList;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.registry.ModMenus;

// 多阈值发信器：上下限锁存，达到上限置位、低于下限复位
public class MultiThresholdLevelEmitterPart extends AbstractMultiLevelEmitterPart {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = Ids.of("part/multi_threshold_level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = Ids.of("part/multi_threshold_level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = Ids.of("part/multi_threshold_level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = Ids.of("part/multi_threshold_level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = Ids.of(
            "part/multi_threshold_level_emitter_status_has_channel");

    private static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    private static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    private static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    private static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    private static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    private static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    private long currentValue;
    private long lowerThreshold;
    private boolean latchOn;

    public MultiThresholdLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    protected IPartModel modelFor(boolean on, boolean powered, boolean connected) {
        if (connected) {
            return on ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        }
        if (powered) {
            return on ? MODEL_ON_ON : MODEL_OFF_ON;
        }
        return on ? MODEL_ON_OFF : MODEL_OFF_OFF;
    }

    @Override
    protected void applySingleKeyAmount(long amount) {
        this.currentValue = amount;
    }

    @Override
    protected void openMenu(Player player) {
        MenuOpener.open(ModMenus.MULTI_THRESHOLD_LEVEL_EMITTER.get(), player, MenuLocators.forPart(this));
    }

    @Override
    protected void refreshValue(IGrid grid) {
        var advanced = getAdvancedFilter();
        if (advanced != null) {
            this.currentValue = computeAdvancedValue(grid, advanced);
            return;
        }
        var list = getFilterList();
        if (list != null) {
            this.currentValue = computeCombinedValue(grid, list);
            return;
        }
        this.currentValue = singleKeyAmount(grid);
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

        // 复位-置位锁存：达到上限置位，低于下限复位
        if (this.currentValue >= getReportingValue()) {
            this.latchOn = true;
        } else if (this.currentValue < this.lowerThreshold) {
            this.latchOn = false;
        }
        // 红石模式：LOW_SIGNAL 时反转输出
        boolean invert = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        return invert != this.latchOn;
    }

    public long getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(long value) {
        this.lowerThreshold = value;
        getMainNode().ifPresent(this::updateReportingValue);
    }

    @Override
    protected void readExtraNbt(CompoundTag data) {
        this.lowerThreshold = data.getLong("lowerThreshold");
        this.latchOn = data.getBoolean("latchOn");
    }

    @Override
    protected void writeExtraNbt(CompoundTag data) {
        data.putLong("lowerThreshold", this.lowerThreshold);
        data.putBoolean("latchOn", this.latchOn);
    }

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
}
