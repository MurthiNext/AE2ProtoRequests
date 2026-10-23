package com.murthinext.ae2pr.integration.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_assembly_line.CrystalAssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_assembly_line.CrystalAssemblyLineBlock;

/**
 * 显示水晶装配线连接状态与作业的 Jade 插件。
 */
@WailaPlugin(ae2pr.MODID)
public class CrystalAssemblyLineJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return CrystalAssemblyLineControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return CrystalAssemblyLineBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_assembly_line_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_assembly_line");
    }

    @Override
    protected int progressColor() {
        return 0xFF3E9BC8;
    }
}
