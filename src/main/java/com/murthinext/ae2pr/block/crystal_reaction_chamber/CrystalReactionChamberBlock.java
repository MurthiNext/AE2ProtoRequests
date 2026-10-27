package com.murthinext.ae2pr.block.crystal_reaction_chamber;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class CrystalReactionChamberBlock
        extends AbstractMultiblockControllerBlock<CrystalReactionChamberControllerBlockEntity> {

    public static final String MACHINE_KEY = "crystal_reaction_chamber";

    public CrystalReactionChamberBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected CrystalReactionChamberControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new CrystalReactionChamberControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(CrystalReactionChamberControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new CrystalReactionChamberMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.crystal_reaction_chamber"));
    }

    @Override
    protected void clientTick(CrystalReactionChamberControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.CRYSTAL_REACTION);
    }
}
