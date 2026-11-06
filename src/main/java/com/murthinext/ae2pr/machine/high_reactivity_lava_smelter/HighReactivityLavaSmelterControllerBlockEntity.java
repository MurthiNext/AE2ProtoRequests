package com.murthinext.ae2pr.machine.high_reactivity_lava_smelter;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;

public class HighReactivityLavaSmelterControllerBlockEntity extends AbstractMultiblockControllerBlockEntity {

    public static final int DUST_SLOT_CAPACITY = 64;
    private static final float LAVA_DAMAGE = 4.0F;
    private static final int POWER_SHORTAGE_INTERVAL = 20;
    private static final ResourceLocation SKY_DUST_ID = ResourceLocation.fromNamespaceAndPath("ae2", "sky_dust");
    private static final String DURABILITY_ID = "durability";
    private static final String DUST_ID = "dust";

    private int powerShortageTicks;
    private int durability;

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

    public HighReactivityLavaSmelterControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER, pos, state);
    }

    @Override
    protected void tickMachine() {
        absorbDust();
        tickPassivePower();
        tickCavityDamage();
    }

    @Override
    protected double energyCost(Match match) {
        // 冶炼炉的电力由被动耗电单独处理
        return 0;
    }

    @Override
    protected int capParallel(MultiblockRecipe recipe, int parallel) {
        return Math.min(parallel, durability);
    }

    @Override
    protected Error parallelShortageError() {
        return Error.DURABILITY;
    }

    @Override
    protected void onJobStarted(Match match) {
        durability -= match.parallel();
        setChanged();
    }

    // 配方耐久

    // 陨石粉转耐久：仅当一批耐久能完整存入时才消耗
    private void absorbDust() {
        int max = ModConfig.highReactivityLavaSmelterMaxDurability();
        int perDust = Math.min(ModConfig.highReactivityLavaSmelterDurabilityPerDust(), max);
        if (durability > max - perDust) {
            return;
        }
        ItemStack stack = dustSlot.getStackInSlot(0);
        if (stack.isEmpty() || !isSkyDust(stack)) {
            return;
        }
        if (dustSlot.extractItem(0, 1, false).isEmpty()) {
            return;
        }
        durability += perDust;
        markStateChanged();
    }

    private static boolean isSkyDust(ItemStack stack) {
        Item item = ForgeRegistries.ITEMS.getValue(SKY_DUST_ID);
        return item != null && stack.is(item);
    }

    // 炉膛空腔伤害：耐久 > 0 时为熔岩 4 倍，否则 2 倍
    private void tickCavityDamage() {
        Level level = this.level;
        if (level == null || !isFormed()) {
            return;
        }
        AABB cavity = HighReactivityLavaSmelterStructure.cavityAabb(worldPosition, getFacing(), mirrorSide, mirrorFront);
        float damage = LAVA_DAMAGE * (durability > 0 ? 4.0F : 2.0F);
        for (Player player : level.getEntitiesOfClass(Player.class, cavity)) {
            if (!player.fireImmune()) {
                player.hurt(player.damageSources().lava(), damage);
            }
        }
    }

    // 结构成型后持续抽取固定能量，供给不足时每秒按概率损失 1 点配方耐久
    private void tickPassivePower() {
        if (!isFormed()) {
            powerShortageTicks = 0;
            return;
        }
        double drain = ModConfig.highReactivityLavaSmelterPassiveEnergy();
        if (drain <= 0 || extractNetworkEnergy(drain, false) >= drain) {
            powerShortageTicks = 0;
            return;
        }
        if (++powerShortageTicks < POWER_SHORTAGE_INTERVAL) {
            return;
        }
        powerShortageTicks = 0;
        if (durability > 0 && level.random.nextDouble() < ModConfig.highReactivityLavaSmelterDurabilityLossChance()) {
            durability--;
            markStateChanged();
        }
    }

    // 能力与持久化

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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(DURABILITY_ID, durability);
        tag.put(DUST_ID, dustSlot.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        durability = tag.getInt(DURABILITY_ID);
        if (tag.contains(DUST_ID)) {
            dustSlot.deserializeNBT(tag.getCompound(DUST_ID));
        }
    }

    @Override
    protected void onControllerRemovedHook() {
        Level level = this.level;
        if (level == null) {
            return;
        }
        ItemStack stack = dustSlot.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Block.popResource(level, worldPosition, stack.copy());
            dustSlot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    public int getDurability() {
        return durability;
    }
}
