package com.murthinext.ae2pr.block.crystal_reactor;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.Config;

/**
 * 水晶母岩候选表：按档位与材质解析实际方块，可选模组未安装时自动剔除。
 */
public final class CrystalBuddingBlocks {

    // 档位索引，与 crystalReactorTierWeights 顺序一致
    public static final int TIER_FLAWLESS = 0;
    public static final int TIER_FLAWED = 1;
    public static final int TIER_CHIPPED = 2;
    public static final int TIER_DAMAGED = 3;

    // 材质索引，与 crystalReactorMaterialWeights 顺序一致
    private static final int MATERIAL_AMETHYST = 0;
    private static final int MATERIAL_CERTUS = 1;
    private static final int MATERIAL_ENERGIZED = 2;
    private static final int MATERIAL_OVERLOAD = 3;
    private static final int MATERIAL_COUNT = 4;
    private static final int TIER_COUNT = 4;

    private static final String NEOECOAE = "neoecoae";
    private static final String AE2LT = "ae2lt";

    @Nullable
    private static Block[][] table;

    private CrystalBuddingBlocks() {
    }

    /**
     * 按配置权重掷出一次母岩：先定档位，再在该档位可用材质间挑选。
     */
    public static BlockState roll(RandomSource random) {
        Block[][] materials = resolve();
        int[] materialWeights = Config.crystalReactorMaterialWeights();
        int tier = pickTier(random);
        int total = 0;
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            if (materials[material][tier] != null) {
                total += materialWeights[material];
            }
        }
        if (total <= 0) {
            // 全部权重为 0 时兜底无瑕赛特斯
            return materials[MATERIAL_CERTUS][TIER_FLAWLESS].defaultBlockState();
        }
        int roll = random.nextInt(total);
        for (int material = 0; material < MATERIAL_COUNT; material++) {
            Block block = materials[material][tier];
            if (block == null) {
                continue;
            }
            roll -= materialWeights[material];
            if (roll < 0) {
                return block.defaultBlockState();
            }
        }
        return materials[MATERIAL_CERTUS][TIER_FLAWLESS].defaultBlockState();
    }

    private static int pickTier(RandomSource random) {
        int[] weights = Config.crystalReactorTierWeights();
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        if (total <= 0) {
            return TIER_DAMAGED;
        }
        int roll = random.nextInt(total);
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            roll -= weights[tier];
            if (roll < 0) {
                return tier;
            }
        }
        return TIER_DAMAGED;
    }

    private static Block[][] resolve() {
        Block[][] cached = table;
        if (cached != null) {
            return cached;
        }
        Block[][] materials = new Block[MATERIAL_COUNT][TIER_COUNT];
        materials[MATERIAL_AMETHYST][TIER_FLAWLESS] = Blocks.BUDDING_AMETHYST;
        materials[MATERIAL_CERTUS][TIER_FLAWLESS] = AEBlocks.FLAWLESS_BUDDING_QUARTZ.block();
        materials[MATERIAL_CERTUS][TIER_FLAWED] = AEBlocks.FLAWED_BUDDING_QUARTZ.block();
        materials[MATERIAL_CERTUS][TIER_CHIPPED] = AEBlocks.CHIPPED_BUDDING_QUARTZ.block();
        materials[MATERIAL_CERTUS][TIER_DAMAGED] = AEBlocks.DAMAGED_BUDDING_QUARTZ.block();
        if (ModList.get().isLoaded(NEOECOAE)) {
            materials[MATERIAL_ENERGIZED][TIER_FLAWLESS] = block("neoecoae:flawless_budding_energized_crystal");
            materials[MATERIAL_ENERGIZED][TIER_FLAWED] = block("neoecoae:flawed_budding_energized_crystal");
            materials[MATERIAL_ENERGIZED][TIER_CHIPPED] = block("neoecoae:chipped_budding_energized_crystal");
            materials[MATERIAL_ENERGIZED][TIER_DAMAGED] = block("neoecoae:damaged_budding_energized_crystal");
        }
        if (ModList.get().isLoaded(AE2LT)) {
            materials[MATERIAL_OVERLOAD][TIER_FLAWLESS] = block("ae2lt:flawless_budding_overload_crystal");
            materials[MATERIAL_OVERLOAD][TIER_FLAWED] = block("ae2lt:flawed_budding_overload_crystal");
            materials[MATERIAL_OVERLOAD][TIER_CHIPPED] = block("ae2lt:cracked_budding_overload_crystal");
            materials[MATERIAL_OVERLOAD][TIER_DAMAGED] = block("ae2lt:damaged_budding_overload_crystal");
        }
        table = materials;
        return materials;
    }

    @Nullable
    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElse(null);
    }
}
