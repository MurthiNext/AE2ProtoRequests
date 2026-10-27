package com.murthinext.ae2pr.client.ctm;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

// 连接纹理的面重写：按邻居掩码选图块并重写 UV
public final class CtmMeshBuilder {

    private static final int STRIDE = 8;
    private static final int UV_OFFSET = 4;
    private static final float GRID = 8.0F;
    private static final float EPS = 1.0E-4F;

    private static final int[] VALID_CORNERS = {
            0, 0, 0, 0, 0, 1, 2, 3, 0, 4, 8, 12, 0, 5, 10, 15
    };

    private static final int[] TILE_OFFSET = {
            0, 1, 2, 3, 4, 5, 7, 9, 13, 14, 16, 18, 22, 23, 27, 31
    };

    private CtmMeshBuilder() {
    }

    // 把四边形 UV 重写到 CTM 图集中对应图块
    public static BakedQuad rewrite(BakedQuad quad, TextureAtlasSprite base, TextureAtlasSprite atlas, int worldMask) {
        int[] src = quad.getVertices();
        int[] out = src.clone();

        int tileIndex = tileIndex(src, worldMask);
        int tileX = tileIndex & 7;
        int tileY = tileIndex >> 3;

        float tileWidth = (atlas.getU1() - atlas.getU0()) / GRID;
        float tileHeight = (atlas.getV1() - atlas.getV0()) / GRID;
        float tileU0 = atlas.getU0() + tileX * tileWidth;
        float tileV0 = atlas.getV0() + tileY * tileHeight;

        float baseWidth = base.getU1() - base.getU0();
        float baseHeight = base.getV1() - base.getV0();

        for (int i = 0; i < 4; i++) {
            int offset = i * STRIDE + UV_OFFSET;
            float u = Float.intBitsToFloat(src[offset]);
            float v = Float.intBitsToFloat(src[offset + 1]);
            float localU = baseWidth == 0.0F ? 0.5F : (u - base.getU0()) / baseWidth;
            float localV = baseHeight == 0.0F ? 0.5F : (v - base.getV0()) / baseHeight;
            out[offset] = Float.floatToRawIntBits(tileU0 + localU * tileWidth);
            out[offset + 1] = Float.floatToRawIntBits(tileV0 + localV * tileHeight);
        }
        return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), atlas, quad.isShade());
    }

    // 替换贴图并按贴图内相对位置重映射 UV
    public static BakedQuad retarget(BakedQuad quad, TextureAtlasSprite from, TextureAtlasSprite to) {
        int[] src = quad.getVertices();
        int[] out = src.clone();
        float fromWidth = from.getU1() - from.getU0();
        float fromHeight = from.getV1() - from.getV0();
        float toWidth = to.getU1() - to.getU0();
        float toHeight = to.getV1() - to.getV0();
        for (int i = 0; i < 4; i++) {
            int offset = i * STRIDE + UV_OFFSET;
            float u = Float.intBitsToFloat(src[offset]);
            float v = Float.intBitsToFloat(src[offset + 1]);
            float localU = fromWidth == 0.0F ? 0.5F : (u - from.getU0()) / fromWidth;
            float localV = fromHeight == 0.0F ? 0.5F : (v - from.getV0()) / fromHeight;
            out[offset] = Float.floatToRawIntBits(to.getU0() + localU * toWidth);
            out[offset + 1] = Float.floatToRawIntBits(to.getV0() + localV * toHeight);
        }
        return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), to, quad.isShade());
    }

    @Nullable
    public static Direction uAxis(BakedQuad quad) {
        return axisFromUvs(quad.getVertices(), true);
    }

    @Nullable
    public static Direction vAxis(BakedQuad quad) {
        return axisFromUvs(quad.getVertices(), false);
    }

    // 计算该面的 47-tile 图块序号，角掩码需两条邻边均已连接
    private static int tileIndex(int[] vertices, int worldMask) {
        Direction uAxis = axisFromUvs(vertices, true);
        Direction vAxis = axisFromUvs(vertices, false);
        int edgeMask = 0;
        if (uAxis != null) {
            if (CtmConfig.connected(worldMask, uAxis.getOpposite())) {
                edgeMask |= 1;
            }
            if (CtmConfig.connected(worldMask, uAxis)) {
                edgeMask |= 2;
            }
        }
        if (vAxis != null) {
            if (CtmConfig.connected(worldMask, vAxis.getOpposite())) {
                edgeMask |= 4;
            }
            if (CtmConfig.connected(worldMask, vAxis)) {
                edgeMask |= 8;
            }
        }

        int cornerMask = 0;
        if (uAxis != null && vAxis != null) {
            Direction minU = uAxis.getOpposite();
            Direction minV = vAxis.getOpposite();
            if ((edgeMask & 0b0101) == 0b0101 && CtmConfig.connected(worldMask, minU, minV)) {
                cornerMask |= 1;
            }
            if ((edgeMask & 0b0110) == 0b0110 && CtmConfig.connected(worldMask, uAxis, minV)) {
                cornerMask |= 2;
            }
            if ((edgeMask & 0b1001) == 0b1001 && CtmConfig.connected(worldMask, minU, vAxis)) {
                cornerMask |= 4;
            }
            if ((edgeMask & 0b1010) == 0b1010 && CtmConfig.connected(worldMask, uAxis, vAxis)) {
                cornerMask |= 8;
            }
        }

        // 图块序号 = 起始序号 + 角状态变体
        int valid = VALID_CORNERS[edgeMask];
        int variant = 0;
        int shift = 0;
        for (int c = 0; c < 4; c++) {
            if ((valid & (1 << c)) != 0) {
                if ((cornerMask & (1 << c)) != 0) {
                    variant |= 1 << shift;
                }
                shift++;
            }
        }
        return TILE_OFFSET[edgeMask] + variant;
    }

    @Nullable
    private static Direction axisFromUvs(int[] vertices, boolean forU) {
        int component = forU ? 0 : 1;
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float uv = Float.intBitsToFloat(vertices[i * STRIDE + UV_OFFSET + component]);
            min = Math.min(min, uv);
            max = Math.max(max, uv);
        }
        if (max - min < EPS) {
            return null;
        }

        float[] minPos = new float[3];
        float[] maxPos = new float[3];
        int minCount = 0;
        int maxCount = 0;
        for (int i = 0; i < 4; i++) {
            float uv = Float.intBitsToFloat(vertices[i * STRIDE + UV_OFFSET + component]);
            float[] target = null;
            if (Math.abs(uv - min) < EPS) {
                target = minPos;
                minCount++;
            } else if (Math.abs(uv - max) < EPS) {
                target = maxPos;
                maxCount++;
            }
            if (target != null) {
                target[0] += Float.intBitsToFloat(vertices[i * STRIDE]);
                target[1] += Float.intBitsToFloat(vertices[i * STRIDE + 1]);
                target[2] += Float.intBitsToFloat(vertices[i * STRIDE + 2]);
            }
        }
        if (minCount == 0 || maxCount == 0) {
            return null;
        }
        for (int i = 0; i < 3; i++) {
            minPos[i] /= minCount;
            maxPos[i] /= maxCount;
        }
        return dominantDirection(maxPos[0] - minPos[0], maxPos[1] - minPos[1], maxPos[2] - minPos[2]);
    }

    private static Direction dominantDirection(float dx, float dy, float dz) {
        float ax = Math.abs(dx);
        float ay = Math.abs(dy);
        float az = Math.abs(dz);
        if (ax >= ay && ax >= az) {
            return dx >= 0.0F ? Direction.EAST : Direction.WEST;
        }
        if (ay >= az) {
            return dy >= 0.0F ? Direction.UP : Direction.DOWN;
        }
        return dz >= 0.0F ? Direction.SOUTH : Direction.NORTH;
    }
}
