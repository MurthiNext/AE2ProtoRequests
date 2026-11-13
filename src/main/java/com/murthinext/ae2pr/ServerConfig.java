package com.murthinext.ae2pr;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import appeng.blockentity.misc.ChargerBlockEntity;

import com.murthinext.ae2pr.api.EnergyAmount;
import com.murthinext.ae2pr.api.math.LatexFormula;
import com.murthinext.ae2pr.api.upgrade.MachineUpgradeDefinition;
import com.murthinext.ae2pr.api.upgrade.MachineUpgradeType;
import com.murthinext.ae2pr.network.ServerConfigSyncPacket;
import com.murthinext.ae2pr.registry.ModNetwork;

public final class ServerConfig {
    public static final String FILE_NAME = "ae2pr-server.toml";

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

    public static final MachineUpgradeDefinition AEV_SPEED;
    public static final MachineUpgradeDefinition AEV_PARALLEL;
    public static final MachineUpgradeDefinition AEV_ENERGY;

    static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("repeatOrder");
        MAX_ROUNDS = BUILDER.comment("单次重复下单允许的最大轮数")
                             .defineInRange("maxRounds", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        RETRY_COUNT = BUILDER.comment("开始下一轮失败后的重试次数").defineInRange("retryCount", 3, 0, 100);
        RETRY_INTERVAL_TICKS = BUILDER.comment("相邻两次重试之间的间隔（ticks）")
                                       .defineInRange("retryIntervalTicks", 20, 1, 72000);
        NOTIFY_ON_FAILURE = BUILDER.comment("重复订单失败时是否发送无线终端通知").define("notifyOnFailure", true);
        BUILDER.pop();

        BUILDER.push("redstoneRequester");
        REQUESTER_SLOTS = BUILDER.comment("ME 红石请求器可标记的物品数量").defineInRange("slots", 5, 1, 63);
        REQUESTER_IDLE_ENERGY = BUILDER.comment("ME 红石请求器空闲时从网络抽取的能量（AE）")
                                        .defineInRange("idleEnergy", 5.0, 0.0, EnergyAmount.MAX);
        REQUESTER_REQUIRE_CHANNEL = BUILDER.comment("ME 红石请求器是否需要占用一个网络频道")
                                            .define("requireChannel", true);
        BUILDER.pop();

        BUILDER.push("crystalAssemblyLine");
        ASSEMBLY_ITEMS_ORDERED = BUILDER.comment("水晶装配线物品输入是否有序").define("itemInputsOrdered", true);
        ASSEMBLY_FLUIDS_ORDERED = BUILDER.comment("水晶装配线流体输入是否有序").define("fluidInputsOrdered", false);
        ASSEMBLY_BASE_PARALLEL = BUILDER.comment("水晶装配线最短结构的默认并行数")
                                         .defineInRange("baseParallel", 16, 1, Integer.MAX_VALUE);
        ASSEMBLY_PARALLEL_PER_SLICE = BUILDER.comment("结构每超出最短长度1片增加的并行数")
                                              .defineInRange("parallelPerSlice", 8, 0, Integer.MAX_VALUE);
        ASSEMBLY_MAX_SLICES = BUILDER.comment("水晶装配线结构允许的最大片数").defineInRange("maxSlices", 31, 5, 1024);
        ASSEMBLY_MODULE_PARALLEL_PER_UNIT = BUILDER.comment("水晶装配线每个并行控制外壳增加的并行数")
                                                    .defineInRange("moduleParallelPerUnit", 16, 0, Integer.MAX_VALUE);
        ASSEMBLY_MODULE_SPEED_PER_UNIT = BUILDER.comment("水晶装配线每个速度控制外壳增加的速度比例")
                                                 .defineInRange("moduleSpeedPerUnit", 0.5, 0.0, 100.0);
        BUILDER.pop();

        BUILDER.push("namingFactory");
        NAMING_FACTORY_MAX_POWER = BUILDER.comment("名称压印工厂的内部能量缓存上限（AE）")
                                           .defineInRange("maxPower", 16000.0, 0.0, EnergyAmount.MAX);
        NAMING_FACTORY_ENERGY_PER_ITEM = BUILDER.comment("名称压印工厂每重命名一个物品消耗的能量（AE）")
                                                 .defineInRange("energyPerItem", 250.0, 0.0, EnergyAmount.MAX);
        BUILDER.pop();

        BUILDER.push("highReactivityLavaSmelter");
        HIGH_REACTIVITY_LAVA_SMELTER_BASE_PARALLEL = BUILDER.comment("熔岩冶炼炉默认并行数")
                                                             .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        HIGH_REACTIVITY_LAVA_SMELTER_PASSIVE_ENERGY = BUILDER.comment("熔岩冶炼炉被动耗电量（AE/tick）")
                                                              .defineInRange("passiveEnergyPerTick", 1000.0, 0.0,
                                                                             EnergyAmount.MAX);
        HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_LOSS_CHANCE =
                BUILDER.comment("熔岩冶炼炉电力供给不足时每秒扣除 1 点配方耐久的概率")
                        .defineInRange("durabilityLossChancePerSecond", 0.5, 0.0, 1.0);
        HIGH_REACTIVITY_LAVA_SMELTER_MAX_DURABILITY = BUILDER.comment("熔岩冶炼炉的配方耐久上限")
                                                              .defineInRange("maxDurability", 128, 1,
                                                                             Integer.MAX_VALUE);
        HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_PER_DUST = BUILDER.comment("每份陨石粉补充的配方耐久")
                                                                   .defineInRange("durabilityPerDust", 32, 1,
                                                                                  Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("matterPhaseTransformer");
        MATTER_PHASE_TRANSFORMER_BASE_PARALLEL = BUILDER.comment("物质相变仪默认并行数")
                                                         .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("crystalReactionChamber");
        CRYSTAL_REACTION_BASE_PARALLEL = BUILDER.comment("水晶反应仓默认并行数")
                                                 .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("crystalWorkstation");
        CRYSTAL_WORKSTATION_BASE_PARALLEL = BUILDER.comment("晶能工作站默认并行数")
                                                    .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("jig");
        JIG_BASE_PARALLEL = BUILDER.comment("跳汰机默认并行数").defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("filterPress");
        FILTER_PRESS_BASE_PARALLEL = BUILDER.comment("压滤机默认并行数")
                                             .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("multiFactory");
        MULTI_FACTORY_BASE_PARALLEL = BUILDER.comment("通式加工厂默认并行数")
                                              .defineInRange("baseParallel", 64, 1, Integer.MAX_VALUE);
        MULTI_FACTORY_INSCRIBING_DURATION = BUILDER.comment("通式加工厂压印模式单次作业时长（ticks）")
                                                    .defineInRange("inscribingDuration", 100, 1, 72000);
        MULTI_FACTORY_CHARGING_DURATION = BUILDER.comment("通式加工厂充能模式单次作业时长（ticks）")
                                                  .defineInRange("chargingDuration", 50, 1, 72000);
        MULTI_FACTORY_INSCRIBING_ENERGY = BUILDER.comment("通式加工厂压印模式每个并行默认扣除的能量（AE）")
                                                  .defineInRange("inscribingEnergyPerParallel", 2000.0, 0.0,
                                                                 EnergyAmount.MAX);
        MULTI_FACTORY_CHARGING_ENERGY = BUILDER.comment("通式加工厂充能模式每个并行默认扣除的能量（AE）")
                                                .defineInRange("chargingEnergyPerParallel",
                                                               ChargerBlockEntity.POWER_MAXIMUM_AMOUNT, 0.0,
                                                               EnergyAmount.MAX);
        BUILDER.pop();

        BUILDER.push("crystalReactor");
        CRYSTAL_REACTOR_RADIUS = BUILDER.comment("水晶反应器以核心为圆心的水平替换半径")
                                         .defineInRange("radius", 8, 1, 64);
        CRYSTAL_REACTOR_DEPTH = BUILDER.comment("水晶反应器以核心为顶面的向下替换层数")
                                        .defineInRange("depth", 4, 0, 64);
        CRYSTAL_REACTOR_DURATION_TICKS = BUILDER.comment("水晶反应器的总扩散耗时（ticks）")
                                                 .defineInRange("durationTicks", 160, 1, 72000);
        CRYSTAL_REACTOR_SOUNDS_PER_TICK = BUILDER.comment("水晶反应器每 tick 最多播放的方块破坏音效数量")
                                                  .defineInRange("breakSoundsPerTick", 2, 0, 64);
        CRYSTAL_REACTOR_FLAWLESS_WEIGHT = BUILDER.comment("水晶反应器无瑕档位权重")
                                                  .defineInRange("flawlessWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_FLAWED_WEIGHT = BUILDER.comment("水晶反应器有瑕档位权重")
                                                .defineInRange("flawedWeight", 4, 0, 1_000_000);
        CRYSTAL_REACTOR_CHIPPED_WEIGHT = BUILDER.comment("水晶反应器开裂档位权重")
                                                 .defineInRange("chippedWeight", 8, 0, 1_000_000);
        CRYSTAL_REACTOR_DAMAGED_WEIGHT = BUILDER.comment("水晶反应器损坏档位权重")
                                                 .defineInRange("damagedWeight", 32, 0, 1_000_000);
        CRYSTAL_REACTOR_AMETHYST_WEIGHT = BUILDER.comment("水晶反应器紫水晶母岩（恒为无瑕）的材质权重")
                                                  .defineInRange("amethystWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_CERTUS_WEIGHT = BUILDER.comment("水晶反应器赛特斯石英母岩的材质权重")
                                                .defineInRange("certusWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_ENERGIZED_WEIGHT = BUILDER.comment("水晶反应器 Neo ECO AE Extension 盈能水晶母岩的材质权重")
                                                   .defineInRange("energizedWeight", 1, 0, 1_000_000);
        CRYSTAL_REACTOR_OVERLOAD_WEIGHT = BUILDER.comment("水晶反应器 AE2 Lightning Tech 过载水晶母岩的材质权重")
                                                  .defineInRange("overloadWeight", 1, 0, 1_000_000);
        BUILDER.pop();

        BUILDER.push("upgradeCards");
        LOSSY_SPEED_PER_CARD = BUILDER.comment("多方块机器每张加速卡提供的速度加成，加法叠加")
                                       .defineInRange("lossySpeedPerCard", 0.5, 0.0, 100.0);
        LOSSY_ENERGY_MULTIPLIER_PER_CARD = BUILDER.comment("多方块机器每张加速卡对单次作业耗能的倍率，逐张相乘")
                                                   .defineInRange("lossyEnergyMultiplierPerCard", 4.0, 1.0, 1000.0);
        AEV_SPEED = define("aev_speed_card", MachineUpgradeType.SPEED, "f(x)=2x",
                           "x 为应用本张卡前的速度倍率，包含结构模块和 AE2 加速卡的增益");
        AEV_PARALLEL = define("aev_parallel_card", MachineUpgradeType.PARALLEL, "f(x)=4x+32",
                              "x 为应用本张卡前的并行上限，包含结构模块的增益；每张卡计算后向下取整");
        AEV_ENERGY = define("aev_energy_card", MachineUpgradeType.ENERGY, "f(x)=\\frac{x}{2}",
                            "x 为应用本张卡前的每并行配方耗能（AE），包含 AE2 加速卡的耗能惩罚");
        BUILDER.pop();

        BUILDER.push("collapse");
        COLLAPSE_ENABLED =
                BUILDER.comment("是否允许机器被破坏时把超量内容压成坍缩物品存储元件",
                                "开启后默认兼容本模组仓室与 AE 系列（接口、样板供应器等使用 AE2 通用库存的机器）")
                        .define("enableCollapse", true);
        COLLAPSE_EXTRA_BLOCKS = BUILDER.comment("额外允许坍缩的方块，格式为 modid:itemid 或 modid:*",
                                                "为这些方块读取 Forge 物品能力中的内容；modid:* 表示该模组全部方块")
                                        .defineListAllowEmpty("extraEnableCollapse", List.of(),
                                                              entry -> entry instanceof String text && !text.isBlank());
        COLLAPSE_THRESHOLD = BUILDER.comment("触发坍缩的物品数量阈值")
                                     .defineInRange("threshold", 32768L, 1L, Long.MAX_VALUE);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private ServerConfig() {
    }

    // 客户端在进入服务器前还未收到同步配置，此时回退到默认值
    private static <T> T read(ForgeConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    // 服务端已运行时的配置重载不会由 Forge 重发，这里补一次全量同步
    public static void onReload(ModConfigEvent.Reloading event) {
        var config = event.getConfig();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (config.getSpec() == SPEC && config.getConfigData() instanceof CommentedFileConfig && server != null) {
            byte[] snapshot = TomlFormat.instance()
                                      .createWriter()
                                      .writeToString(config.getConfigData())
                                      .getBytes(StandardCharsets.UTF_8);
            server.execute(
                    () -> ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new ServerConfigSyncPacket(snapshot)));
        }
    }

    private static MachineUpgradeDefinition define(String id, MachineUpgradeType type, String fallback,
                                                   String meaning) {
        LatexFormula.parse(fallback);
        BUILDER.push(id);
        ForgeConfigSpec.ConfigValue<String>
                value = BUILDER.comment(meaning, "同类卡逐张代入；无卡时不应用公式。使用 LaTeX，例如 f(x)=\\frac{x}{2}",
                                        "TOML 单引号字符串可直接写反斜杠；双引号字符串需要转义反斜杠。",
                                        "语法与边界规则见 docs/upgrade_cards.md")
                                .define("formula", fallback, candidate -> {
                                    if (!(candidate instanceof String source)) {
                                        return false;
                                    }
                                    try {
                                        LatexFormula.parse(source);
                                        return true;
                                    } catch (IllegalArgumentException exception) {
                                        ae2pr.LOGGER.warn("Invalid upgrade formula for {}: {}", id,
                                                          exception.getMessage());
                                        return false;
                                    }
                                });
        BUILDER.pop();
        return new MachineUpgradeDefinition(id, type, fallback, () -> read(value));
    }

    public static int maxRounds() {
        return read(MAX_ROUNDS);
    }

    public static int retryCount() {
        return read(RETRY_COUNT);
    }

    public static int retryIntervalTicks() {
        return read(RETRY_INTERVAL_TICKS);
    }

    public static boolean notifyOnFailure() {
        return read(NOTIFY_ON_FAILURE);
    }

    public static int requesterSlots() {
        return read(REQUESTER_SLOTS);
    }

    public static double requesterIdleEnergy() {
        return read(REQUESTER_IDLE_ENERGY);
    }

    public static boolean requesterRequireChannel() {
        return read(REQUESTER_REQUIRE_CHANNEL);
    }

    public static boolean assemblyItemsOrdered() {
        return read(ASSEMBLY_ITEMS_ORDERED);
    }

    public static boolean assemblyFluidsOrdered() {
        return read(ASSEMBLY_FLUIDS_ORDERED);
    }

    public static int assemblyBaseParallel() {
        return read(ASSEMBLY_BASE_PARALLEL);
    }

    public static int assemblyParallelPerSlice() {
        return read(ASSEMBLY_PARALLEL_PER_SLICE);
    }

    public static int crystalAssemblyLineMaxSlices() {
        return read(ASSEMBLY_MAX_SLICES);
    }

    public static int assemblyModuleParallelPerUnit() {
        return read(ASSEMBLY_MODULE_PARALLEL_PER_UNIT);
    }

    public static double assemblyModuleSpeedPerUnit() {
        return read(ASSEMBLY_MODULE_SPEED_PER_UNIT);
    }

    public static double namingFactoryMaxPower() {
        return read(NAMING_FACTORY_MAX_POWER);
    }

    public static double namingFactoryEnergyPerItem() {
        return read(NAMING_FACTORY_ENERGY_PER_ITEM);
    }

    public static int highReactivityLavaSmelterBaseParallel() {
        return read(HIGH_REACTIVITY_LAVA_SMELTER_BASE_PARALLEL);
    }

    public static double highReactivityLavaSmelterPassiveEnergy() {
        return read(HIGH_REACTIVITY_LAVA_SMELTER_PASSIVE_ENERGY);
    }

    public static double highReactivityLavaSmelterDurabilityLossChance() {
        return read(HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_LOSS_CHANCE);
    }

    public static int highReactivityLavaSmelterMaxDurability() {
        return read(HIGH_REACTIVITY_LAVA_SMELTER_MAX_DURABILITY);
    }

    public static int highReactivityLavaSmelterDurabilityPerDust() {
        return read(HIGH_REACTIVITY_LAVA_SMELTER_DURABILITY_PER_DUST);
    }

    public static int matterPhaseTransformerBaseParallel() {
        return read(MATTER_PHASE_TRANSFORMER_BASE_PARALLEL);
    }

    public static int crystalReactionBaseParallel() {
        return read(CRYSTAL_REACTION_BASE_PARALLEL);
    }

    public static int crystalWorkstationBaseParallel() {
        return read(CRYSTAL_WORKSTATION_BASE_PARALLEL);
    }

    public static int jigBaseParallel() {
        return read(JIG_BASE_PARALLEL);
    }

    public static int filterPressBaseParallel() {
        return read(FILTER_PRESS_BASE_PARALLEL);
    }

    public static int multiFactoryBaseParallel() {
        return read(MULTI_FACTORY_BASE_PARALLEL);
    }

    public static int multiFactoryInscribingDuration() {
        return read(MULTI_FACTORY_INSCRIBING_DURATION);
    }

    public static int multiFactoryChargingDuration() {
        return read(MULTI_FACTORY_CHARGING_DURATION);
    }

    public static double multiFactoryInscribingEnergyPerParallel() {
        return read(MULTI_FACTORY_INSCRIBING_ENERGY);
    }

    public static double multiFactoryChargingEnergyPerParallel() {
        return read(MULTI_FACTORY_CHARGING_ENERGY);
    }

    public static int crystalReactorRadius() {
        return read(CRYSTAL_REACTOR_RADIUS);
    }

    public static int crystalReactorDepth() {
        return read(CRYSTAL_REACTOR_DEPTH);
    }

    public static int crystalReactorDurationTicks() {
        return read(CRYSTAL_REACTOR_DURATION_TICKS);
    }

    public static int crystalReactorBreakSoundsPerTick() {
        return read(CRYSTAL_REACTOR_SOUNDS_PER_TICK);
    }

    public static int[] crystalReactorTierWeights() {
        return new int[] { read(CRYSTAL_REACTOR_FLAWLESS_WEIGHT), read(CRYSTAL_REACTOR_FLAWED_WEIGHT),
                           read(CRYSTAL_REACTOR_CHIPPED_WEIGHT), read(CRYSTAL_REACTOR_DAMAGED_WEIGHT) };
    }

    public static int[] crystalReactorMaterialWeights() {
        return new int[] { read(CRYSTAL_REACTOR_AMETHYST_WEIGHT), read(CRYSTAL_REACTOR_CERTUS_WEIGHT),
                           read(CRYSTAL_REACTOR_ENERGIZED_WEIGHT), read(CRYSTAL_REACTOR_OVERLOAD_WEIGHT) };
    }

    public static double lossySpeedPerCard() {
        return read(LOSSY_SPEED_PER_CARD);
    }

    public static double lossyEnergyMultiplierPerCard() {
        return read(LOSSY_ENERGY_MULTIPLIER_PER_CARD);
    }

    public static boolean collapseEnabled() {
        return read(COLLAPSE_ENABLED);
    }

    public static List<String> collapseExtraBlocks() {
        return read(COLLAPSE_EXTRA_BLOCKS).stream().filter(String.class ::isInstance).map(String.class ::cast).toList();
    }

    public static long collapseThreshold() {
        return read(COLLAPSE_THRESHOLD);
    }
}
