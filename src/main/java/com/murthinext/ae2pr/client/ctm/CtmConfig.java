package com.murthinext.ae2pr.client.ctm;

import java.util.Arrays;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartFacade;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;
import com.murthinext.ae2pr.block.MeteorSteelPipeBlock;

// 连接纹理的逐方块配置
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

    // 邻居是否可沿 {@code dir} 方向连接，管道还需轴向一致
    public static boolean connects(BlockState neighbor, BlockState self, Direction dir) {
        if (!connects(neighbor, self)) {
            return false;
        }
        if (self.getBlock() == ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()) {
            return dir.getAxis() == self.getValue(MeteorSteelPipeBlock.FACING).getAxis();
        }
        return true;
    }

    // 连接族标识：同一标识之间才连接
    private static int familyKey(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()) {
            return 1;
        }
        if (block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()) {

            return partFamily(state, 1);
        }
        if (block == ModBlocks.ME_NETWORK_ACCESS_HATCH.get()) {
            // 接入仓仅成型后加入结构机壳的连接族
            return partFamily(state, 0);
        }
        if (block == ModBlocks.AEV_MACHINE_CASING.get()) {
            return 9;
        }
        if (block == ModBlocks.REINFORCED_HIGH_POWER_CASING.get()) {
            return 10;
        }
        if (MachinePartBlock.isAevPart(block)) {

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
                || block == ModBlocks.CRYSTAL_REACTION_CHAMBER.get()
                || block == ModBlocks.CRYSTAL_WORKSTATION.get()
                || block == ModBlocks.JIG.get()
                || block == ModBlocks.MULTI_FACTORY.get()
                || block == ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()) {
            MachinePartFacade facade = facade(state);
            return facade != null ? facade.ctmFamily() : 0;
        }
        if (block == ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()) {
            return 4;
        }
        if (block == ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get()) {
            return 11;
        }
        if (block == ModBlocks.ME_COMPUTER_CASING.get()) {
            return 12;
        }
        if (block == ModBlocks.ME_ADVANCED_COMPUTER_CASING.get()) {
            return 13;
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
        if (state.is(ModBlocks.JIG.get()) && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIUM_IRON;
        }
        if ((state.is(ModBlocks.CRYSTAL_WORKSTATION.get()) || state.is(ModBlocks.MULTI_FACTORY.get()))
                && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.HIGH_POWER;
        }
        if (state.is(ModBlocks.CRYSTAL_ASSEMBLY_LINE.get())
                && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL;
        }
        if (state.is(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get())
                && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModBlocks.MATTER_PHASE_TRANSFORMER.get()) && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModBlocks.CRYSTAL_REACTION_CHAMBER.get())
                && state.getValue(AbstractMultiblockControllerBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL;
        }
        if (state.getBlock() instanceof MachinePartBlock && state.getValue(MachinePartBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(MachinePartBlock.STYLE));
        }
        if (state.is(ModBlocks.ME_NETWORK_ACCESS_HATCH.get())
                && state.getValue(MENetworkAccessHatchBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(MENetworkAccessHatchBlock.STYLE));
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
        if (block == ModBlocks.MULTI_FACTORY.get()) {
            return CtmSprites.baseSprite(CtmSprites.MULTI_FACTORY_CONTROLLER_CASING);
        }
        if (block == ModBlocks.JIG.get()) {
            return CtmSprites.baseSprite(CtmSprites.JIG_CONTROLLER_CASING);
        }
        if (block == ModBlocks.CRYSTAL_WORKSTATION.get()) {
            return CtmSprites.baseSprite(CtmSprites.CRYSTAL_WORKSTATION_CONTROLLER_CASING);
        }
        if (block == ModBlocks.CRYSTAL_ASSEMBLY_LINE.get()) {
            return CtmSprites.baseSprite(CtmSprites.CRYSTAL_ASSEMBLY_LINE_CONTROLLER_CASING);
        }
        if (block == ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()) {
            return CtmSprites.baseSprite(CtmSprites.LAVA_SMELTER_CONTROLLER_CASING);
        }
        if (block == ModBlocks.MATTER_PHASE_TRANSFORMER.get()) {
            return CtmSprites.baseSprite(CtmSprites.MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING);
        }
        if (block == ModBlocks.CRYSTAL_REACTION_CHAMBER.get()) {
            return CtmSprites.baseSprite(CtmSprites.CRYSTAL_REACTION_CONTROLLER_CASING);
        }
        if (block == ModBlocks.ME_NETWORK_ACCESS_HATCH.get()) {
            // 接入仓成型外壳以水晶强化复合机壳为基础贴图
            return CtmSprites.baseSprite(MachinePartFacade.CRYSTAL.texture());
        }
        if (!(block instanceof MachinePartBlock)) {
            return null;
        }
        return MachinePartBlock.isAevPart(block)
                ? CtmSprites.baseSprite(CtmSprites.AEV_PART_CASING)
                : CtmSprites.baseSprite(MachinePartFacade.CRYSTAL.texture());
    }

    // 世界空间连接掩码：bit 0-5 六面，bit 6-17 十二棱
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

    /**
     * 以自定义连接判定计算掩码，用于跨方块族的成型外观。
     *
     * @param joined 判定某个邻居位置是否与本方块相连
     */
    public static int mask(BlockAndTintGetter level, BlockPos pos, BiPredicate<BlockPos, BlockState> joined) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (joined.test(neighbor, level.getBlockState(neighbor))) {
                mask |= 1 << dir.ordinal();
            }
        }
        for (int i = 0; i < EDGES.length; i++) {
            if (!connected(mask, EDGES[i][0]) || !connected(mask, EDGES[i][1])) {
                continue;
            }
            BlockPos neighbor = pos.relative(EDGES[i][0]).relative(EDGES[i][1]);
            if (joined.test(neighbor, level.getBlockState(neighbor))) {
                mask |= 1 << (6 + i);
            }
        }
        return mask;
    }

    public static boolean connected(int worldMask, Direction dir) {
        return (worldMask & (1 << dir.ordinal())) != 0;
    }

    // 棱方向是否连接，a、b 需为不同轴的面方向
    public static boolean connected(int worldMask, Direction a, Direction b) {
        int bit = EDGE_BIT[a.ordinal()][b.ordinal()];
        return bit >= 0 && (worldMask & (1 << (6 + bit))) != 0;
    }
}
