package com.murthinext.ae2pr;

import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

import appeng.blockentity.misc.ChargerBlockEntity;
import com.murthinext.ae2pr.api.EnergyAmount;

public final class ModConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue MAX_ROUNDS;
    private static final ForgeConfigSpec.IntValue RETRY_COUNT;
    private static final ForgeConfigSpec.IntValue RETRY_INTERVAL_TICKS;
    private static final ForgeConfigSpec.BooleanValue NOTIFY_ON_FAILURE;

    private static final ForgeConfigSpec.IntValue REQUESTER_SLOTS;
    private static final ForgeConfigSpec.DoubleValue REQUESTER_IDLE_ENERGY;
    private static final ForgeConfigSpec.BooleanValue REQUESTER_REQUIRE_CHANNEL;

    private static final ForgeConfigSpec.BooleanValue ASSEMBLY_ITEMS_ORDERED;
    private static final ForgeConfigSpec.BooleanValue ASSEMBLY_FLUIDS_ORDERED;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_BASE_PARALLEL;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_PARALLEL_PER_SLICE;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_MAX_SLICES;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_MODULE_PARALLEL_PER_UNIT;
    private static final ForgeConfigSpec.DoubleValue ASSEMBLY_MODULE_SPEED_PER_UNIT;

    private static final ForgeConfigSpec.DoubleValue NAMING_FACTORY_MAX_POWER;
    private static final ForgeConfigSpec.DoubleValue NAMING_FACTORY_ENERGY_PER_ITEM;

    private static final ForgeConfigSpec.IntValue HIGH_REACTIVITY_LAVA_SMELTER_BASE_PARALLEL;
    private static final ForgeConfigSpec.DoubleValue HIGH_REACTIVITY_LAVA_SMELTER_PASSIVE_ENERGY;
    private static final ForgeConfigSpec.DoubleValue HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_LOSS_CHANCE;
    private static final ForgeConfigSpec.IntValue HIGH_REACTIVITY_LAVA_SMELTER_MAX_DURABILITY;
    private static final ForgeConfigSpec.IntValue HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_PER_DUST;

    private static final ForgeConfigSpec.IntValue MATTER_PHASE_TRANSFORMER_BASE_PARALLEL;

    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTION_BASE_PARALLEL;
    private static final ForgeConfigSpec.IntValue CRYSTAL_WORKSTATION_BASE_PARALLEL;

    private static final ForgeConfigSpec.IntValue JIG_BASE_PARALLEL;

    private static final ForgeConfigSpec.IntValue FILTER_PRESS_BASE_PARALLEL;

    private static final ForgeConfigSpec.IntValue MULTI_FACTORY_BASE_PARALLEL;
    private static final ForgeConfigSpec.IntValue MULTI_FACTORY_INSCRIBING_DURATION;
    private static final ForgeConfigSpec.IntValue MULTI_FACTORY_CHARGING_DURATION;
    private static final ForgeConfigSpec.DoubleValue MULTI_FACTORY_INSCRIBING_ENERGY;
    private static final ForgeConfigSpec.DoubleValue MULTI_FACTORY_CHARGING_ENERGY;

    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_RADIUS;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_DEPTH;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_DURATION_TICKS;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_SOUNDS_PER_TICK;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_FLAWLESS_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_FLAWED_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_CHIPPED_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_DAMAGED_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_AMETHYST_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_CERTUS_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_ENERGIZED_WEIGHT;
    private static final ForgeConfigSpec.IntValue CRYSTAL_REACTOR_OVERLOAD_WEIGHT;

    private static final ForgeConfigSpec.DoubleValue LOSSY_SPEED_PER_CARD;
    private static final ForgeConfigSpec.DoubleValue LOSSY_ENERGY_MULTIPLIER_PER_CARD;

    private static final ForgeConfigSpec.BooleanValue COLLAPSE_ENABLED;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> COLLAPSE_EXTRA_BLOCKS;
    private static final ForgeConfigSpec.LongValue COLLAPSE_THRESHOLD;

    static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("repeatOrder");
        MAX_ROUNDS = BUILDER
                .comment("单次重复下单允许的最大轮数")
                .defineInRange("maxRounds", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        RETRY_COUNT = BUILDER
                .comment("开始下一轮失败后的重试次数")
                .defineInRange("retryCount", 3, 0, 100);
        RETRY_INTERVAL_TICKS = BUILDER
                .comment("相邻两次重试之间的间隔（ticks）")
                .defineInRange("retryIntervalTicks", 20, 1, 72000);
        NOTIFY_ON_FAILURE = BUILDER
                .comment("重复订单失败时是否发送无线终端通知")
                .define("notifyOnFailure", true);
        BUILDER.pop();

        BUILDER.push("redstoneRequester");
        REQUESTER_SLOTS = BUILDER
                .comment("ME 红石请求器可标记的物品数量")
                .defineInRange("slots", 5, 1, 63);
        REQUESTER_IDLE_ENERGY = BUILDER
                .comment("ME 红石请求器空闲时从网络抽取的能量（AE）")
                .defineInRange("idleEnergy", 5.0, 0.0, EnergyAmount.MAX);
        REQUESTER_REQUIRE_CHANNEL = BUILDER
                .comment("ME 红石请求器是否需要占用一个网络频道")
                .define("requireChannel", true);
        BUILDER.pop();

        BUILDER.push("crystalAssemblyLine");
        ASSEMBLY_ITEMS_ORDERED = BUILDER
                .comment("水晶装配线物品输入是否有序")
                .define("itemInputsOrdered", true);
        ASSEMBLY_FLUIDS_ORDERED = BUILDER
                .comment("水晶装配线流体输入是否有序")
                .define("fluidInputsOrdered", false);
        ASSEMBLY_BASE_PARALLEL = BUILDER
                .comment("水晶装配线最短结构的默认并行数")
                .defineInRange("baseParallel", 16, 1, Integer.MAX_VALUE);
        ASSEMBLY_PARALLEL_PER_SLICE = BUILDER
                .comment("结构每超出最短长度1片增加的并行数")
                .defineInRange("parallelPerSlice", 8, 0, Integer.MAX_VALUE);
        ASSEMBLY_MAX_SLICES = BUILDER
                .comment("水晶装配线结构允许的最大片数")
                .defineInRange("maxSlices", 31, 5, 1024);
        ASSEMBLY_MODULE_PARALLEL_PER_UNIT = BUILDER
                .comment("水晶装配线每个并行控制外壳增加的并行数")
                .defineInRange("moduleParallelPerUnit", 16, 0, Integer.MAX_VALUE);
        ASSEMBLY_MODULE_SPEED_PER_UNIT = BUILDER
                .comment("水晶装配线每个速度控制外壳增加的速度比例")
                .defineInRange("moduleSpeedPerUnit", 0.5, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("namingFactory");
        NAMING_FACTORY_MAX_POWER = BUILDER
                .comment("名称压印工厂的内部能量缓存上限（AE）")
                .defineInRange("maxPower", 16000.0, 0.0, EnergyAmount.MAX);
        NAMING_FACTORY_ENERGY_PER_ITEM = BUILDER
                .comment("名称压印工厂每重命名一个物品消耗的能量（AE）")
                .defineInRange("energyPerItem", 250.0, 0.0, EnergyAmount.MAX);
        BUILDER.pop();

        BUILDER.push("highReactivityLavaSmelter");
        HIGH_REACTIVITY_LAVA_SMELTER_BASE_PARALLEL = BUILDER
                .comment("熔岩冶炼炉默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        HIGH_REACTIVITY_LAVA_SMELTER_PASSIVE_ENERGY = BUILDER
                .comment("熔岩冶炼炉被动耗电量（AE/tick）")
                .defineInRange("passiveEnergyPerTick", 1000.0, 0.0, EnergyAmount.MAX);
        HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_LOSS_CHANCE = BUILDER
                .comment("熔岩冶炼炉电力供给不足时每秒扣除 1 点配方耐久的概率")
                .defineInRange("durabilityLossChancePerSecond", 0.5, 0.0, 1.0);
        HIGH_REACTIVITY_LAVA_SMELTER_MAX_DURABILITY = BUILDER
                .comment("熔岩冶炼炉的配方耐久上限")
                .defineInRange("maxDurability", 128, 1, Integer.MAX_VALUE);
        HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_PER_DUST = BUILDER
                .comment("每份陨石粉补充的配方耐久")
                .defineInRange("durabilityPerDust", 32, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("matterPhaseTransformer");
        MATTER_PHASE_TRANSFORMER_BASE_PARALLEL = BUILDER
                .comment("物质相变仪默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("crystalReactionChamber");
        CRYSTAL_REACTION_BASE_PARALLEL = BUILDER
                .comment("水晶反应仓默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("crystalWorkstation");
        CRYSTAL_WORKSTATION_BASE_PARALLEL = BUILDER
                .comment("晶能工作站默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("jig");
        JIG_BASE_PARALLEL = BUILDER
                .comment("跳汰机默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("filterPress");
        FILTER_PRESS_BASE_PARALLEL = BUILDER
                .comment("压滤机默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("multiFactory");
        MULTI_FACTORY_BASE_PARALLEL = BUILDER
                .comment("通式加工厂默认并行数")
                .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        MULTI_FACTORY_INSCRIBING_DURATION = BUILDER
                .comment("通式加工厂压印模式单次作业时长（ticks）")
                .defineInRange("inscribingDuration", 100, 1, 72000);
        MULTI_FACTORY_CHARGING_DURATION = BUILDER
                .comment("通式加工厂充能模式单次作业时长（ticks）")
                .defineInRange("chargingDuration", 50, 1, 72000);
        MULTI_FACTORY_INSCRIBING_ENERGY = BUILDER
                .comment("通式加工厂压印模式每个并行默认扣除的能量（AE）")
                .defineInRange("inscribingEnergyPerParallel", 2000.0, 0.0, EnergyAmount.MAX);
        MULTI_FACTORY_CHARGING_ENERGY = BUILDER
                .comment("通式加工厂充能模式每个并行默认扣除的能量（AE）")
                .defineInRange("chargingEnergyPerParallel",
                        ChargerBlockEntity.POWER_MAXIMUM_AMOUNT, 0.0, EnergyAmount.MAX);
        BUILDER.pop();

        BUILDER.push("crystalReactor");
        CRYSTAL_REACTOR_RADIUS = BUILDER
                .comment("水晶反应器以核心为圆心的水平替换半径")
                .defineInRange("radius", 8, 1, 64);
        CRYSTAL_REACTOR_DEPTH = BUILDER
                .comment("水晶反应器以核心为顶面的向下替换层数")
                .defineInRange("depth", 4, 0, 64);
        CRYSTAL_REACTOR_DURATION_TICKS = BUILDER
                .comment("水晶反应器的总扩散耗时（ticks）")
                .defineInRange("durationTicks", 160, 1, 72000);
        CRYSTAL_REACTOR_SOUNDS_PER_TICK = BUILDER
                .comment("水晶反应器每 tick 最多播放的方块破坏音效数量")
                .defineInRange("breakSoundsPerTick", 2, 0, 64);
        CRYSTAL_REACTOR_FLAWLESS_WEIGHT = BUILDER
                .comment("水晶反应器无瑕档位权重")
                .defineInRange("flawlessWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_FLAWED_WEIGHT = BUILDER
                .comment("水晶反应器有瑕档位权重")
                .defineInRange("flawedWeight", 4, 0, 1_000_000);
        CRYSTAL_REACTOR_CHIPPED_WEIGHT = BUILDER
                .comment("水晶反应器开裂档位权重")
                .defineInRange("chippedWeight", 8, 0, 1_000_000);
        CRYSTAL_REACTOR_DAMAGED_WEIGHT = BUILDER
                .comment("水晶反应器损坏档位权重")
                .defineInRange("damagedWeight", 32, 0, 1_000_000);
        CRYSTAL_REACTOR_AMETHYST_WEIGHT = BUILDER
                .comment("水晶反应器紫水晶母岩（恒为无瑕）的材质权重")
                .defineInRange("amethystWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_CERTUS_WEIGHT = BUILDER
                .comment("水晶反应器赛特斯石英母岩的材质权重")
                .defineInRange("certusWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_ENERGIZED_WEIGHT = BUILDER
                .comment("水晶反应器 Neo ECO AE Extension 盈能水晶母岩的材质权重")
                .defineInRange("energizedWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_OVERLOAD_WEIGHT = BUILDER
                .comment("水晶反应器 AE2 Lightning Tech 过载水晶母岩的材质权重")
                .defineInRange("overloadWeight", 1, 0, 1_000_000);
        BUILDER.pop();

        BUILDER.push("upgradeCards");
        LOSSY_SPEED_PER_CARD = BUILDER
                .comment("多方块机器每张加速卡提供的速度加成，加法叠加")
                .defineInRange("lossySpeedPerCard", 0.5, 0.0, 100.0);
        LOSSY_ENERGY_MULTIPLIER_PER_CARD = BUILDER
                .comment("多方块机器每张加速卡对单次作业耗能的倍率，逐张相乘")
                .defineInRange("lossyEnergyMultiplierPerCard", 4.0, 1.0, 1000.0);
        BUILDER.pop();

        BUILDER.push("collapse");
        COLLAPSE_ENABLED = BUILDER
                .comment("是否允许机器被破坏时把超量内容压成坍缩物品元件",
                        "开启后默认兼容本模组仓室与 AE 系列（接口、样板供应器等使用 AE2 通用库存的机器）")
                .define("enableCollapse", true);
        COLLAPSE_EXTRA_BLOCKS = BUILDER
                .comment("额外允许坍缩的方块，格式为 modid:itemid 或 modid:*",
                        "为这些方块读取 Forge 物品能力中的内容；modid:* 表示该模组全部方块")
                .defineListAllowEmpty("extraEnableCollapse", List.of(),
                        entry -> entry instanceof String text && !text.isBlank());
        COLLAPSE_THRESHOLD = BUILDER
                .comment("触发坍缩的物品数量阈值")
                .defineInRange("threshold", 32768L, 1L, Long.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ModConfig() {
    }

    public static int maxRounds() {
        return MAX_ROUNDS.get();
    }

    public static int retryCount() {
        return RETRY_COUNT.get();
    }

    public static int retryIntervalTicks() {
        return RETRY_INTERVAL_TICKS.get();
    }

    public static boolean notifyOnFailure() {
        return NOTIFY_ON_FAILURE.get();
    }

    public static int requesterSlots() {
        return REQUESTER_SLOTS.get();
    }

    public static double requesterIdleEnergy() {
        return REQUESTER_IDLE_ENERGY.get();
    }

    public static boolean requesterRequireChannel() {
        return REQUESTER_REQUIRE_CHANNEL.get();
    }

    public static boolean assemblyItemsOrdered() {
        return ASSEMBLY_ITEMS_ORDERED.get();
    }

    public static boolean assemblyFluidsOrdered() {
        return ASSEMBLY_FLUIDS_ORDERED.get();
    }

    public static int assemblyBaseParallel() {
        return ASSEMBLY_BASE_PARALLEL.get();
    }

    public static int assemblyParallelPerSlice() {
        return ASSEMBLY_PARALLEL_PER_SLICE.get();
    }

    public static int crystalAssemblyLineMaxSlices() {
        return ASSEMBLY_MAX_SLICES.get();
    }

    public static int assemblyModuleParallelPerUnit() {
        return ASSEMBLY_MODULE_PARALLEL_PER_UNIT.get();
    }

    public static double assemblyModuleSpeedPerUnit() {
        return ASSEMBLY_MODULE_SPEED_PER_UNIT.get();
    }

    public static double namingFactoryMaxPower() {
        return NAMING_FACTORY_MAX_POWER.get();
    }

    public static double namingFactoryEnergyPerItem() {
        return NAMING_FACTORY_ENERGY_PER_ITEM.get();
    }

    public static int highReactivityLavaSmelterBaseParallel() {
        return HIGH_REACTIVITY_LAVA_SMELTER_BASE_PARALLEL.get();
    }

    public static double highReactivityLavaSmelterPassiveEnergy() {
        return HIGH_REACTIVITY_LAVA_SMELTER_PASSIVE_ENERGY.get();
    }

    public static double highReactivityLavaSmelterDurabilityLossChance() {
        return HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_LOSS_CHANCE.get();
    }

    public static int highReactivityLavaSmelterMaxDurability() {
        return HIGH_REACTIVITY_LAVA_SMELTER_MAX_DURABILITY.get();
    }

    public static int highReactivityLavaSmelterDurabilityPerDust() {
        return HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_PER_DUST.get();
    }

    public static int matterPhaseTransformerBaseParallel() {
        return MATTER_PHASE_TRANSFORMER_BASE_PARALLEL.get();
    }

    public static int crystalReactionBaseParallel() {
        return CRYSTAL_REACTION_BASE_PARALLEL.get();
    }

    public static int crystalWorkstationBaseParallel() {
        return CRYSTAL_WORKSTATION_BASE_PARALLEL.get();
    }

    public static int jigBaseParallel() {
        return JIG_BASE_PARALLEL.get();
    }

    public static int filterPressBaseParallel() {
        return FILTER_PRESS_BASE_PARALLEL.get();
    }

    public static int multiFactoryBaseParallel() {
        return MULTI_FACTORY_BASE_PARALLEL.get();
    }

    public static int multiFactoryInscribingDuration() {
        return MULTI_FACTORY_INSCRIBING_DURATION.get();
    }

    public static int multiFactoryChargingDuration() {
        return MULTI_FACTORY_CHARGING_DURATION.get();
    }

    public static double multiFactoryInscribingEnergyPerParallel() {
        return MULTI_FACTORY_INSCRIBING_ENERGY.get();
    }

    public static double multiFactoryChargingEnergyPerParallel() {
        return MULTI_FACTORY_CHARGING_ENERGY.get();
    }

    public static int crystalReactorRadius() {
        return CRYSTAL_REACTOR_RADIUS.get();
    }

    public static int crystalReactorDepth() {
        return CRYSTAL_REACTOR_DEPTH.get();
    }

    public static int crystalReactorDurationTicks() {
        return CRYSTAL_REACTOR_DURATION_TICKS.get();
    }

    public static int crystalReactorBreakSoundsPerTick() {
        return CRYSTAL_REACTOR_SOUNDS_PER_TICK.get();
    }

    public static int[] crystalReactorTierWeights() {
        return new int[] { CRYSTAL_REACTOR_FLAWLESS_WEIGHT.get(), CRYSTAL_REACTOR_FLAWED_WEIGHT.get(),
                CRYSTAL_REACTOR_CHIPPED_WEIGHT.get(), CRYSTAL_REACTOR_DAMAGED_WEIGHT.get() };
    }

    public static int[] crystalReactorMaterialWeights() {
        return new int[] { CRYSTAL_REACTOR_AMETHYST_WEIGHT.get(), CRYSTAL_REACTOR_CERTUS_WEIGHT.get(),
                CRYSTAL_REACTOR_ENERGIZED_WEIGHT.get(), CRYSTAL_REACTOR_OVERLOAD_WEIGHT.get() };
    }

    public static double lossySpeedPerCard() {
        return LOSSY_SPEED_PER_CARD.get();
    }

    public static double lossyEnergyMultiplierPerCard() {
        return LOSSY_ENERGY_MULTIPLIER_PER_CARD.get();
    }

    public static boolean collapseEnabled() {
        return COLLAPSE_ENABLED.get();
    }

    public static List<String> collapseExtraBlocks() {
        return COLLAPSE_EXTRA_BLOCKS.get().stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    public static long collapseThreshold() {
        return COLLAPSE_THRESHOLD.get();
    }
}
