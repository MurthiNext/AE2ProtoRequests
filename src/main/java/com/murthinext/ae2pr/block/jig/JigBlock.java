package com.murthinext.ae2pr.block.jig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;

public class JigBlock extends AbstractMultiblockControllerBlock<JigControllerBlockEntity> {

    public static final String MACHINE_KEY = "jig";

    public JigBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.5F)
                .sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    @Override
    protected JigControllerBlockEntity newController(BlockPos pos, BlockState state) {
        return new JigControllerBlockEntity(pos, state);
    }

    @Override
    public String machineKey() {
        return MACHINE_KEY;
    }

    @Override
    protected MenuProvider menuProvider(JigControllerBlockEntity controller) {
        return new SimpleMenuProvider((id, inventory, player) -> new JigMenu(id, inventory, controller),
                Component.translatable("block.ae2pr.jig"));
    }

    @Override
    protected void clientTick(JigControllerBlockEntity controller) {
        MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.JIG);
    }
}
