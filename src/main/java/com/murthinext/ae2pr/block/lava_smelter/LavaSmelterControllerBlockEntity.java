package com.murthinext.ae2pr.block.lava_smelter;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModRecipes;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.block.assembly_line.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;

/**
 * 高反应性熔岩冶炼炉主机方块实体：结构检测 + 异星熔岩配方执行。
 * <p>
 * 结构检测由 {@link StructureValidator} 完成：结构未变化时走缓存快速路径。
 * <p>
 * 执行流程：匹配配方、校验输出空间与配方耐久、扣除原料与耐久后加工 {@code duration} tick、产物输出。
 * <p>
 * 配方耐久存储在主机内（上限 {@value #MAX_DURABILITY}，每个陨石粉补充 {@value #DURABILITY_PER_DUST}），
 * 每份配方消耗 1 点；耐久 &gt; 0 时炉膛空腔渲染假异星熔岩并对腔内玩家造成 4 倍熔岩伤害，
 * 耐久为 0 时渲染普通熔岩、伤害为 2 倍。
 */
public class LavaSmelterControllerBlockEntity extends BlockEntity {

    /** 结构检测周期（tick） */
    private static final int CHECK_INTERVAL = 20;
    /** 暂停后的重试间隔（tick） */
    private static final int RETRY_INTERVAL = 20;
    /** 配方耐久上限 */
    public static final int MAX_DURABILITY = 128;
    /** 每个陨石粉提供的耐久 */
    public static final int DURABILITY_PER_DUST = 32;
    /** 陨石粉槽容量 */
    public static final int DUST_SLOT_CAPACITY = 64;
    /** 原版熔岩单次伤害 */
    private static final float LAVA_DAMAGE = 4.0F;

    /** 陨石粉（AE2 天石粉） */
    private static final ResourceLocation SKY_DUST_ID = new ResourceLocation("ae2", "sky_dust");

    private static final String DURABILITY_ID = "durability";
    private static final String DUST_ID = "dust";
    private static final String MIRROR_SIDE_ID = "mirrorSide";
    private static final String MIRROR_FRONT_ID = "mirrorFront";
    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String TAG_UPGRADES = "upgrades";

    /** 暂停原因（供界面报错） */
    public enum Error {
        NONE, DURABILITY, POWER, OUTPUT
    }

    private int tickCounter;
    private int retryCounter;

    /** 加工剩余 tick（<= 0 表示空闲或暂停） */
    private int jobTicksLeft;
    /** 当前作业的实际总耗时（tick，含加速卡加成） */
    private int jobTotalTicks;
    /** 当前作业的并行数 */
    private int jobParallel;
    /** 是否处于暂停态（耐久耗尽/输出不足，黄色暂停贴图） */
    private boolean paused;
    private Error error = Error.NONE;
    /** 当前作业（加工中保留，用于结算产物） */
    @Nullable
    private LavaSmelterRecipe currentRecipe;

    /** 配方耐久：每份配方消耗 1 点，陨石粉补充 */
    private int durability;

    /** 最近一次成型的镜像（供假熔岩渲染与伤害判定） */
    private boolean mirrorSide;
    private boolean mirrorFront;

    /** 客户端展示用：当前作业产物（仅由同步包写入） */
    private ItemStack clientJobOutput = ItemStack.EMPTY;

    /** 结构内的输入总线（按 片 → 行 → 列 排序） */
    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    /** 结构内的输出总线 */
    private final List<ItemBusBlockEntity> outputBuses = new ArrayList<>();
    /** 结构内的能源仓 */
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    /** 结构验证器：模式匹配 + 缓存快速路径 */
    private final StructureValidator structure = new StructureValidator(LavaSmelterStructure.PATTERN);
    /** 升级槽 */
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.HIGH_REACTIVITY_LAVA_SMELTER.get(), 4, this::onUpgradesChanged);

    /** 陨石粉槽：容量 64，仅接受陨石粉；对外暴露物品能力 */
    private final ItemStackHandler dustSlot = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return DUST_SLOT_CAPACITY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isSkyDust(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> dustCapability = LazyOptional.of(() -> dustSlot);

    public LavaSmelterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_REACTIVITY_LAVA_SMELTER.get(), pos, state);
    }

    /** 服务端 tick：结构检测、陨石粉补充、空腔伤害与加工作业。 */
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (++tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;
            validateStructure();
        }
        absorbDust();
        tickCavityDamage();
        tickJob();
    }

    // ---------------------------------------------------------------- 配方耐久

    /** 自动把槽内陨石粉转化为耐久：仅当 32 点耐久能完整存入时才消耗。 */
    private void absorbDust() {
        if (durability > MAX_DURABILITY - DURABILITY_PER_DUST) {
            return;
        }
        ItemStack stack = dustSlot.getStackInSlot(0);
        if (stack.isEmpty() || !isSkyDust(stack)) {
            return;
        }
        if (dustSlot.extractItem(0, 1, false).isEmpty()) {
            return;
        }
        durability += DURABILITY_PER_DUST;
        setChanged();
        syncClient();
    }

    private static boolean isSkyDust(ItemStack stack) {
        Item item = ForgeRegistries.ITEMS.getValue(SKY_DUST_ID);
        return item != null && stack.is(item);
    }

    /** 炉膛空腔伤害：耐久 &gt; 0 时为熔岩 4 倍，否则 2 倍。 */
    private void tickCavityDamage() {
        Level level = this.level;
        if (level == null || !isFormed()) {
            return;
        }
        Direction facing = getBlockState().getValue(HighReactivityLavaSmelterBlock.FACING);
        AABB cavity = LavaSmelterStructure.cavityAabb(worldPosition, facing, mirrorSide, mirrorFront);
        float damage = LAVA_DAMAGE * (durability > 0 ? 4.0F : 2.0F);
        for (Player player : level.getEntitiesOfClass(Player.class, cavity)) {
            if (!player.fireImmune()) {
                player.hurt(player.damageSources().lava(), damage);
            }
        }
    }

    // ---------------------------------------------------------------- 加工作业

    private void tickJob() {
        if (!isFormed()) {
            if (jobTicksLeft > 0 || paused || error != Error.NONE || currentRecipe != null) {
                jobTicksLeft = 0;
                jobTotalTicks = 0;
                jobParallel = 0;
                currentRecipe = null;
                clearPause();
            }
            return;
        }
        if (jobTicksLeft > 0) {
            if (--jobTicksLeft == 0) {
                finishJob();
            }
            return;
        }
        if (retryCounter > 0) {
            retryCounter--;
            return;
        }
        retryCounter = RETRY_INTERVAL;
        tryStartJob();
    }

    /** 尝试开始一次加工：匹配配方 → 扣耐久 → 输出检查 → 扣电 → 扣料 → 进入运行态。 */
    private void tryStartJob() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        LavaSmelterRecipe recipe = findMatch();
        if (recipe == null) {
            clearPause();
            return;
        }
        if (durability <= 0) {
            pause(Error.DURABILITY);
            return;
        }
        // 实际并行数 = 输入份数、配方耐久与输出容量中的较小值
        int parallel = parallelFor(recipe);
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        // 能量按并行数与加速卡耗能倍率从结构内能源仓所接 ME 网络一次性扣除
        double cost = recipe.getEnergyPerParallel() * parallel * lossyEnergyMultiplier();
        if (extractNetworkEnergy(cost, true) < cost) {
            pause(Error.POWER);
            return;
        }
        // 扣电、扣耐久与扣料
        extractNetworkEnergy(cost, false);
        durability -= parallel;
        consumeInputs(recipe, parallel);

        jobTotalTicks = scaledDuration(recipe.getDuration());
        jobTicksLeft = jobTotalTicks;
        jobParallel = parallel;
        currentRecipe = recipe;
        paused = false;
        error = Error.NONE;
        setChanged();
        syncClient();
        applyControllerState();
    }

    /** 加工完成：产物写入输出总线，并立即触发下一次配方检查以消除连续加工之间的空档。 */
    private void finishJob() {
        LavaSmelterRecipe recipe = currentRecipe;
        int parallel = jobParallel;
        currentRecipe = null;
        jobParallel = 0;
        if (recipe != null && level != null) {
            insertOutputs(recipe, parallel);
        }
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        applyControllerState();
        if (isFormed()) {
            tryStartJob();
        }
    }

    private void pause(Error reason) {
        jobTicksLeft = 0;
        jobTotalTicks = 0;
        paused = true;
        error = reason;
        applyControllerState();
    }

    private void clearPause() {
        if (paused || error != Error.NONE) {
            paused = false;
            error = Error.NONE;
            applyControllerState();
        }
    }

    // ---------------------------------------------------------------- 配方匹配

    /** 找到首个输入总线可满足、且耐久/输出允许尝试的配方。 */
    @Nullable
    private LavaSmelterRecipe findMatch() {
        Level level = this.level;
        if (level == null) {
            return null;
        }
        for (LavaSmelterRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.LAVA_SMELTER_TYPE.get())) {
            if (hasIngredients(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    /** 输入总线内是否有满足配方的原料（统计所有槽位，只校验种类与总数量）。 */
    private boolean hasIngredients(LavaSmelterRecipe recipe) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int available = 0;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                    ItemStack stack = bus.getStorage().getStackInSlot(slot);
                    if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                        available += stack.getCount();
                    }
                }
            }
            if (available < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private void consumeInputs(LavaSmelterRecipe recipe, int parallel) {
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int remaining = ingredient.count() * parallel;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount() && remaining > 0; slot++) {
                    // 单次抽取最多只能取走物品的堆叠上限（64），按需求量分批抽取
                    while (remaining > 0) {
                        ItemStack current = bus.getStorage().getStackInSlot(slot);
                        if (current.isEmpty() || !ingredient.ingredient().test(current)) {
                            break;
                        }
                        ItemStack taken = bus.getStorage().extractItem(slot, remaining, false);
                        if (taken.isEmpty()) {
                            break;
                        }
                        remaining -= taken.getCount();
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * 实际并行数：输入份数、配方耐久与输出容量的较小值，上限为配置的最大并行数；
     * 每种原料按总数量除以单份需求计算可并行份数。
     */
    private int parallelFor(LavaSmelterRecipe recipe) {
        int parallel = maxParallel();
        parallel = Math.min(parallel, durability);
        for (CountedIngredient ingredient : recipe.getCountedIngredients()) {
            int available = 0;
            for (ItemBusBlockEntity bus : inputBuses) {
                for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                    ItemStack stack = bus.getStorage().getStackInSlot(slot);
                    if (!stack.isEmpty() && ingredient.ingredient().test(stack)) {
                        available += stack.getCount();
                    }
                }
            }
            parallel = Math.min(parallel, available / ingredient.count());
        }
        return outputParallel(recipe, parallel);
    }

    /** 在输入与耐久允许的并行范围内，二分求出全部产物都能放下的最大并行数。 */
    private int outputParallel(LavaSmelterRecipe recipe, int maximum) {
        int low = 0;
        int high = maximum;
        while (low < high) {
            int middle = low + (high - low + 1) / 2;
            if (canFitOutputs(recipe, middle)) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /** 模拟多个产物依次进入输出总线（逐槽位），防止不同产物重复占用同一空槽。 */
    private boolean canFitOutputs(LavaSmelterRecipe recipe, int parallel) {
        List<ItemBusBlockEntity> owners = new ArrayList<>();
        List<ItemStack> simulated = new ArrayList<>();
        for (ItemBusBlockEntity bus : outputBuses) {
            for (int slot = 0; slot < bus.getSlotCount(); slot++) {
                owners.add(bus);
                simulated.add(bus.getStorage().getStackInSlot(slot).copy());
            }
        }
        for (ItemStack result : recipe.getResults()) {
            long remaining = (long) result.getCount() * parallel;
            for (int index = 0; index < simulated.size() && remaining > 0; index++) {
                ItemStack stored = simulated.get(index);
                int capacity = owners.get(index).getCapacityPerSlot();
                if (stored.isEmpty()) {
                    int inserted = (int) Math.min(remaining, capacity);
                    simulated.set(index, result.copyWithCount(inserted));
                    remaining -= inserted;
                } else if (ItemStack.isSameItemSameTags(stored, result)) {
                    int inserted = (int) Math.min(remaining,
                            Math.max(0, capacity - stored.getCount()));
                    stored.grow(inserted);
                    remaining -= inserted;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private void insertOutputs(LavaSmelterRecipe recipe, int parallel) {
        Level level = this.level;
        if (level == null) {
            return;
        }
        for (ItemStack result : recipe.getResults()) {
            ItemStack remaining = result.copyWithCount(result.getCount() * parallel);
            for (ItemBusBlockEntity bus : outputBuses) {
                if (remaining.isEmpty()) {
                    break;
                }
                remaining = ItemHandlerHelper.insertItemStacked(bus.getStorage(), remaining, false);
            }
            if (!remaining.isEmpty()) {
                // 启动前已检查；兜底掉落到主机处，避免产物丢失
                ae2pr.LOGGER.warn("熔岩冶炼炉产物无法放入输出总线，已掉落 {}", remaining);
                Block.popResource(level, worldPosition, remaining);
            }
        }
    }

    // ---------------------------------------------------------------- 加速卡与电力

    /** 结构允许的最大并行数 */
    public int maxParallel() {
        return Math.max(1, Config.lavaSmelterParallel());
    }

    /** 已安装的加速卡数量 */
    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    /** 加速卡的速度倍率 */
    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    /** 加速卡的耗能倍率 */
    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    /** 按速度倍率折算实际耗时 */
    private int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    /** 升级槽 */
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    /** 升级变化回调 */
    private void onUpgradesChanged() {
        setChanged();
    }

    /** 从结构内能源仓一次性抽取能量 */
    private double extractNetworkEnergy(double amount, boolean simulate) {
        double remaining = amount;
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (remaining <= 0) {
                break;
            }
            remaining -= hatch.extractAEPower(remaining, simulate);
        }
        return amount - remaining;
    }

    /** 结构内是否有能源仓接入 ME 网络 */
    public boolean isEnergyConnected() {
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (hatch.isGridConnected()) {
                return true;
            }
        }
        return false;
    }

    /** 结构内能源仓所接 ME 网络的可用能量合计 */
    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }

    // ---------------------------------------------------------------- 结构检测

    /** 立即检测一次结构，并按结果切换主机与结构内部件的状态；结构未变化时走缓存快速路径。 */
    public void validateStructure() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.HIGH_REACTIVITY_LAVA_SMELTER.get())) {
            return;
        }
        Direction facing = state.getValue(HighReactivityLavaSmelterBlock.FACING);
        StructureResult result = structure.validate(level, worldPosition, facing, Direction.UP,
                LavaSmelterStructure.PATTERN.minSlices());
        if (result.fromCache()) {
            // 缓存命中：结构未变化，无需刷新外观与部件引用
            return;
        }
        mirrorSide = result.mirrorSide();
        mirrorFront = result.mirrorFront();
        boolean formed = result.formed();
        LavaSmelterStructure.updateFormed(level, worldPosition, facing, result);
        refreshParts(level, result);
        applyControllerState(formed);
        syncClient();
    }

    /** 收集结构内的输入/输出总线与能源仓（仅在结构成型时收集）。 */
    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        outputBuses.clear();
        energyHatches.clear();
        if (!result.formed()) {
            return;
        }
        // 显式按 片 → 行 → 列 排序，保证部件顺序稳定
        List<BlockPos> cells = new ArrayList<>(result.cells());
        cells.sort(LavaSmelterStructure.PATTERN.posComparator(
                getBlockState().getValue(HighReactivityLavaSmelterBlock.FACING), Direction.UP,
                result.mirrorSide(), result.mirrorFront()));
        for (BlockPos pos : cells) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ItemBusBlockEntity bus) {
                if (bus.isOutputBus()) {
                    outputBuses.add(bus);
                } else {
                    inputBuses.add(bus);
                }
            } else if (blockEntity instanceof FluixCrystalEnergyHatchBlockEntity hatch) {
                energyHatches.add(hatch);
            }
        }
    }

    /** 按当前方块状态读取成型标记并刷新控制器状态。 */
    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(HighReactivityLavaSmelterBlock.FORMED));
    }

    /** 同步控制器的成型标记与运行/暂停贴图。 */
    private void applyControllerState(boolean formed) {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean showRunning = formed && jobTicksLeft > 0;
        boolean showPaused = formed && paused && jobTicksLeft <= 0;
        BlockState updated = state;
        if (state.getValue(HighReactivityLavaSmelterBlock.FORMED) != formed) {
            updated = updated.setValue(HighReactivityLavaSmelterBlock.FORMED, formed);
        }
        if (state.getValue(HighReactivityLavaSmelterBlock.RUNNING) != showRunning) {
            updated = updated.setValue(HighReactivityLavaSmelterBlock.RUNNING, showRunning);
        }
        if (state.getValue(HighReactivityLavaSmelterBlock.PAUSED) != showPaused) {
            updated = updated.setValue(HighReactivityLavaSmelterBlock.PAUSED, showPaused);
        }
        if (updated != state) {
            level.setBlock(worldPosition, updated, Block.UPDATE_ALL);
        }
    }

    /** 控制器被移除 / 破坏时调用：复位结构中的部件外观并清空结构缓存。 */
    public void onControllerRemoved() {
        Level level = this.level;
        if (level == null || level.isClientSide) {
            return;
        }
        dropDust(level);
        structure.invalidate();
        LavaSmelterStructure.updateFormed(level, worldPosition,
                getBlockState().getValue(HighReactivityLavaSmelterBlock.FACING), StructureResult.EMPTY);
    }

    /** 破坏时把粉槽内容散落到地面。 */
    private void dropDust(Level level) {
        ItemStack stack = dustSlot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition, stack.copy());
            dustSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    // ---------------------------------------------------------------- 客户端同步

    /** 把耐久与镜像状态同步到客户端（假熔岩渲染用）。 */
    private void syncClient() {
        Level level = this.level;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        // 默认实现返回空 tag，这里带上完整存档数据（耐久等）与镜像状态
        CompoundTag tag = saveWithoutMetadata();
        tag.putBoolean(MIRROR_SIDE_ID, mirrorSide);
        tag.putBoolean(MIRROR_FRONT_ID, mirrorFront);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    // ---------------------------------------------------------------- 持久化（耐久 / 陨石粉 / 加工中的作业）

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(DURABILITY_ID, durability);
        tag.put(DUST_ID, dustSlot.serializeNBT());
        upgrades.writeToNBT(tag, TAG_UPGRADES);
        if (currentRecipe != null && jobTicksLeft > 0) {
            tag.putString(JOB_RECIPE_ID, currentRecipe.getId().toString());
            tag.putInt(JOB_TICKS_ID, jobTicksLeft);
            tag.putInt(JOB_TOTAL_TICKS_ID, jobTotalTicks);
            tag.putInt(JOB_PARALLEL_ID, jobParallel);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        durability = tag.getInt(DURABILITY_ID);
        if (tag.contains(DUST_ID)) {
            dustSlot.deserializeNBT(tag.getCompound(DUST_ID));
        }
        upgrades.readFromNBT(tag, TAG_UPGRADES);
        if (tag.contains(MIRROR_SIDE_ID)) {
            mirrorSide = tag.getBoolean(MIRROR_SIDE_ID);
            mirrorFront = tag.getBoolean(MIRROR_FRONT_ID);
        }
        Level level = this.level;
        if (level == null || level.isClientSide || !tag.contains(JOB_RECIPE_ID)) {
            return;
        }
        var recipe = level.getRecipeManager().byKey(new ResourceLocation(tag.getString(JOB_RECIPE_ID)));
        if (recipe.orElse(null) instanceof LavaSmelterRecipe smelterRecipe) {
            // 恢复加工进度
            jobTicksLeft = tag.getInt(JOB_TICKS_ID);
            jobTotalTicks = tag.contains(JOB_TOTAL_TICKS_ID)
                    ? tag.getInt(JOB_TOTAL_TICKS_ID)
                    : Math.max(1, smelterRecipe.getDuration());
            jobParallel = Math.max(1, tag.getInt(JOB_PARALLEL_ID));
            currentRecipe = smelterRecipe;
        }
    }

    // ---------------------------------------------------------------- 能力

    public ItemStackHandler getDustSlot() {
        return dustSlot;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return dustCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        dustCapability.invalidate();
    }

    // ---------------------------------------------------------------- 状态查询

    /** 是否正在加工。 */
    public boolean isRunning() {
        return jobTicksLeft > 0 && isFormed();
    }

    /** 是否因耐久/输出不足暂停。 */
    public boolean isPaused() {
        return paused && isFormed();
    }

    /** 暂停原因（NONE 表示未暂停）。 */
    public Error getError() {
        return error;
    }

    /** 是否已成型。 */
    public boolean isFormed() {
        return getBlockState().getValue(HighReactivityLavaSmelterBlock.FORMED);
    }

    /** 剩余配方耐久。 */
    public int getDurability() {
        return durability;
    }

    /** 最近一次成型的左右镜像（假熔岩渲染用）。 */
    public boolean isMirrorSide() {
        return mirrorSide;
    }

    /** 最近一次成型的前后镜像（假熔岩渲染用）。 */
    public boolean isMirrorFront() {
        return mirrorFront;
    }

    /** 当前作业的展示产物（数量已乘并行数）；空闲返回空。 */
    public ItemStack getJobOutput() {
        if (currentRecipe == null || jobTicksLeft <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack result = currentRecipe.getResultItem();
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        long total = (long) result.getCount() * jobParallel;
        return result.copyWithCount((int) Math.min(total, Integer.MAX_VALUE));
    }

    /** 当前作业的实际总耗时（tick，含加速卡加成）；空闲为 0。 */
    public int getJobDuration() {
        return currentRecipe != null && jobTicksLeft > 0 ? jobTotalTicks : 0;
    }

    /** 当前作业已进行的时间（tick）；空闲为 0。 */
    public int getJobElapsed() {
        return currentRecipe != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    /** 客户端展示的当前作业产物（仅由同步包写入）。 */
    public ItemStack getClientJobOutput() {
        return clientJobOutput;
    }

    /** 客户端同步入口：覆盖本地作业产物（仅由同步包调用）。 */
    public void applyClientJobOutput(ItemStack stack) {
        this.clientJobOutput = stack;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测（不持久化），交给首次 tick 完成，避免加载期修改世界
            tickCounter = CHECK_INTERVAL - 1;
        }
    }
}
