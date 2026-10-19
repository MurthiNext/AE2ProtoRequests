package com.murthinext.ae2pr.block.assembly_line;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

/**
 * 水晶装配线控制器方块。
 */
public class CrystalAssemblyLineBlock
        extends AbstractMultiblockControllerBlock<AssemblyLineControllerBlockEntity> {

    public static final String MACHINE_KEY = "crystal_assembly_line";

    public CrystalAssemblyLineBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    protected AssemblyLineControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new AssemblyLineControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(AssemblyLineControllerBlockEntity controller) {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new AssemblyLineMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.crystal_assembly_line"));
    }

    @Override
    protected void clientTick(AssemblyLineControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.ASSEMBLY_LINE);
    }
}
