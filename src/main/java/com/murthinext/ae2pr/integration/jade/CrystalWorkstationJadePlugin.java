package com.murthinext.ae2pr.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationBlock;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationMode;

@WailaPlugin(ae2pr.MODID)
public class CrystalWorkstationJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return CrystalWorkstationControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return CrystalWorkstationBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_workstation_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_workstation");
    }

    @Override
    protected int progressColor() {
        return 0xFF55A3A8;
    }

    @Override
    protected void appendExtraServerData(CompoundTag data, BlockEntity controller) {
        data.putString("mode", ((CrystalWorkstationControllerBlockEntity) controller).getMode().id());
    }

    @Override
    protected void appendExtraTooltip(ITooltip tooltip, CompoundTag data) {
        tooltip.add(Component.translatable("gui.ae2pr.crystal_workstation.mode",
                CrystalWorkstationMode.byId(data.getString("mode")).displayName()));
    }
}
