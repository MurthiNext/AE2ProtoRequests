package com.murthinext.ae2pr.client.molecular_assembly_core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.block.molecular_assembly_core.MolecularAssemblyCoreBlockEntity;

public class MolecularAssemblyCoreRenderer implements BlockEntityRenderer<MolecularAssemblyCoreBlockEntity> {

    private static final ItemStack SINGULARITY = AEItems.SINGULARITY.stack();

    private static final float YAW_SPEED = 1.2F;
    private static final float PITCH_SPEED = 0.6F;
    private static final float ROLL_SPEED = 0.6F;
    private static final float ITEM_SCALE = 0.55F;

    public MolecularAssemblyCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MolecularAssemblyCoreBlockEntity core, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        Level level = core.getLevel();
        if (level == null) {
            return;
        }
        float ticks = level.getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(core.yawOffset + ticks * YAW_SPEED * core.yawSpeed));
        pose.mulPose(Axis.XP.rotationDegrees(core.pitchOffset + ticks * PITCH_SPEED * core.pitchSpeed));
        pose.mulPose(Axis.ZP.rotationDegrees(core.rollOffset + ticks * ROLL_SPEED * core.rollSpeed));
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(SINGULARITY, ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT, overlay, pose, buffers, level, 0);
        pose.popPose();
    }
}
