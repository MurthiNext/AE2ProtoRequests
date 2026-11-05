package com.murthinext.ae2pr.api.ctm;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

// 连接纹理的模型包装：注入方块位置，并按绑定规则重写 UV
public class CtmBakedModel extends BakedModelWrapper<BakedModel> {

    private final CtmBinding binding;

    public CtmBakedModel(BakedModel originalModel, CtmBinding binding) {
        super(originalModel);
        this.binding = binding;
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

        // 多方块成型换装：先把基础面贴图整组换掉，再参与连接
        CtmBinding.Retarget retarget = binding.retarget(state);
        if (retarget != null) {
            quads = CtmMeshBuilder.retarget(quads, retarget.from(), retarget.to());
        }
        BlockAndTintGetter level = data.get(CtmModelProperties.LEVEL);
        BlockPos pos = data.get(CtmModelProperties.POS);
        if (level == null || pos == null) {
            return quads;
        }
        List<BakedQuad> appearance = binding.appearance(quads, side, level, pos);
        if (appearance != null) {
            return appearance;
        }
        return binding.enabled(state) ? CtmMeshBuilder.rewrite(quads, binding.mask(level, pos, state)) : quads;
    }
}
