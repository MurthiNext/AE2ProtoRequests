package com.murthinext.ae2pr.client.ctm;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * 象限连接纹理：把一个面拆成 4 个象限，各自从 4×4 格（32×32）象限图集里取一块。
 *
 * <p>图集语义与 GregTech Modern 的 {@code *_ctm.png} 一致：
 * <ul>
 * <li>(0..1, 0..1) 未连接 —— 直接取基础贴图的对应象限，保留外框、铆钉与槽纹</li>
 * <li>(0..1, 0..1) 图集 四邻接 + 对角全连接 —— 无缝面板</li>
 * <li>(0..1, 2..3) 仅水平相连 —— 斜纹横向展开、竖向槽纹消失</li>
 * <li>(2..3, 0..1) 仅竖直相连 —— 竖向槽纹保留并延长</li>
 * <li>(2..3, 2..3) 两邻边相连但缺对角 —— 凹角</li>
 * </ul>
 */
public final class QuadrantCtmMeshBuilder {

    /** 图集每边的格数。 */
    public static final int GRID = 4;

    private static final int STRIDE = 8;
    private static final int UV_OFFSET = 4;
    private static final float HALF = 0.5F;

    private QuadrantCtmMeshBuilder() {
    }

    /**
     * 把单个面拆成 4 个象限面。
     */
    public static List<BakedQuad> build(BakedQuad quad, TextureAtlasSprite base, TextureAtlasSprite atlas,
            int worldMask) {
        Direction uAxis = CtmMeshBuilder.uAxis(quad);
        Direction vAxis = CtmMeshBuilder.vAxis(quad);
        int edgeMask = edgeMask(uAxis, vAxis, worldMask);

        Corners corners = Corners.of(quad, base);
        float tileWidth = (atlas.getU1() - atlas.getU0()) / GRID;
        float tileHeight = (atlas.getV1() - atlas.getV0()) / GRID;

        List<BakedQuad> result = new ArrayList<>(4);
        for (int qx = 0; qx < 2; qx++) {
            for (int qy = 0; qy < 2; qy++) {
                boolean horizontal = (edgeMask & (1 << qx)) != 0;
                boolean vertical = (edgeMask & (1 << (2 + qy))) != 0;
                boolean diagonal = diagonal(1 << (qy * 2 + qx), edgeMask, worldMask, uAxis, vAxis);
                int[] cell = cell(qx, qy, horizontal, vertical, diagonal);
                result.add(corners.subQuad(quad, base, atlas, qx, qy, cell, tileWidth, tileHeight));
            }
        }
        return result;
    }

    /**
     * 该象限对应的图集格 {@code {col, row}}；{@code null} 表示沿用基础贴图的对应象限。
     */
    @Nullable
    private static int[] cell(int qx, int qy, boolean horizontal, boolean vertical, boolean diagonal) {
        if (!horizontal && !vertical) {
            return null;
        }
        if (horizontal && vertical) {
            return diagonal ? new int[] { qx, qy } : new int[] { qx + 2, qy + 2 };
        }
        return horizontal ? new int[] { qx, qy + 2 } : new int[] { qx + 2, qy };
    }

    /**
     * 边掩码：bit0 = 最小 U 侧、bit1 = 最大 U 侧、bit2 = 最小 V 侧、bit3 = 最大 V 侧。
     */
    private static int edgeMask(@Nullable Direction uAxis, @Nullable Direction vAxis, int worldMask) {
        int mask = 0;
        if (uAxis != null) {
            if (CtmConfig.connected(worldMask, uAxis.getOpposite())) {
                mask |= 1;
            }
            if (CtmConfig.connected(worldMask, uAxis)) {
                mask |= 2;
            }
        }
        if (vAxis != null) {
            if (CtmConfig.connected(worldMask, vAxis.getOpposite())) {
                mask |= 4;
            }
            if (CtmConfig.connected(worldMask, vAxis)) {
                mask |= 8;
            }
        }
        return mask;
    }

    /**
     * 该象限的对角块是否连接；两条邻边未同时连接时恒为 false。
     */
    private static boolean diagonal(int cornerBit, int edgeMask, int worldMask,
            @Nullable Direction uAxis, @Nullable Direction vAxis) {
        if (uAxis == null || vAxis == null) {
            return false;
        }
        Direction minU = uAxis.getOpposite();
        Direction minV = vAxis.getOpposite();
        return switch (cornerBit) {
            case 1 -> (edgeMask & 0b0101) == 0b0101 && CtmConfig.connected(worldMask, minU, minV);
            case 2 -> (edgeMask & 0b0110) == 0b0110 && CtmConfig.connected(worldMask, uAxis, minV);
            case 4 -> (edgeMask & 0b1001) == 0b1001 && CtmConfig.connected(worldMask, minU, vAxis);
            default -> (edgeMask & 0b1010) == 0b1010 && CtmConfig.connected(worldMask, uAxis, vAxis);
        };
    }

    /**
     * 原面四角按 (s, t) 归一化位置索引，便于按象限切片。
     */
    private record Corners(float[][] pos, boolean ccw) {

        static Corners of(BakedQuad quad, TextureAtlasSprite base) {
            int[] src = quad.getVertices();
            float width = base.getU1() - base.getU0();
            float height = base.getV1() - base.getV0();
            float[][] pos = new float[4][3];
            float[] stS = new float[4];
            float[] stT = new float[4];
            for (int i = 0; i < 4; i++) {
                int offset = i * STRIDE;
                float u = Float.intBitsToFloat(src[offset + UV_OFFSET]);
                float v = Float.intBitsToFloat(src[offset + UV_OFFSET + 1]);
                float s = width == 0.0F ? 0.0F : (u - base.getU0()) / width;
                float t = height == 0.0F ? 0.0F : (v - base.getV0()) / height;
                stS[i] = s;
                stT[i] = t;
                int index = (s > HALF ? 1 : 0) + (t > HALF ? 2 : 0);
                pos[index][0] = Float.intBitsToFloat(src[offset]);
                pos[index][1] = Float.intBitsToFloat(src[offset + 1]);
                pos[index][2] = Float.intBitsToFloat(src[offset + 2]);
            }
            float area = 0.0F;
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                area += stS[i] * stT[next] - stS[next] * stT[i];
            }
            return new Corners(pos, area >= 0.0F);
        }

        /**
         * 按 (s, t) 双线性插值取位置分量。
         */
        float posAt(int component, float s, float t) {
            float near = pos[0][component] * (1.0F - s) + pos[1][component] * s;
            float far = pos[2][component] * (1.0F - s) + pos[3][component] * s;
            return near * (1.0F - t) + far * t;
        }

        /**
         * 切出指定象限，并把 UV 重映射到目标格或基础贴图的对应象限。
         */
        BakedQuad subQuad(BakedQuad quad, TextureAtlasSprite base, TextureAtlasSprite atlas,
                int qx, int qy, @Nullable int[] cell, float tileWidth, float tileHeight) {
            int[] src = quad.getVertices();
            int[] out = src.clone();

            float s0 = qx * HALF;
            float s1 = s0 + HALF;
            float t0 = qy * HALF;
            float t1 = t0 + HALF;

            // 与原面同向的两个环绕顺序
            float[][] forward = { { s0, t0 }, { s1, t0 }, { s1, t1 }, { s0, t1 } };
            float[][] reverse = { { s0, t0 }, { s0, t1 }, { s1, t1 }, { s1, t0 } };
            float[][] order = ccw ? forward : reverse;

            float baseWidth = base.getU1() - base.getU0();
            float baseHeight = base.getV1() - base.getV0();
            // 未连接时取基础贴图里对应的那一个象限，而不是整张底图
            float cellU0 = cell == null ? base.getU0() + qx * baseWidth * HALF
                    : atlas.getU0() + cell[0] * tileWidth;
            float cellV0 = cell == null ? base.getV0() + qy * baseHeight * HALF
                    : atlas.getV0() + cell[1] * tileHeight;
            float spanU = cell == null ? baseWidth * HALF : tileWidth;
            float spanV = cell == null ? baseHeight * HALF : tileHeight;

            for (int i = 0; i < 4; i++) {
                float s = order[i][0];
                float t = order[i][1];
                int offset = i * STRIDE;
                out[offset] = Float.floatToRawIntBits(posAt(0, s, t));
                out[offset + 1] = Float.floatToRawIntBits(posAt(1, s, t));
                out[offset + 2] = Float.floatToRawIntBits(posAt(2, s, t));
                // 把 [qx/2, qx/2+1/2] 的整面比例换算成该象限内的 0..1
                out[offset + UV_OFFSET] = Float.floatToRawIntBits(cellU0 + (s * 2.0F - qx) * spanU);
                out[offset + UV_OFFSET + 1] = Float.floatToRawIntBits(cellV0 + (t * 2.0F - qy) * spanV);
            }
            return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), cell == null ? base : atlas,
                    quad.isShade());
        }
    }
}
