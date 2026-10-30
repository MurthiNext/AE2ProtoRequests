package com.murthinext.ae2pr.block.high_reactivity_lava_smelter;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class HighReactivityLavaSmelterBlock
        extends AbstractMultiblockControllerBlock<HighReactivityLavaSmelterControllerBlockEntity> {

    public static final String MACHINE_KEY = "high_reactivity_lava_smelter";

    public HighReactivityLavaSmelterBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_YELLOW)
                .strength(3.5F)
                .sound(SoundType.STONE)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected HighReactivityLavaSmelterControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new HighReactivityLavaSmelterControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    public String tooltipKey() {
        return "high_reactivity_lava_smelter";
    }

    @Override
    protected MenuProvider menuProvider(HighReactivityLavaSmelterControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new HighReactivityLavaSmelterMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.high_reactivity_lava_smelter"));
    }

    @Override
    protected void clientTick(HighReactivityLavaSmelterControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.HIGH_REACTIVITY_LAVA_SMELTER);
    }
}
