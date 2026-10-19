package com.murthinext.ae2pr.block.crystal_workstation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

/**
 * 晶能工作站主机方块。
 */
public class CrystalWorkstationBlock extends AbstractMultiblockControllerBlock<CrystalWorkstationControllerBlockEntity> {

    public static final String MACHINE_KEY = "crystal_workstation";

    public CrystalWorkstationBlock() {
        super(Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected CrystalWorkstationControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new CrystalWorkstationControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(CrystalWorkstationControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new CrystalWorkstationMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.crystal_workstation"));
    }
}
