package com.murthinext.ae2pr.block.matter_phase_transformer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class MatterPhaseTransformerBlock
        extends AbstractMultiblockControllerBlock<MatterPhaseTransformerControllerBlockEntity> {

    public static final String MACHINE_KEY = "matter_phase_transformer";

    public MatterPhaseTransformerBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected MatterPhaseTransformerControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new MatterPhaseTransformerControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(MatterPhaseTransformerControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new MatterPhaseTransformerMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.matter_phase_transformer"));
    }

    @Override
    protected void clientTick(MatterPhaseTransformerControllerBlockEntity controller) {
        controller.clientTick();
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.MATTER_PHASE_TRANSFORMER);
    }
}
