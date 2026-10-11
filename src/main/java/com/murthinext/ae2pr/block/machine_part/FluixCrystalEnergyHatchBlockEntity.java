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
 * 福鲁伊克斯水晶能源仓：为多方块机器提供 ME 电力。
 */
public class FluixCrystalEnergyHatchBlockEntity extends AENetworkBlockEntity {

    public FluixCrystalEnergyHatchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUIX_CRYSTAL_ENERGY_HATCH.get(), pos, state);
        getMainNode().setIdlePowerUsage(0);
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        return EnumSet.of(getBlockState().getValue(FluixCrystalEnergyHatchBlock.FACING));
    }

    public void updateGridConnectableSides() {
        onGridConnectableSidesChanged();
    }

    public boolean isGridConnected() {
        return getMainNode().getGrid() != null;
    }

    public IGrid getConnectedGrid() {
        return getMainNode().getGrid();
    }

    /**
     * 直接从 ME 网络抽取，无本地缓存。
     */
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

    public double getAvailableAEPower() {
        IGrid grid = getMainNode().getGrid();
        return grid != null ? grid.getEnergyService().getStoredPower() : 0;
    }

    /**
     * 多仓合计网络能量，按网络去重。
     */
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
