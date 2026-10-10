package com.murthinext.ae2pr;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置文件。
 */
public final class Config {
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
    private static final ForgeConfigSpec.DoubleValue ASSEMBLY_ENERGY_PER_PARALLEL;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_MODULE_PARALLEL_PER_UNIT;
    private static final ForgeConfigSpec.DoubleValue ASSEMBLY_MODULE_SPEED_PER_UNIT;
    private static final ForgeConfigSpec.IntValue ASSEMBLY_MAX_PARALLEL;
    private static final ForgeConfigSpec.DoubleValue ASSEMBLY_MAX_SPEED_MULTIPLIER;

    private static final ForgeConfigSpec.DoubleValue NAMING_FACTORY_MAX_POWER;
    private static final ForgeConfigSpec.DoubleValue NAMING_FACTORY_ENERGY_PER_ITEM;

    private static final ForgeConfigSpec.IntValue LAVA_SMELTER_PARALLEL;

    static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("repeatOrder");
        MAX_ROUNDS = BUILDER
                .comment("单次重复下单允许的最大轮数")
                .defineInRange("maxRounds", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        RETRY_COUNT = BUILDER
                .comment("开始下一轮失败后的重试次数（每次重试会重新模拟计划）")
                .defineInRange("retryCount", 3, 0, 100);
        RETRY_INTERVAL_TICKS = BUILDER
                .comment("相邻两次重试之间的间隔（tick）")
                .defineInRange("retryIntervalTicks", 20, 1, 72000);
        NOTIFY_ON_FAILURE = BUILDER
                .comment("重复订单失败时是否发送无线终端通知")
                .define("notifyOnFailure", true);
        BUILDER.pop();

        BUILDER.push("redstoneRequester");
        REQUESTER_SLOTS = BUILDER
                .comment("单个 ME 红石请求器可标记的物品数量")
                .defineInRange("slots", 5, 1, 63);
        REQUESTER_IDLE_ENERGY = BUILDER
                .comment("ME 红石请求器空闲时从网络抽取的能量（AE）")
                .defineInRange("idleEnergy", 5.0, 0.0, Double.MAX_VALUE);
        REQUESTER_REQUIRE_CHANNEL = BUILDER
                .comment("ME 红石请求器是否需要占用一个网络频道")
                .define("requireChannel", true);
        BUILDER.pop();

        BUILDER.push("crystalAssemblyLine");
        ASSEMBLY_ITEMS_ORDERED = BUILDER
                .comment("物品输入是否有序")
                .define("itemInputsOrdered", true);
        ASSEMBLY_FLUIDS_ORDERED = BUILDER
                .comment("流体输入是否有序")
                .define("fluidInputsOrdered", false);
        ASSEMBLY_BASE_PARALLEL = BUILDER
                .comment("最短结构（5 片）的最大并行数")
                .defineInRange("baseParallel", 16, 1, 1_000_000);
        ASSEMBLY_PARALLEL_PER_SLICE = BUILDER
                .comment("结构每超出最短长度 1 片增加的最大并行数")
                .defineInRange("parallelPerSlice", 8, 0, 1_000_000);
        ASSEMBLY_MAX_SLICES = BUILDER
                .comment("结构允许的最大片数（最长长度，默认 31 格）")
                .defineInRange("maxSlices", 31, 5, 1024);
        ASSEMBLY_ENERGY_PER_PARALLEL = BUILDER
                .comment("每并行一次执行从 ME 网络扣除的能量")
                .defineInRange("energyPerParallel", 50000.0, 0.0, Double.MAX_VALUE);
        ASSEMBLY_MODULE_PARALLEL_PER_UNIT = BUILDER
                .comment("每个并行控制外壳增加的并行上限")
                .defineInRange("moduleParallelPerUnit", 8, 0, 1_000_000);
        ASSEMBLY_MODULE_SPEED_PER_UNIT = BUILDER
                .comment("每个速度控制外壳增加的速度比例（0.25 表示 +25%）")
                .defineInRange("moduleSpeedPerUnit", 0.25, 0.0, 100.0);
        ASSEMBLY_MAX_PARALLEL = BUILDER
                .comment("并行上限（包含结构长度与模块的全部加成）")
                .defineInRange("maxParallel", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        ASSEMBLY_MAX_SPEED_MULTIPLIER = BUILDER
                .comment("速度倍率上限")
                .defineInRange("maxSpeedMultiplier", 64.0, 1.0, Double.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("namingFactory");
        NAMING_FACTORY_MAX_POWER = BUILDER
                .comment("名称压印工厂的内部能量缓存上限（AE）")
                .defineInRange("maxPower", 16000.0, 0.0, Double.MAX_VALUE);
        NAMING_FACTORY_ENERGY_PER_ITEM = BUILDER
                .comment("每重命名一个物品消耗的能量（AE）")
                .defineInRange("energyPerItem", 250.0, 0.0, Double.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("lavaSmelter");
        LAVA_SMELTER_PARALLEL = BUILDER
                .comment("熔岩冶炼炉的最大并行数（每并行一份消耗 1 点配方耐久）")
                .defineInRange("parallel", 64, 1, 1_000_000);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private Config() {
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

    public static int assemblyLineMaxSlices() {
        return ASSEMBLY_MAX_SLICES.get();
    }

    public static double assemblyEnergyPerParallel() {
        return ASSEMBLY_ENERGY_PER_PARALLEL.get();
    }

    public static int assemblyModuleParallelPerUnit() {
        return ASSEMBLY_MODULE_PARALLEL_PER_UNIT.get();
    }

    public static double assemblyModuleSpeedPerUnit() {
        return ASSEMBLY_MODULE_SPEED_PER_UNIT.get();
    }

    public static int assemblyMaxParallel() {
        return ASSEMBLY_MAX_PARALLEL.get();
    }

    public static double assemblyMaxSpeedMultiplier() {
        return ASSEMBLY_MAX_SPEED_MULTIPLIER.get();
    }

    public static double namingFactoryMaxPower() {
        return NAMING_FACTORY_MAX_POWER.get();
    }

    public static double namingFactoryEnergyPerItem() {
        return NAMING_FACTORY_ENERGY_PER_ITEM.get();
    }

    public static int lavaSmelterParallel() {
        return LAVA_SMELTER_PARALLEL.get();
    }
}
