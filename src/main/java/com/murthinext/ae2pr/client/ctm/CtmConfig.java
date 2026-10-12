package com.murthinext.ae2pr.client.ctm;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberBlock;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerBlock;
import com.murthinext.ae2pr.block.meteor_steel_pipe.MeteorSteelPipeBlock;

/**
 * 连接纹理的逐方块配置。
 */
public final class CtmConfig {

    private static final Direction[][] EDGES = {
            { Direction.EAST, Direction.UP }, { Direction.EAST, Direction.DOWN },
            { Direction.WEST, Direction.UP }, { Direction.WEST, Direction.DOWN },
            { Direction.EAST, Direction.NORTH }, { Direction.EAST, Direction.SOUTH },
            { Direction.WEST, Direction.NORTH }, { Direction.WEST, Direction.SOUTH },
            { Direction.UP, Direction.NORTH }, { Direction.UP, Direction.SOUTH },
            { Direction.DOWN, Direction.NORTH }, { Direction.DOWN, Direction.SOUTH },
    };

    private static final int[][] EDGE_BIT = new int[6][6];

    static {
        for (int[] row : EDGE_BIT) {
            Arrays.fill(row, -1);
        }
        for (int i = 0; i < EDGES.length; i++) {
            EDGE_BIT[EDGES[i][0].ordinal()][EDGES[i][1].ordinal()] = i;
            EDGE_BIT[EDGES[i][1].ordinal()][EDGES[i][0].ordinal()] = i;
        }
    }

    private CtmConfig() {
    }

    public static boolean enabled(BlockState state) {
        return familyKey(state) != 0;
    }

    public static boolean connects(BlockState neighbor, BlockState self) {
        int key = familyKey(self);
        return key != 0 && key == familyKey(neighbor);
    }

    /**
     * 邻居是否可沿 {@code dir} 方向连接，管道还需轴向一致。
     */
    public static boolean connects(BlockState neighbor, BlockState self, Direction dir) {
        if (!connects(neighbor, self)) {
            return false;
        }
        if (self.getBlock() == ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()) {
            return dir.getAxis() == self.getValue(MeteorSteelPipeBlock.FACING).getAxis();
        }
        return true;
    }

    /**
     * 连接族标识：同一标识之间才连接。
     */
    private static int familyKey(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()) {
            return 1;
        }
        if (block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()
                || block == ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()) {

            return partFamily(state, 1);
        }
        if (block == ModBlocks.AEV_MACHINE_CASING.get()) {
            return 9;
        }
        if (block == ModBlocks.AEV_INPUT_BUS.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_BUS.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get()) {

            return partFamily(state, 9);
        }
        if (block == ModBlocks.CRYSTAL_GLASS.get()) {
            return 3;
        }
        if (block == ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()) {
            return 8;
        }
        if (block == ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()
                || block == ModBlocks.MATTER_PHASE_TRANSFORMER.get()
                || block == ModBlocks.CRYSTAL_REACTION_CHAMBER.get()) {
            MachinePartFacade facade = facade(state);
            return facade != null ? facade.ctmFamily() : 0;
        }
        if (block == ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()) {
            return 4;
        }
        if (block == ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()) {
            // 管道按轴向分族
            return 5 + state.getValue(MeteorSteelPipeBlock.FACING).getAxis().ordinal();
        }
        return 0;
    }

    private static int partFamily(BlockState state, int casingFamily) {
        MachinePartFacade facade = facade(state);
        return facade != null ? facade.ctmFamily() : casingFamily;
    }

    @Nullable
    public static MachinePartFacade facade(BlockState state) {
        if (state.is(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get())
                && state.getValue(HighReactivityLavaSmelterBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModBlocks.MATTER_PHASE_TRANSFORMER.get()) && state.getValue(MatterPhaseTransformerBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModBlocks.CRYSTAL_REACTION_CHAMBER.get())
                && state.getValue(CrystalReactionChamberBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL;
        }
        if (state.getBlock() instanceof MachinePartBlock && state.getValue(MachinePartBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(MachinePartBlock.STYLE));
        }
        if (state.is(ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get())
                && state.getValue(FluixCrystalEnergyHatchBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(FluixCrystalEnergyHatchBlock.STYLE));
        }
        return null;
    }

    @Nullable
    public static TextureAtlasSprite facadeSprite(BlockState state) {
        MachinePartFacade facade = facade(state);
        return facade != null ? CtmSprites.baseSprite(facade.texture()) : null;
    }

    @Nullable
    public static TextureAtlasSprite facadeBaseSprite(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()) {
            return CtmSprites.baseSprite(CtmSprites.LAVA_SMELTER_CONTROLLER_CASING);
        }
        if (block == ModBlocks.MATTER_PHASE_TRANSFORMER.get()) {
            return CtmSprites.baseSprite(CtmSprites.MATTER_PHASE_CONTROLLER_CASING);
        }
        if (block == ModBlocks.CRYSTAL_REACTION_CHAMBER.get()) {
            return CtmSprites.baseSprite(CtmSprites.CRYSTAL_REACTION_CONTROLLER_CASING);
        }
        if (block == ModBlocks.FLUIX_CRYSTAL_ENERGY_HATCH.get()) {
            // 能源仓的成型模型以水晶机壳为基础贴图
            return CtmSprites.baseSprite(state.getValue(FluixCrystalEnergyHatchBlock.FORMED)
                    ? MachinePartFacade.CRYSTAL.texture()
                    : CtmSprites.CERTUS_PART_CASING);
        }
        if (!(block instanceof MachinePartBlock)) {
            return null;
        }
        return MachinePartBlock.isAevPart(block)
                ? CtmSprites.baseSprite(CtmSprites.AEV_PART_CASING)
                : CtmSprites.baseSprite(CtmSprites.CERTUS_PART_CASING);
    }

    /**
     * 世界空间连接掩码：bit 0-5 六面，bit 6-17 十二棱。
     */
    public static int worldMask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (connects(level.getBlockState(pos.relative(dir)), state, dir)) {
                mask |= 1 << dir.ordinal();
            }
        }
        for (int i = 0; i < EDGES.length; i++) {
            if (!connected(mask, EDGES[i][0]) || !connected(mask, EDGES[i][1])) {
                continue;
            }
            if (connects(level.getBlockState(pos.relative(EDGES[i][0]).relative(EDGES[i][1])), state)) {
                mask |= 1 << (6 + i);
            }
        }
        return mask;
    }

    public static boolean connected(int worldMask, Direction dir) {
        return (worldMask & (1 << dir.ordinal())) != 0;
    }

    /**
     * 棱方向是否连接，a、b 需为不同轴的面方向。
     */
    public static boolean connected(int worldMask, Direction a, Direction b) {
        int bit = EDGE_BIT[a.ordinal()][b.ordinal()];
        return bit >= 0 && (worldMask & (1 << (6 + bit))) != 0;
    }
}
