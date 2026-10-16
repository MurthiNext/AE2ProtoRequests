package com.murthinext.ae2pr.compat.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.CrystalAssemblyLineBlock;

/**
 * 显示水晶装配线连接状态与作业的 Jade 插件。
 */
@WailaPlugin(ae2pr.MODID)
public class AssemblyLineJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return AssemblyLineControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return CrystalAssemblyLineBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "assembly_line_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "assembly_line");
    }

    @Override
    protected int progressColor() {
        return 0xFF3E9BC8;
    }

    @Override
    protected JobState jobState(BlockEntity controller) {
        AssemblyLineControllerBlockEntity line = (AssemblyLineControllerBlockEntity) controller;
        ItemStack output = line.getJobOutput();
        return new JobState(line.isFormed(), line.isEnergyConnected(), line.getJobElapsed(), line.getJobDuration(),
                output, FluidStack.EMPTY);
    }
}
