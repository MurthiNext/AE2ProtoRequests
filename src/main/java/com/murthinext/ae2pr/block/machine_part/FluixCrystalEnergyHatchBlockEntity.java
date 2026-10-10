package com.murthinext.ae2pr.block.machine_part;

import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.IGrid;
import appeng.api.orientation.BlockOrientation;
import appeng.blockentity.grid.AENetworkBlockEntity;

import com.murthinext.ae2pr.ModBlockEntities;

/**
 * 福鲁伊克斯水晶能源仓方块实体：为多方块机器提供 ME 网络电力。
 * <p>
 * 仅仓口面（朝向面）可连接 ME 线缆；不占频道、不耗待机电力，内部不缓存能量；
 * 机器通过 {@link #extractAEPower} 直接向 ME 网络取电，网络离线或电量不足时取不到电。
 */
public class FluixCrystalEnergyHatchBlockEntity extends AENetworkBlockEntity {

    public FluixCrystalEnergyHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUIX_CRYSTAL_ENERGY_HATCH.get(), pos, state);
        getMainNode().setIdlePowerUsage(0);
    }

    /** 仅仓口面（朝向面）允许连接 ME 线缆。 */
    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(getBlockState().getValue(FluixCrystalEnergyHatchBlock.FACING));
    }

    /** 朝向变化后刷新 ME 连接面。 */
    public void updateGridConnectableSides() {
        onGridConnectableSidesChanged();
    }

    /** 是否已接入 ME 网络（有线缆连接）。 */
    public boolean isGridConnected() {
        return getMainNode().getGrid() != null;
    }

    /** 所接入的 ME 网络（未接线为 null）。 */
    public IGrid getConnectedGrid() {
        return getMainNode().getGrid();
    }

    /** 从 ME 网络直接抽取能量（AE 单位，无本地缓存）；simulate 时仅试算。 */
    public double extractAEPower(double amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null || !getMainNode().isActive()) {
            return 0;
        }
        return grid.getEnergyService().extractAEPower(amount,
                simulate ? Actionable.SIMULATE : Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    /** ME 网络当前可用能量（AE 估算值，仅供判断/展示）。 */
    public double getAvailableAEPower() {
        IGrid grid = getMainNode().getGrid();
        return grid != null ? grid.getEnergyService().getStoredPower() : 0;
    }

    /** 多个能源仓的可用网络能量合计（按网络去重，避免同一网络重复计数）。 */
    public static double totalAvailableAEPower(Iterable<FluixCrystalEnergyHatchBlockEntity> hatches) {
        Set<IGrid> grids = Collections.newSetFromMap(new IdentityHashMap<>());
        for (FluixCrystalEnergyHatchBlockEntity hatch : hatches) {
            IGrid grid = hatch.getConnectedGrid();
            if (grid != null) {
                grids.add(grid);
            }
        }
        double total = 0;
        for (IGrid grid : grids) {
            total += grid.getEnergyService().getStoredPower();
        }
        return total;
    }
}
