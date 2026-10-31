package com.murthinext.ae2pr.machine.crystal_workstation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.machine.ModeMachine;
import com.murthinext.ae2pr.logic.multiblock.AbstractMultiblockControllerBlockEntity;

public class CrystalWorkstationControllerBlockEntity extends AbstractMultiblockControllerBlockEntity
        implements ModeMachine<CrystalWorkstationMode> {

    private static final String MODE_ID = "mode";

    private CrystalWorkstationMode mode = CrystalWorkstationMode.ETCHING;

    public CrystalWorkstationControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.CRYSTAL_WORKSTATION, pos, state);
    }

    @Override
    protected RecipeType<?> recipeType() {
        return mode.recipeType();
    }

    @Override
    public CrystalWorkstationMode getMode() {
        return mode;
    }

    @Override
    public boolean switchMode() {
        if (level == null || level.isClientSide || isBusy()) {
            return false;
        }
        mode = mode.next();
        clearPauseState();
        markStateChanged();
        return true;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(MODE_ID, mode.id());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        mode = CrystalWorkstationMode.byId(tag.getString(MODE_ID));
    }

    @Override
    public BlockPos getCorePos() {
        return CrystalWorkstationStructure.corePos(worldPosition, getFacing(), mirrorFront);
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos core = getCorePos();
        // 覆盖两侧面板与内腔粒子
        return new AABB(core).inflate(5.0);
    }
}
