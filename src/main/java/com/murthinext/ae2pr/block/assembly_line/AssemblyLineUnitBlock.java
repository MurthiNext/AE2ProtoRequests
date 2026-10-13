package com.murthinext.ae2pr.block.assembly_line;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;

import com.murthinext.ae2pr.logic.wrench.Wrenchable;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;

/**
 * 水晶装配线控制外壳。
 */
public class AssemblyLineUnitBlock extends Block implements Wrenchable {

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AssemblyLineUnitBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (this instanceof ModuleProvider provider) {
            var definition = provider.moduleDefinition();
            tooltip.add(Component.translatable("tooltip.ae2pr.assembly_line_module",
                    Component.translatable(definition.type().translationKey()), definition.level()));
        }
    }
}
