package com.murthinext.ae2pr.block.filter_press;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class FilterPressBlock extends AbstractMultiblockControllerBlock<FilterPressControllerBlockEntity> {

    public static final String MACHINE_KEY = "filter_press";

    public FilterPressBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected FilterPressControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new FilterPressControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(FilterPressControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new FilterPressMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.filter_press"));
    }

    @Override
    protected void clientTick(FilterPressControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.FILTER_PRESS);
    }
}
