package com.murthinext.ae2pr.machine.crystal_workstation;

import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.api.machine.ModeMachineBlockEntity;
import com.murthinext.ae2pr.registry.ModMachines;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CrystalWorkstationControllerBlockEntity extends ModeMachineBlockEntity<CrystalWorkstationMode> {

    private static final float IDLE_TIDE = 0.6F;
    private float previousTide = IDLE_TIDE;
    private float tide = IDLE_TIDE;

    public CrystalWorkstationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.CRYSTAL_WORKSTATION, pos, state, List.of(CrystalWorkstationMode.values()));
    }

    @Override
    public BlockPos getCorePos() {
        return CrystalWorkstationStructure.corePos(worldPosition, getFacing(), isMirrorFront());
    }

    public void tickTide() {
        previousTide = tide;
        float target = getBlockState().getValue(MachineBlock.RUNNING) ? 1 : IDLE_TIDE;
        tide = Mth.approach(tide, target, target > tide ? 0.05F : 0.028F);
    }

    float tideStrength(float partialTick) {
        return Mth.lerp(partialTick, previousTide, tide);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getCorePos()).inflate(5.0);
    }
}
