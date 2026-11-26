package com.murthinext.ae2pr.block.entangled_casing;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.ctm.CtmMeshBuilder;
import com.murthinext.ae2pr.api.ctm.CtmTextures;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntangledCasingPulseRenderer {

    public static final ResourceLocation BASE = Ids.of("block/entangled_casing_idle");
    private static final ResourceLocation CTM = Ids.of(BASE.getPath() + CtmTextures.ATLAS_SUFFIX);

    private static final double RENDER_DISTANCE = 64;
    private static final float OUTSET = 0.002F;
    private static final int VERTEX_STRIDE = 8;
    private static final RenderType LAYER = RenderType.cutout();
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(256));
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Frame[] FRAMES = new Frame[EntangledCasingPulse.FRAME_COUNT];

    private EntangledCasingPulseRenderer() {}

    public static void stitch(TextureAtlas atlas) {
        for (int i = 0; i < FRAMES.length; i++) {
            TextureAtlasSprite base = atlas.getSprite(Ids.of("block/entangled_casing_pulse/" + i));
            TextureAtlasSprite ctm = atlas.getSprite(Ids.of("block/entangled_casing_pulse/ctm_" + i));
            FRAMES[i] = isMissing(base) || isMissing(ctm) ? null : new Frame(base, ctm);
        }
    }

    private static boolean isMissing(TextureAtlasSprite sprite) {
        return MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name());
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !EntangledCasingPulse.hasFlashes(level)) {
            return;
        }

        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var blockRenderer = minecraft.getBlockRenderer();
        var buffer = BUFFERS.getBuffer(LAYER);
        EntangledCasingPulse.forEachFlash((pos, index) -> {
            Frame frame = FRAMES[index];
            if (frame == null
                    || !pos.closerToCenterThan(camera, RENDER_DISTANCE)
                    || !event.getFrustum().isVisible(new AABB(pos))
                    || !EntangledCasingPulse.isCasing(level, pos)) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            BakedModel model = blockRenderer.getBlockModel(state);
            ModelData data = model.getModelData(level, pos, state, ModelData.EMPTY);
            pose.pushPose();
            pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            blockRenderer
                    .getModelRenderer()
                    .tesselateBlock(
                            level,
                            new PulseModel(model, frame),
                            state,
                            pos,
                            pose,
                            buffer,
                            true,
                            RANDOM,
                            state.getSeed(pos),
                            OverlayTexture.NO_OVERLAY,
                            data,
                            RenderType.solid());
            pose.popPose();
        });
        BUFFERS.endBatch(LAYER);
    }

    private record Frame(TextureAtlasSprite base, TextureAtlasSprite ctm) {}

    private static final class PulseModel extends BakedModelWrapper<BakedModel> {

        private final Frame frame;

        private PulseModel(BakedModel originalModel, Frame frame) {
            super(originalModel);
            this.frame = frame;
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState state,
                @Nullable Direction side,
                RandomSource random,
                ModelData data,
                @Nullable RenderType renderType) {
            var result = new ArrayList<BakedQuad>();
            for (BakedQuad quad : super.getQuads(state, side, random, data, renderType)) {
                TextureAtlasSprite sprite = quad.getSprite();
                ResourceLocation name = sprite.contents().name();
                TextureAtlasSprite target = BASE.equals(name) ? frame.base() : CTM.equals(name) ? frame.ctm() : null;
                if (target != null) {
                    BakedQuad overlay = CtmMeshBuilder.retarget(quad, sprite, target);
                    offset(overlay);
                    result.add(overlay);
                }
            }
            return result;
        }

        private static void offset(BakedQuad quad) {
            Direction normal = quad.getDirection();
            int axis = normal.getAxis().ordinal();
            float shift = normal.getAxisDirection().getStep() * OUTSET;
            int[] vertices = quad.getVertices();
            for (int i = axis; i < vertices.length; i += VERTEX_STRIDE) {
                vertices[i] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[i]) + shift);
            }
        }
    }
}
