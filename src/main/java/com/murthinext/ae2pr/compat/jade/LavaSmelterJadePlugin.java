package com.murthinext.ae2pr.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.ITooltip;
import snownee.jade.api.WailaPlugin;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;

/**
 * 显示熔岩冶炼炉连接状态与作业的 Jade 插件。
 */
@WailaPlugin(ae2pr.MODID)
public class LavaSmelterJadePlugin extends AbstractMultiblockJadePlugin {

    private static final String KEY_DURABILITY = "durability";

    @Override
    protected Class<? extends BlockEntity> controllerType() {
        return LavaSmelterControllerBlockEntity.class;
    }

    @Override
    protected Class<? extends Block> blockType() {
        return HighReactivityLavaSmelterBlock.class;
    }

    @Override
    protected ResourceLocation dataUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "lava_smelter_data");
    }

    @Override
    protected ResourceLocation componentUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "lava_smelter");
    }

    @Override
    protected int progressColor() {
        return 0xFFE0681C;
    }

    @Override
    protected void appendExtraServerData(CompoundTag data, BlockEntity controller) {
        data.putInt(KEY_DURABILITY, ((LavaSmelterControllerBlockEntity) controller).getDurability());
    }

    @Override
    protected void appendExtraTooltip(ITooltip tooltip, CompoundTag data) {
        tooltip.add(Component.translatable("jade.ae2pr.lava_smelter.durability",
                data.getInt(KEY_DURABILITY), Config.lavaSmelterMaxDurability()));
    }
}
