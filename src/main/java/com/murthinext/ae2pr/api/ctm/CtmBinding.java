package com.murthinext.ae2pr.api.ctm;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 连接纹理的模型绑定：由模组侧提供“哪些方块参与连接、怎么连、成型后换成什么外观”，
 * 模板负责按掩码重写 UV。
 */
public interface CtmBinding {

    boolean enabled(BlockState state);

    /** 世界空间连接掩码，可用 {@link CtmMask#compute} 组装。 */
    int mask(BlockAndTintGetter level, BlockPos pos, BlockState state);

    // 整面替换外观；返回 null 表示走通用连接纹理
    @Nullable
    default List<BakedQuad> appearance(List<BakedQuad> quads, @Nullable Direction side, BlockAndTintGetter level,
                                       BlockPos pos) {
        return null;
    }

    @Nullable
    default Retarget retarget(BlockState state) {
        return null;
    }

    record Retarget(TextureAtlasSprite from, TextureAtlasSprite to) {}
}
