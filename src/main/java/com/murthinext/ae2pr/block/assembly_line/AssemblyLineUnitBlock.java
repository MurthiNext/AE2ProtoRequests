package com.murthinext.ae2pr.block.assembly_line;

import java.text.NumberFormat;
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

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.logic.wrench.Wrenchable;
import com.murthinext.ae2pr.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.multiblock.module.ModuleProvider;
import com.murthinext.ae2pr.multiblock.module.ModuleType;

/**
 * 水晶装配线控制外壳。
 */
public class AssemblyLineUnitBlock extends Block implements Wrenchable {

    private static final NumberFormat NUMBER = NumberFormat.getNumberInstance();

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
            tooltip.add(moduleTooltip(provider.moduleDefinition()));
        }
    }

    /**
     * 增益模块显示实际效果，其余模块显示提供的等级。
     */
    private static Component moduleTooltip(ModuleDefinition definition) {
        ModuleType type = definition.type();
        if (type == ModModules.PARALLEL) {
            return Component.translatable("tooltip.ae2pr.assembly_line_module.parallel",
                    NUMBER.format(Config.assemblyModuleParallelPerUnit()));
        }
        if (type == ModModules.SPEED) {
            return Component.translatable("tooltip.ae2pr.assembly_line_module.speed",
                    NUMBER.format(Config.assemblyModuleSpeedPerUnit() * 100.0) + "%");
        }
        return Component.translatable("tooltip.ae2pr.assembly_line_module",
                Component.translatable(type.translationKey()), definition.level());
    }
}
