package com.murthinext.ae2pr.block.level_emitter;

import java.util.function.Predicate;

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

// 多条件发信器：命中任一/全部匹配项即输出红石
public class MultiLevelEmitterPart extends AbstractMultiLevelEmitterPart {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = Ids.of("part/multi_level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = Ids.of("part/multi_level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = Ids.of("part/multi_level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = Ids.of("part/multi_level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = Ids.of(
            "part/multi_level_emitter_status_has_channel");

    private static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    private static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    private static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    private static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    private static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    private static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    private boolean filteredOn;

    public MultiLevelEmitterPart(IPartItem<?> partItem) {
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
        this.lastReportedValue = amount;
    }

    @Override
    protected void openMenu(Player player) {
        MenuOpener.open(ModMenus.MULTI_LEVEL_EMITTER.get(), player, MenuLocators.forPart(this));
    }

    @Override
    protected void refreshValue(IGrid grid) {
        var advanced = getAdvancedFilter();
        if (advanced != null) {
            this.filteredOn = computeAdvancedOn(grid, advanced);
            return;
        }
        var list = getFilterList();
        if (list != null) {
            this.filteredOn = computeFilteredOn(grid, list);
            return;
        }
        this.filteredOn = false;
        this.lastReportedValue = singleKeyAmount(grid);
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

    private boolean computeFilteredOn(IGrid grid, IPartitionList list) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean lowSignal = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;
        long threshold = getReportingValue();

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
        return matchAll;
    }

    private boolean computeAdvancedOn(IGrid grid, Predicate<AEKey> filter) {
        var stacks = grid.getStorageService().getCachedInventory();
        boolean lowSignal = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        boolean matchAll = getConfigManager().getSetting(COMBINE_MODE) == CombineMode.AND;
        long threshold = getReportingValue();

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
        return matchAll;
    }
}
