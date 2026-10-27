package com.murthinext.ae2pr.integration.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerBlock;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;

@WailaPlugin(ae2pr.MODID)
public class MatterPhaseTransformerJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return MatterPhaseTransformerControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return MatterPhaseTransformerBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "matter_phase_transformer_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "matter_phase_transformer");
    }

    @Override
    protected int progressColor() {
        return 0xFF62D0DA;
    }
}
