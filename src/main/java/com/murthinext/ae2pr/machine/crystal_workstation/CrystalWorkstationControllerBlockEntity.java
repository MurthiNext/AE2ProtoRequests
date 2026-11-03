package com.murthinext.ae2pr.machine.crystal_workstation;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.api.machine.ModeMachineBlockEntity;
import com.murthinext.ae2pr.registry.ModMachines;

public class CrystalWorkstationControllerBlockEntity
        extends ModeMachineBlockEntity<CrystalWorkstationMode> {

    public CrystalWorkstationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.CRYSTAL_WORKSTATION, pos, state, List.of(CrystalWorkstationMode.values()));
    }

    @Override
    public BlockPos getCorePos() {
        return CrystalWorkstationStructure.corePos(worldPosition, getFacing(), isMirrorFront());
    }

    @Override
    public AABB getRenderBoundingBox() {
        // 覆盖两侧面板与内腔粒子
        return new AABB(getCorePos()).inflate(5.0);
    }
}
