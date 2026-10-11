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
import com.murthinext.ae2pr.block.machine_part.FluixCrystalEnergyHatchBlockEntity;
import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.multiblock.StructureResult;
import com.murthinext.ae2pr.multiblock.StructureValidator;
import com.murthinext.ae2pr.multiblock.structure.LavaSmelterStructure;
import com.murthinext.ae2pr.recipe.CountedIngredient;
import com.murthinext.ae2pr.recipe.LavaSmelterRecipe;

/**
 * 高反应性熔岩冶炼炉主机方块实体。
 */
public class LavaSmelterControllerBlockEntity extends BlockEntity {

    private static final int CHECK_INTERVAL = 20;
    private static final int RETRY_INTERVAL = 20;
    public static final int MAX_DURABILITY = 128;
    public static final int DURABILITY_PER_DUST = 32;
    public static final int DUST_SLOT_CAPACITY = 64;
    private static final float LAVA_DAMAGE = 4.0F;

    private static final ResourceLocation SKY_DUST_ID = ResourceLocation.fromNamespaceAndPath("ae2", "sky_dust");

    private static final String DURABILITY_ID = "durability";
    private static final String DUST_ID = "dust";
    private static final String MIRROR_SIDE_ID = "mirrorSide";
    private static final String MIRROR_FRONT_ID = "mirrorFront";
    private static final String JOB_RECIPE_ID = "jobRecipe";
    private static final String JOB_TICKS_ID = "jobTicks";
    private static final String JOB_TOTAL_TICKS_ID = "jobTotalTicks";
    private static final String JOB_PARALLEL_ID = "jobParallel";
    private static final String TAG_UPGRADES = "upgrades";

    /**
     * 暂停原因。
     */
    public enum Error {
        NONE, DURABILITY, POWER, OUTPUT
    }

    private int tickCounter;
    private int retryCounter;

    private int jobTicksLeft;
    private int jobTotalTicks;
    private int jobParallel;
    private boolean paused;
    private Error error = Error.NONE;
    @Nullable
    private LavaSmelterRecipe currentRecipe;

    private int durability;

    private boolean mirrorSide;
    private boolean mirrorFront;

    private ItemStack clientJobOutput = ItemStack.EMPTY;

    private final List<ItemBusBlockEntity> inputBuses = new ArrayList<>();
    private final List<ItemBusBlockEntity> outputBuses = new ArrayList<>();
    private final List<FluixCrystalEnergyHatchBlockEntity> energyHatches = new ArrayList<>();
    private final StructureValidator structure = new StructureValidator(LavaSmelterStructure.PATTERN);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            ModItems.HIGH_REACTIVITY_LAVA_SMELTER.get(), 4, this::onUpgradesChanged);

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

    /**
     * 服务端 tick：结构检测、陨石粉补充、空腔伤害与加工作业。
     */
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

    // 配方耐久

    /**
     * 陨石粉转耐久：仅当一批耐久能完整存入时才消耗。
     */
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

    /**
     * 炉膛空腔伤害：耐久 > 0 时为熔岩 4 倍，否则 2 倍。
     */
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

    // 加工作业

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
        int parallel = parallelFor(recipe);
        if (parallel <= 0) {
            pause(Error.OUTPUT);
            return;
        }
        // 能量按并行数与耗能倍率从能源仓网络一次性扣除
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

    // 配方匹配

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
                    // 单次抽取上限为物品堆叠上限，按需求量分批抽取
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
     * 输入份数、配方耐久与输出容量中的较小值。
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
                // 启动前已检查，兜底掉落避免产物丢失
                ae2pr.LOGGER.warn("熔岩冶炼炉产物无法放入输出总线，已掉落 {}", remaining);
                Block.popResource(level, worldPosition, remaining);
            }
        }
    }

    // 加速卡与电力

    public int maxParallel() {
        return Math.max(1, Config.lavaSmelterParallel());
    }

    public int getSpeedCardCount() {
        return upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
    }

    public double getSpeedMultiplier() {
        return 1.0 + getSpeedCardCount() * Config.lossySpeedPerCard();
    }

    public double lossyEnergyMultiplier() {
        return Math.pow(Config.lossyEnergyMultiplierPerCard(), getSpeedCardCount());
    }

    private int scaledDuration(int baseTicks) {
        return Math.max(1, (int) Math.ceil(baseTicks / getSpeedMultiplier()));
    }

    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private void onUpgradesChanged() {
        setChanged();
    }

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

    public boolean isEnergyConnected() {
        for (FluixCrystalEnergyHatchBlockEntity hatch : energyHatches) {
            if (hatch.isGridConnected()) {
                return true;
            }
        }
        return false;
    }

    public double getNetworkStoredPower() {
        return FluixCrystalEnergyHatchBlockEntity.totalAvailableAEPower(energyHatches);
    }

    // 结构检测

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

    private void refreshParts(Level level, StructureResult result) {
        inputBuses.clear();
        outputBuses.clear();
        energyHatches.clear();
        if (!result.formed()) {
            return;
        }
        // 按 片 → 行 → 列 排序，保证部件顺序稳定
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

    private void applyControllerState() {
        applyControllerState(getBlockState().getValue(HighReactivityLavaSmelterBlock.FORMED));
    }

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

    private void dropDust(Level level) {
        ItemStack stack = dustSlot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition, stack.copy());
            dustSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    // 客户端同步

    private void syncClient() {
        Level level = this.level;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        // 默认返回空 tag，这里附带完整存档数据与镜像状态
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

    // 持久化

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
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse(tag.getString(JOB_RECIPE_ID)));
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

    // 能力

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

    // 状态查询

    public boolean isRunning() {
        return jobTicksLeft > 0 && isFormed();
    }

    public boolean isPaused() {
        return paused && isFormed();
    }

    public Error getError() {
        return error;
    }

    public boolean isFormed() {
        return getBlockState().getValue(HighReactivityLavaSmelterBlock.FORMED);
    }

    public int getDurability() {
        return durability;
    }

    public boolean isMirrorSide() {
        return mirrorSide;
    }

    public boolean isMirrorFront() {
        return mirrorFront;
    }

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

    public int getJobDuration() {
        return currentRecipe != null && jobTicksLeft > 0 ? jobTotalTicks : 0;
    }

    public int getJobElapsed() {
        return currentRecipe != null && jobTicksLeft > 0 ? getJobDuration() - jobTicksLeft : 0;
    }

    public ItemStack getClientJobOutput() {
        return clientJobOutput;
    }

    public void applyClientJobOutput(ItemStack stack) {
        this.clientJobOutput = stack;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // 结构状态在加载后重新检测
            tickCounter = CHECK_INTERVAL - 1;
        }
    }
}
