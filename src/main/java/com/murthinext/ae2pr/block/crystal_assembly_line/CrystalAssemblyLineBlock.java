package com.murthinext.ae2pr.block.crystal_assembly_line;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class CrystalAssemblyLineBlock
        extends AbstractMultiblockControllerBlock<CrystalAssemblyLineControllerBlockEntity> {

    public static final String MACHINE_KEY = "crystal_assembly_line";

    public CrystalAssemblyLineBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected CrystalAssemblyLineControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new CrystalAssemblyLineControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(CrystalAssemblyLineControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new CrystalAssemblyLineMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.crystal_assembly_line"));
    }

    @Override
    protected void clientTick(CrystalAssemblyLineControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.CRYSTAL_ASSEMBLY_LINE);
    }
}
