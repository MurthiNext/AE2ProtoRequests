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

import com.murthinext.ae2pr.client.gui.crystal_workstation.CrystalWorkstationCtm;

/**
 * 连接纹理的模型包装：注入方块位置并按邻居重写 UV。
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

        BlockAndTintGetter level = data.get(CtmModelProperties.LEVEL);
        BlockPos pos = data.get(CtmModelProperties.POS);

        // 多方块成型换装：替换基础面贴图为结构外观贴图
        TextureAtlasSprite facade = CtmConfig.facadeSprite(state);
        if (facade != null) {
            TextureAtlasSprite base = CtmConfig.facadeBaseSprite(state);
            if (base != null && base != facade) {
                quads = retarget(quads, base, facade);
            }
        }

        if (level == null || pos == null) {
            return quads;
        }
        // 晶能工作站成型外观全部在客户端替换
        List<BakedQuad> appearance = CrystalWorkstationCtm.appearance(quads, side, level, pos);
        if (appearance != null) {
            return appearance;
        }
        if (!CtmConfig.enabled(state)) {
            return quads;
        }
        int worldMask = CtmConfig.worldMask(level, pos, state);
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite atlas = CtmSprites.atlasFor(quad.getSprite());
            result.add(atlas != null ? CtmMeshBuilder.rewrite(quad, quad.getSprite(), atlas, worldMask) : quad);
        }
        return result;
    }

    private static List<BakedQuad> retarget(List<BakedQuad> quads, TextureAtlasSprite from, TextureAtlasSprite to) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            result.add(quad.getSprite() == from ? CtmMeshBuilder.retarget(quad, from, to) : quad);
        }
        return result;
    }
}
