package com.murthinext.ae2pr.client.crystal_workstation;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlock;
import com.murthinext.ae2pr.multiblock.structure.CrystalWorkstationStructure;

/**
 * 晶能工作站成型外观的客户端缓存。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT)
public final class CrystalWorkstationShell {

    private static final int PRUNE_INTERVAL = 10;

    public record Look(CrystalWorkstationStructure.PartRole role, @Nullable Direction outward,
            @Nullable Direction depthAxis, int panelX, int panelY) {
    }

    private record Snapshot(Map<BlockPos, Look> looks, BlockPos min, BlockPos max, int signature) {
    }

    private static final Map<BlockPos, Snapshot> MACHINES = new HashMap<>();
    // 区块网格在后台线程构建，索引整体替换后以 volatile 发布
    private static volatile Map<BlockPos, Look> index = Map.of();

    @Nullable
    private static ClientLevel level;
    private static int pruneTicks;

    private CrystalWorkstationShell() {
    }

    // 单元格的成型外观
    @Nullable
    public static Look look(BlockPos pos) {
        return index.get(pos);
    }

    public static boolean isStability(BlockPos pos) {
        Look look = index.get(pos);
        return look != null && look.role() == CrystalWorkstationStructure.PartRole.STABILITY;
    }

    public static boolean isCore(BlockPos pos) {
        Look look = index.get(pos);
        return look != null && look.role() == CrystalWorkstationStructure.PartRole.CORE;
    }

    public static boolean bordersStability(BlockPos pos) {
        if (index.isEmpty()) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            if (isStability(pos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    public static void update(CrystalWorkstationControllerBlockEntity controller) {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null || controller.getLevel() != current) {
            return;
        }
        if (level != current) {
            reset();
            level = current;
        }
        BlockPos controllerPos = controller.getBlockPos();
        Snapshot previous = MACHINES.get(controllerPos);
        int signature = signature(controller);
        if (previous != null && previous.signature() == signature) {
            return;
        }
        refresh(controllerPos, previous, controller, signature);
    }

    private static void refresh(BlockPos controllerPos, @Nullable Snapshot previous,
            CrystalWorkstationControllerBlockEntity controller, int signature) {
        Map<BlockPos, Look> looks = controller.isFormed() ? collect(controller) : Map.of();
        if (looks.isEmpty()) {
            if (previous == null) {
                return;
            }
            MACHINES.remove(controllerPos);
            rebuildIndex();
            markDirty(previous, null);
            return;
        }
        Snapshot next = snapshot(looks, signature);
        MACHINES.put(controllerPos, next);
        rebuildIndex();
        markDirty(previous, next);
    }

    private static int signature(CrystalWorkstationControllerBlockEntity controller) {
        int signature = controller.isFormed() ? 1 : 0;
        if (controller.isMirrorSide()) {
            signature |= 2;
        }
        if (controller.isMirrorFront()) {
            signature |= 4;
        }
        return signature | controller.getBlockState().getValue(AbstractMultiblockControllerBlock.FACING).ordinal() << 3;
    }

    private static Map<BlockPos, Look> collect(CrystalWorkstationControllerBlockEntity controller) {
        Direction facing = controller.getBlockState().getValue(AbstractMultiblockControllerBlock.FACING);
        Map<BlockPos, Look> looks = new HashMap<>();
        for (CrystalWorkstationStructure.Part part : CrystalWorkstationStructure.effectParts(
                controller.getBlockPos(), facing, controller.isMirrorSide(), controller.isMirrorFront())) {
            looks.put(part.pos(),
                    new Look(part.role(), part.outward(), part.depthAxis(), part.panelX(), part.panelY()));
        }
        return looks;
    }

    private static Snapshot snapshot(Map<BlockPos, Look> looks, int signature) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : looks.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new Snapshot(Map.copyOf(looks), new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ),
                signature);
    }

    private static void rebuildIndex() {
        Map<BlockPos, Look> merged = new HashMap<>();
        for (Snapshot snapshot : MACHINES.values()) {
            merged.putAll(snapshot.looks());
        }
        index = Map.copyOf(merged);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ClientLevel current = Minecraft.getInstance().level;
        if (current == null) {
            reset();
            return;
        }
        if (level != current) {
            reset();
            level = current;
            return;
        }
        if (MACHINES.isEmpty() || ++pruneTicks < PRUNE_INTERVAL) {
            return;
        }
        pruneTicks = 0;
        for (BlockPos pos : MACHINES.keySet().toArray(BlockPos[]::new)) {
            if (!current.hasChunkAt(pos)) {
                // 区块未加载，保留缓存等待主机重新上报
                continue;
            }
            BlockEntity blockEntity = current.getBlockEntity(pos);
            Snapshot previous = MACHINES.get(pos);
            if (blockEntity instanceof CrystalWorkstationControllerBlockEntity controller) {
                if (controller.isFormed() && previous.signature() == signature(controller)) {
                    continue;
                }
                refresh(pos, previous, controller, signature(controller));
            } else {
                remove(pos, previous);
            }
        }
    }

    private static void remove(BlockPos controllerPos, Snapshot previous) {
        MACHINES.remove(controllerPos);
        rebuildIndex();
        markDirty(previous, null);
    }

    private static void reset() {
        // 切换世界时旧网格区域已失效，无需再触发重建
        MACHINES.clear();
        index = Map.of();
        level = null;
        pruneTicks = 0;
    }

    private static void markDirty(@Nullable Snapshot a, @Nullable Snapshot b) {
        BlockPos min;
        BlockPos max;
        if (a == null) {
            if (b == null) {
                return;
            }
            min = b.min();
            max = b.max();
        } else if (b == null) {
            min = a.min();
            max = a.max();
        } else {
            min = new BlockPos(Math.min(a.min().getX(), b.min().getX()), Math.min(a.min().getY(), b.min().getY()),
                    Math.min(a.min().getZ(), b.min().getZ()));
            max = new BlockPos(Math.max(a.max().getX(), b.max().getX()), Math.max(a.max().getY(), b.max().getY()),
                    Math.max(a.max().getZ(), b.max().getZ()));
        }
        var levelRenderer = Minecraft.getInstance().levelRenderer;
        if (levelRenderer != null) {
            levelRenderer.setBlocksDirty(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        }
    }
}
