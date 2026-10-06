package com.murthinext.ae2pr.block.meteorite;

import java.util.List;

import com.murthinext.ae2pr.ModBlocks;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * 陨石矿石的随机选取与采集速度处理。
 */
public final class MeteoriteOres {

    private record OreEntry(RegistryObject<MeteoriteOreBlock> block, int weight) {
    }

    // 原版稀有度权重，总和 100
    private static final List<OreEntry> VANILLA_ORES = List.of(
            new OreEntry(ModBlocks.METEORITE_IRON_ORE, 30),
            new OreEntry(ModBlocks.METEORITE_COPPER_ORE, 30),
            new OreEntry(ModBlocks.METEORITE_GOLD_ORE, 15),
            new OreEntry(ModBlocks.METEORITE_LAPIS_ORE, 15),
            new OreEntry(ModBlocks.METEORITE_DIAMOND_ORE, 5),
            new OreEntry(ModBlocks.METEORITE_EMERALD_ORE, 5));

    private static final int WEIGHT_SUM = VANILLA_ORES.stream().mapToInt(OreEntry::weight).sum();

    private MeteoriteOres() {
    }

    /**
     * 随机选取一种矿石，锆英石与原版矿石约为 1:1。
     */
    public static BlockState randomOre(RandomSource random) {
        if (random.nextFloat() < 0.5F) {
            return ModBlocks.METEORITE_ZIRCON_ORE.get().defaultBlockState();
        }
        int roll = random.nextInt(WEIGHT_SUM);
        for (var entry : VANILLA_ORES) {
            roll -= entry.weight();
            if (roll < 0) {
                return entry.block().get().defaultBlockState();
            }
        }
        return ModBlocks.METEORITE_IRON_ORE.get().defaultBlockState();
    }

    /**
     * 与天空石一致：使用铁镐以上工具挖掘时速度提升 10 倍。
     */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getState().getBlock() instanceof MeteoriteOreBlock)) {
            return;
        }
        ItemStack tool = event.getEntity().getItemBySlot(EquipmentSlot.MAINHAND);
        if (tool.getDestroySpeed(event.getState()) > Tiers.IRON.getSpeed()) {
            event.setNewSpeed(event.getNewSpeed() * 10.0F);
        }
    }
}
