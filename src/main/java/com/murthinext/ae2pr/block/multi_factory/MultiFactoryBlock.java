package com.murthinext.ae2pr.block.multi_factory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class MultiFactoryBlock extends AbstractMultiblockControllerBlock<MultiFactoryControllerBlockEntity> {

    public static final String MACHINE_KEY = "multi_factory";

    public MultiFactoryBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected MultiFactoryControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new MultiFactoryControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(MultiFactoryControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new MultiFactoryMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.multi_factory"));
    }

    @Override
    protected void clientTick(MultiFactoryControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, controller.getMode() == MultiFactoryMode.INSCRIBING
                ? MachineProcessingSounds.Type.MULTI_FACTORY_INSCRIBING
                : MachineProcessingSounds.Type.MULTI_FACTORY_CHARGING);
    }
}
