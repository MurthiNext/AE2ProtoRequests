package com.murthinext.ae2pr.client.gui.crystal_workstation;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.client.ctm.CtmConfig;
import com.murthinext.ae2pr.client.ctm.CtmMeshBuilder;
import com.murthinext.ae2pr.client.ctm.CtmSprites;
import com.murthinext.ae2pr.client.ctm.QuadrantCtmMeshBuilder;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;

/**
 * 晶能工作站成型外观：主机换装、隐藏核心、玻璃单面化与两侧 3x3 面板换装连纹。
 */
public final class CrystalWorkstationCtm {

    private static final ResourceLocation STABILITY_SKIN = id("block/high_energy_stability_casing");
    private static final int STRIDE = 8;
    private static final int UV_OFFSET = 4;

    private CrystalWorkstationCtm() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, path);
    }

    /**
     * 工作站成型外观的完整面重写。
     */
    @Nullable
    public static List<BakedQuad> appearance(List<BakedQuad> quads, @Nullable Direction side,
            BlockAndTintGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CRYSTAL_WORKSTATION.get())) {
            return state.getValue(AbstractMultiblockControllerBlock.FORMED) ? controller(quads, level, pos) : null;
        }
        CrystalWorkstationShell.Look look = CrystalWorkstationShell.look(pos);
        // 与面板或成型主机相邻的机壳并入工作站外观
        boolean shell = look == null && state.is(ModBlocks.REINFORCED_HIGH_POWER_CASING.get())
                && (CrystalWorkstationShell.bordersStability(pos) || bordersController(level, pos));
        if (look == null && !shell) {
            return null;
        }
        if (look != null && look.role() == CrystalWorkstationStructure.PartRole.CORE) {
            // 核心由方块实体渲染接管
            return List.of();
        }

        boolean pane = look != null && look.role() == CrystalWorkstationStructure.PartRole.GLASS;
        // 玻璃只保留朝外一面，内侧面不再渲染
        if (pane && side == look.outward().getOpposite()) {
            return List.of();
        }
        boolean panel = look != null && look.role() == CrystalWorkstationStructure.PartRole.STABILITY;
        boolean outer = side == null || (look != null && side == look.outward());
        // 面板外侧换成强化高能外壳，内侧换成独立的 3x3 面板纹理
        if (panel) {
            quads = retargetPanel(quads, outer, look);
        }

        int worldMask;
        if (panel) {
            // 外侧与机壳连成一片，内侧只在 3x3 面板内部相连
            worldMask = outer ? CtmConfig.mask(level, pos, CrystalWorkstationCtm::connectsShell)
                    : CtmConfig.mask(level, pos,
                            (neighbor, neighborState) -> CrystalWorkstationShell.isStability(neighbor));
        } else if (shell) {
            worldMask = CtmConfig.mask(level, pos, CrystalWorkstationCtm::connectsShell);
        } else {
            worldMask = CtmConfig.worldMask(level, pos, state);
        }
        List<BakedQuad> result = rewrite(quads, worldMask);
        if (!pane) {
            return result;
        }
        // 单层玻璃补一份反向绕序，隔着正面玻璃仍能看到另一侧玻璃的贴图
        List<BakedQuad> doubled = new ArrayList<>(result.size() * 2);
        for (BakedQuad quad : result) {
            doubled.add(quad);
            doubled.add(flipped(quad));
        }
        return doubled;
    }

    private static List<BakedQuad> controller(List<BakedQuad> quads, BlockAndTintGetter level, BlockPos pos) {
        TextureAtlasSprite base = CtmSprites.baseSprite(CtmSprites.CRYSTAL_WORKSTATION_CONTROLLER_CASING);
        TextureAtlasSprite facade = CtmSprites.baseSprite(MachinePartFacade.HIGH_POWER.texture());
        if (base != null && facade != null && base != facade) {
            quads = retarget(quads, base, facade);
        }
        return rewrite(quads, CtmConfig.mask(level, pos, CrystalWorkstationCtm::connectsShell));
    }

    private static List<BakedQuad> rewrite(List<BakedQuad> quads, int worldMask) {
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            TextureAtlasSprite base = quad.getSprite();
            TextureAtlasSprite atlas = CtmSprites.atlasFor(base);
            if (atlas == null) {
                result.add(quad);
            } else if (CtmSprites.isQuadrant(base)) {
                result.addAll(QuadrantCtmMeshBuilder.build(quad, base, atlas, worldMask));
            } else {
                result.add(CtmMeshBuilder.rewrite(quad, base, atlas, worldMask));
            }
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

    private static List<BakedQuad> retargetPanel(List<BakedQuad> quads, boolean outer,
            CrystalWorkstationShell.Look look) {
        TextureAtlasSprite target = CtmSprites
                .baseSprite(outer ? MachinePartFacade.HIGH_POWER.texture() : CtmSprites.WORKSTATION_PANEL_INNER);
        if (target == null) {
            return quads;
        }
        List<BakedQuad> result = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (!STABILITY_SKIN.equals(quad.getSprite().contents().name())) {
                result.add(quad);
                continue;
            }
            result.add(outer ? CtmMeshBuilder.retarget(quad, quad.getSprite(), target)
                    : panelRegion(quad, target, look));
        }
        return result;
    }

    /**
     * 按方块在 3x3 面板中的位置取样整块面板贴图，使九格拼成一个连续图案。
     * 顶点是方块局部坐标（0~1），直接用它与面板内序号拼出整版面 UV。
     */
    private static BakedQuad panelRegion(BakedQuad quad, TextureAtlasSprite sprite, CrystalWorkstationShell.Look look) {
        int size = CrystalWorkstationStructure.PANEL_SIZE;
        Direction depth = look.depthAxis();
        int[] src = quad.getVertices();
        int[] out = src.clone();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        for (int i = 0; i < 4; i++) {
            int offset = i * STRIDE;
            float x = Float.intBitsToFloat(src[offset]);
            float y = Float.intBitsToFloat(src[offset + 1]);
            float z = Float.intBitsToFloat(src[offset + 2]);
            // 面内横向沿结构深度轴，纵向自面板顶端向下
            float across = depth.getStepX() != 0
                    ? (depth.getStepX() > 0 ? x : 1.0F - x)
                    : (depth.getStepZ() > 0 ? z : 1.0F - z);
            float down = 1.0F - y;
            float u = (look.panelX() + across) / size;
            float v = (look.panelY() + down) / size;
            int uv = i * STRIDE + UV_OFFSET;
            out[uv] = Float.floatToRawIntBits(u0 + u * (u1 - u0));
            out[uv + 1] = Float.floatToRawIntBits(v0 + v * (v1 - v0));
        }
        return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade());
    }

    private static BakedQuad flipped(BakedQuad quad) {
        int[] src = quad.getVertices();
        int[] out = src.clone();
        System.arraycopy(src, STRIDE, out, STRIDE * 3, STRIDE);
        System.arraycopy(src, STRIDE * 3, out, STRIDE, STRIDE);
        return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    private static boolean bordersController(BlockAndTintGetter level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(ModBlocks.CRYSTAL_WORKSTATION.get())
                    && neighbor.getValue(AbstractMultiblockControllerBlock.FORMED)) {
                return true;
            }
        }
        return false;
    }

    private static boolean connectsShell(BlockPos neighbor, BlockState neighborState) {
        if (CrystalWorkstationShell.isStability(neighbor)) {
            return true;
        }
        if (neighborState.is(ModBlocks.CRYSTAL_WORKSTATION.get())) {
            return neighborState.getValue(AbstractMultiblockControllerBlock.FORMED);
        }
        return CtmConfig.connects(neighborState, ModBlocks.REINFORCED_HIGH_POWER_CASING.get().defaultBlockState());
    }
}
