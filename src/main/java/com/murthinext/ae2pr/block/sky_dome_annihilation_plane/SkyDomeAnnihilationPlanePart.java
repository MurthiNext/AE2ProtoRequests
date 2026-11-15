package com.murthinext.ae2pr.block.sky_dome_annihilation_plane;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.model.data.ModelData;

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
import appeng.parts.automation.PlaneConnectionHelper;
import appeng.parts.automation.PlaneModelData;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.api.Ids;

public class SkyDomeAnnihilationPlanePart extends AEBasePart implements IGridTickable {

    private static final int TICKS_PER_MINUTE = 1200;
    private static final int TICK_INTERVAL = 5;
    private static final int EFFICIENCY_INTERVAL = 20;

    @PartModels
    public static final ResourceLocation MODEL_OFF = Ids.of("part/sky_dome_annihilation_plane");
    @PartModels
    public static final ResourceLocation MODEL_POWERED = Ids.of("part/sky_dome_annihilation_plane_powered");
    @PartModels
    public static final ResourceLocation MODEL_ACTIVE = Ids.of("part/sky_dome_annihilation_plane_active");
    @PartModels
    public static final ResourceLocation MODEL_SURFACE_OFF = Ids.of("part/sky_dome_annihilation_plane_surface");
    @PartModels
    public static final ResourceLocation MODEL_SURFACE_POWERED = Ids.of(
            "part/sky_dome_annihilation_plane_surface_powered");
    @PartModels
    public static final ResourceLocation MODEL_SURFACE_ACTIVE = Ids.of(
            "part/sky_dome_annihilation_plane_surface_active");

    private static final IPartModel OFF = new PartModel(MODEL_OFF, MODEL_SURFACE_OFF);
    private static final IPartModel POWERED = new PartModel(MODEL_POWERED, MODEL_SURFACE_POWERED);
    private static final IPartModel ACTIVE = new PartModel(MODEL_ACTIVE, MODEL_SURFACE_ACTIVE);

    private final IActionSource actionSource = new MachineSource(this);
    private final PlaneConnectionHelper connectionHelper = new PlaneConnectionHelper(this);
    private int efficiencyCooldown;
    private double efficiency;
    private double pendingDust;

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

        int elapsedTicks = Math.min(ticksSinceLastCall, TICK_INTERVAL);
        if (efficiencyCooldown <= 0) {
            efficiency = SkyDomePlaneGroup.efficiency(this);
            efficiencyCooldown = EFFICIENCY_INTERVAL;
        }
        efficiencyCooldown -= elapsedTicks;

        pendingDust += ServerConfig.skyDomeStandardYieldPerMinute() * efficiency * elapsedTicks / TICKS_PER_MINUTE;
        long amount = (long) pendingDust;
        if (amount > 0) {
            var grid = node.getGrid();
            long inserted = StorageHelper.poweredInsert(grid.getEnergyService(),
                                                        grid.getStorageService().getInventory(),
                                                        AEItemKey.of(AEItems.SKY_DUST), amount, actionSource,
                                                        Actionable.MODULATE);
            // 网络阻塞期间最多保留 1 个陨石粉，不做补算。
            pendingDust = Math.min(pendingDust - inserted, 1.0);
        }

        getHost().markForSave();
        return TickRateModulation.SAME;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        pendingDust = Mth.clamp(data.getDouble("pendingDust"), 0.0, 1.0);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putDouble("pendingDust", pendingDust);
    }

    @Override
    public void getBoxes(IPartCollisionHelper helper) {
        connectionHelper.getBoxes(helper);
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        connectionHelper.updateConnections();
        efficiencyCooldown = 0;
    }

    @Override
    public void onUpdateShape(Direction side) {
        if (side.getAxis() != getSide().getAxis()) {
            connectionHelper.updateConnections();
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(PlaneModelData.CONNECTIONS, connectionHelper.getConnections()).build();
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
