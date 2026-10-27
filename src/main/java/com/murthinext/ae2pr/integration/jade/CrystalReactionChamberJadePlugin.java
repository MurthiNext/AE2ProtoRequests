package com.murthinext.ae2pr.integration.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberBlock;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;

@WailaPlugin(ae2pr.MODID)
public class CrystalReactionChamberJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return CrystalReactionChamberControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return CrystalReactionChamberBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_reaction_chamber_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_reaction_chamber");
    }

    @Override
    protected int progressColor() {
        return 0xFF55A3A8;
    }
}
