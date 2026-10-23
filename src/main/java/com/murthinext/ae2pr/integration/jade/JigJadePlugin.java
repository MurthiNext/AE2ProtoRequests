package com.murthinext.ae2pr.integration.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.jig.JigBlock;
import com.murthinext.ae2pr.block.jig.JigControllerBlockEntity;

@WailaPlugin(ae2pr.MODID)
public class JigJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return JigControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return JigBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "jig_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "jig");
    }

    @Override
    protected int progressColor() {
        return 0xFF7DA9D2;
    }
}
