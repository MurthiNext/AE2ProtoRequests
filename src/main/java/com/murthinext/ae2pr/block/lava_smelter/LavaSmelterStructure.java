package com.murthinext.ae2pr.block.lava_smelter;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.multiblock.MultiblockPattern;
import com.murthinext.ae2pr.multiblock.RelativeDirection;
import com.murthinext.ae2pr.multiblock.StructurePredicate;

/**
 * 高反应性熔岩冶炼炉结构定义。
 * <p>
 * 模式按 {@code misc/high_reactivity_lava_smelter.nbt} 逐片照抄（木桶位替换为主机），
 * 参考坐标系与装配线指南 NBT 相同：主机朝东时，NBT 的 +z 为"'主机右侧'、-x 为"主机后方"。
 * <ul>
 * <li><b>片（slice）</b>：沿控制器右侧延伸，共 4 片，第 0 片含主机</li>
 * <li><b>行（string）</b>：沿上递增（第 0 行是最底行），共 8 行</li>
 * <li><b>列（char）</b>：沿控制器背面递增（前→后深度），共 4 列</li>
 * </ul>
 * 字符含义：{@code C}=主机（原 NBT 木桶位） {@code B}=锆刚玉砖块 {@code G}=耐火水晶玻璃
 * {@code P}=陨钢管道（NBT 中炉膛顶朝下、烟囱朝上，结构只校验方块类型）
 * {@code L}=物流控制机械方块 {@code #}=任意（炉膛内部与烟囱周围留空）。
 */
public final class LavaSmelterStructure {

    /** 结构模式：主机位于第 0 行、第 0 列。 */
    public static final MultiblockPattern PATTERN = MultiblockPattern
            .builder(RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.BACK)
            .origin(0, 0)
            .slice("CBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#PB#", "#P##", "#P##")
            .slice("BBBB", "G##B", "G##B", "BPBB", "BLLB", "#BB#", "####", "####")
            .slice("BBBB", "BBBB", "BBBB", "BBBB", "#BB#", "####", "####", "####")
            .where('C', StructurePredicate.blocks(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get()))
            .where('B', StructurePredicate.blocks(ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()))
            .where('G', StructurePredicate.blocks(ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()))
            .where('P', StructurePredicate.blocks(ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()))
            .where('L', StructurePredicate.blocks(ModBlocks.LOGISTICS_CONTROL_CASING.get()))
            .where('#', StructurePredicate.any())
            .build();

    private LavaSmelterStructure() {
    }
}
