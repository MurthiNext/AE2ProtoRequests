package com.murthinext.ae2pr.integration.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.filter_press.FilterPressBlock;
import com.murthinext.ae2pr.block.filter_press.FilterPressControllerBlockEntity;

@WailaPlugin(ae2pr.MODID)
public class FilterPressJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return FilterPressControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return FilterPressBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "filter_press_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "filter_press");
    }

    @Override
    protected int progressColor() {
        return 0xFF7DA9D2;
    }
}
