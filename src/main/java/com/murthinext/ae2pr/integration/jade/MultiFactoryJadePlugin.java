package com.murthinext.ae2pr.integration.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryBlock;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.block.multi_factory.MultiFactoryMode;

@WailaPlugin(ae2pr.MODID)
public class MultiFactoryJadePlugin extends AbstractMultiblockJadePlugin {

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return MultiFactoryControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return MultiFactoryBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multi_factory_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multi_factory");
    }

    @Override
    protected int progressColor() {
        return 0xFFA47BD6;
    }

    @Override
    protected void appendExtraServerData(CompoundTag data, BlockEntity controller) {
        data.putString("mode", ((MultiFactoryControllerBlockEntity) controller).getMode().id());
    }

    @Override
    protected void appendExtraTooltip(ITooltip tooltip, CompoundTag data) {
        tooltip.add(Component.translatable("gui.ae2pr.multi_factory.mode",
                MultiFactoryMode.byId(data.getString("mode")).displayName()));
    }
}
