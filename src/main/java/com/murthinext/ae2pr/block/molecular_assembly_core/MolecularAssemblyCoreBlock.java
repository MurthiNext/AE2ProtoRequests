package com.murthinext.ae2pr.block.molecular_assembly_core;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.block.DisassemblableBlock;
import com.murthinext.ae2pr.client.molecular_assembly_core.MolecularAssemblyCoreParticles;

public class MolecularAssemblyCoreBlock extends DisassemblableBlock implements EntityBlock {

    public MolecularAssemblyCoreBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MolecularAssemblyCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? (tickLevel, pos, tickState, blockEntity) -> MolecularAssemblyCoreParticles.tick(tickLevel, pos)
                : null;
    }
}
