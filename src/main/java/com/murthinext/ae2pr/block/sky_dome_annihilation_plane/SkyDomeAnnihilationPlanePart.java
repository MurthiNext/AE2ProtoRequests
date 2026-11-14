package com.murthinext.ae2pr.block.sky_dome_annihilation_plane;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.api.util.AECableType;
import appeng.core.definitions.AEItems;
import appeng.items.parts.PartModels;
import appeng.me.helpers.MachineSource;
import appeng.parts.AEBasePart;
import appeng.parts.PartModel;

import com.murthinext.ae2pr.api.Ids;

public class SkyDomeAnnihilationPlanePart extends AEBasePart implements IGridTickable {

    public static final int MIN_PRODUCTION = 32;
    public static final int MAX_PRODUCTION = 128;

    private static final int TICKS_PER_MINUTE = 1200;
    private static final int TICK_INTERVAL = 5;

    @PartModels
    public static final ResourceLocation MODEL_OFF = Ids.of("part/sky_dome_annihilation_plane");
    @PartModels
    public static final ResourceLocation MODEL_POWERED = Ids.of("part/sky_dome_annihilation_plane_powered");
    @PartModels
    public static final ResourceLocation MODEL_ACTIVE = Ids.of("part/sky_dome_annihilation_plane_active");

    private static final IPartModel OFF = new PartModel(MODEL_OFF);
    private static final IPartModel POWERED = new PartModel(MODEL_POWERED);
    private static final IPartModel ACTIVE = new PartModel(MODEL_ACTIVE);

    private final IActionSource actionSource = new MachineSource(this);
    private int productionRate;
    private int minuteTicks;
    private int progress;

    public SkyDomeAnnihilationPlanePart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL).setIdlePowerUsage(4);
        getMainNode().addService(IGridTickable.class, this);
    }

    public static boolean canPlaceAt(@Nullable Level level, BlockPos pos, @Nullable Direction side) {
        // 存档反序列化时宿主可能尚未绑定世界，高度在入世后再次校验。
        return side == Direction.UP && (level == null || pos.getY() == level.getMaxBuildHeight() - 1);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TICK_INTERVAL, TICK_INTERVAL, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var level = getLevel();
        if (level.isClientSide() || !getMainNode().isActive() ||
            !canPlaceAt(level, getBlockEntity().getBlockPos(), getSide())) {
            return TickRateModulation.SAME;
        }

        var grid = node.getGrid();
        var dust = AEItemKey.of(AEItems.SKY_DUST);
        var energy = grid.getEnergyService();
        var inventory = grid.getStorageService().getInventory();
        if (StorageHelper.poweredInsert(energy, inventory, dust, 1, actionSource, Actionable.SIMULATE) != 1) {
            return TickRateModulation.SAME;
        }

        if (productionRate == 0) {
            productionRate = MIN_PRODUCTION + level.random.nextInt(MAX_PRODUCTION - MIN_PRODUCTION + 1);
        }

        // 不补算断电、卸载或网络阻塞期间的产量。
        int elapsedTicks = Math.min(Math.min(ticksSinceLastCall, TICK_INTERVAL), TICKS_PER_MINUTE - minuteTicks);
        int nextProgress = progress + productionRate * elapsedTicks;
        if (nextProgress >= TICKS_PER_MINUTE) {
            if (StorageHelper.poweredInsert(energy, inventory, dust, 1, actionSource, Actionable.MODULATE) != 1) {
                return TickRateModulation.SAME;
            }
            nextProgress -= TICKS_PER_MINUTE;
        }

        progress = nextProgress;
        minuteTicks += elapsedTicks;
        if (minuteTicks == TICKS_PER_MINUTE) {
            minuteTicks = 0;
            productionRate = MIN_PRODUCTION + level.random.nextInt(MAX_PRODUCTION - MIN_PRODUCTION + 1);
        }
        getHost().markForSave();
        return TickRateModulation.SAME;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        int rate = data.getInt("productionRate");
        productionRate = rate == 0 ? 0 : Mth.clamp(rate, MIN_PRODUCTION, MAX_PRODUCTION);
        minuteTicks = Mth.clamp(data.getInt("minuteTicks"), 0, TICKS_PER_MINUTE - 1);
        progress = Mth.clamp(data.getInt("progress"), 0, TICKS_PER_MINUTE - 1);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putInt("productionRate", productionRate);
        data.putInt("minuteTicks", minuteTicks);
        data.putInt("progress", progress);
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        helper.addBox(1, 1, 15, 15, 15, 16);
        helper.addBox(5, 5, 14, 11, 11, 15);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive()) {
            return ACTIVE;
        }
        return isPowered() ? POWERED : OFF;
    }
}
