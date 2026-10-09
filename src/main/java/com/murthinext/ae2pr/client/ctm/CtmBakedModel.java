package com.murthinext.ae2pr.client.ctm;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

/**
 * 连接纹理的模型包装：把方块位置注入 {@link ModelData}，并在取四边形时按世界邻居重写 UV。
 */
public class CtmBakedModel extends BakedModelWrapper<BakedModel> {

    public CtmBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData parent = super.getModelData(level, pos, state, modelData);
        return ModelData.builder()
                .with(CtmModelProperties.LEVEL, level)
                .with(CtmModelProperties.POS, pos)
                .with(CtmModelProperties.PARENT, parent)
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
            ModelData data, @Nullable RenderType renderType) {
        ModelData parent = data.has(CtmModelProperties.PARENT) ? data.get(CtmModelProperties.PARENT) : data;
        List<BakedQuad> quads = super.getQuads(state, side, rand, parent, renderType);
        if (state == null || quads.isEmpty()) {
            return quads;
        }

        // 多方块成型换装：把部件自身的基础面贴图换成结构外观贴图（UV 不变，连接纹理由下方重写）
        TextureAtlasSprite facade = CtmConfig.facadeSprite(state);
        if (facade != null) {
            TextureAtlasSprite base = CtmConfig.partBaseSprite(state);
            if (base != null && base != facade) {
                quads = retargetFacade(quads, base, facade);
            }
        }

        BlockAndTintGetter level = data.get(CtmModelProperties.LEVEL);
        BlockPos pos = data.get(CtmModelProperties.POS);
        if (level == null || pos == null || !CtmConfig.enabled(state)) {
            return quads;
        }

        int worldMask = CtmConfig.worldMask(level, pos, state);
        List<BakedQuad> result = null;
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            TextureAtlasSprite atlas = CtmSprites.atlasFor(quad.getSprite());
            if (atlas == null) {
                if (result != null) {
                    result.add(quad);
                }
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(quads.size());
                result.addAll(quads.subList(0, i));
            }
            result.add(CtmMeshBuilder.rewrite(quad, quad.getSprite(), atlas, worldMask));
        }
        return result != null ? result : quads;
    }

    /** 把基础面的四边形换成结构外观贴图（UV 随之重映射）；其余四边形原样保留。 */
    private static List<BakedQuad> retargetFacade(List<BakedQuad> quads, TextureAtlasSprite base,
            TextureAtlasSprite facade) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            result.add(quad.getSprite() == base ? CtmMeshBuilder.retarget(quad, base, facade) : quad);
        }
        return result;
    }
}
