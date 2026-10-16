package com.murthinext.ae2pr.compat.jade;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;

import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberBlock;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;

/**
 * 显示水晶反应仓连接状态与作业的 Jade 插件。
 */
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

    @Override
    protected JobState jobState(BlockEntity controller) {
        CrystalReactionChamberControllerBlockEntity chamber = (CrystalReactionChamberControllerBlockEntity) controller;
        return new JobState(chamber.isFormed(), chamber.isEnergyConnected(), chamber.getJobElapsed(),
                chamber.getJobDuration(), chamber.getJobItem(), chamber.getJobFluid());
    }
}
