package com.murthinext.ae2pr.block.entangled_casing;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntangledCasingPulse {

    static final int FRAME_COUNT = 8;
    private static final int HOP_TICKS = 4;
    private static final int QUIET_TICKS = 52;
    private static final int MAX_WAVES = 16;
    private static final int MAX_WAVE_BLOCKS = 4096;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static final List<Wave> WAVES = new ArrayList<>();
    private static final LongOpenHashSet RESERVED = new LongOpenHashSet();
    private static final Long2LongOpenHashMap FLASHES = new Long2LongOpenHashMap();

    @Nullable
    private static ClientLevel currentLevel;

    @Nullable
    private static BlockPos pendingSeed;

    private static long tick;

    private EntangledCasingPulse() {}

    public static void offer(Level level, BlockPos pos) {
        if (level instanceof ClientLevel clientLevel) {
            useLevel(clientLevel);
            if (pendingSeed == null && WAVES.size() < MAX_WAVES && !RESERVED.contains(pos.asLong())) {
                pendingSeed = pos.immutable();
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        useLevel(level);
        if (level == null || minecraft.isPaused()) {
            return;
        }

        tick++;
        var flashes = Long2LongMaps.fastIterator(FLASHES);
        while (flashes.hasNext()) {
            if (tick - flashes.next().getLongValue() >= FRAME_COUNT) {
                flashes.remove();
            }
        }
        WAVES.removeIf(wave -> {
            if (wave.advance(level)) {
                RESERVED.removeAll(wave.component);
                return true;
            }
            return false;
        });
        if (pendingSeed != null) {
            if (isCasing(level, pendingSeed)) {
                var wave = new Wave(level, pendingSeed);
                WAVES.add(wave);
                RESERVED.addAll(wave.component);
            }
            pendingSeed = null;
        }
    }

    static boolean hasFlashes(ClientLevel level) {
        return currentLevel == level && !FLASHES.isEmpty();
    }

    static void forEachFlash(ObjIntConsumer<BlockPos> consumer) {
        var flashes = Long2LongMaps.fastIterator(FLASHES);
        while (flashes.hasNext()) {
            var flash = flashes.next();
            consumer.accept(BlockPos.of(flash.getLongKey()), (int) (tick - flash.getLongValue()));
        }
    }

    private static void useLevel(@Nullable ClientLevel level) {
        if (currentLevel != level) {
            currentLevel = level;
            pendingSeed = null;
            tick = 0;
            WAVES.clear();
            RESERVED.clear();
            FLASHES.clear();
        }
    }

    static boolean isCasing(ClientLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(ModBlocks.ENTANGLED_CASING.get());
    }

    private static final class Wave {

        private final LongOpenHashSet component = new LongOpenHashSet();
        private final LongOpenHashSet reached = new LongOpenHashSet();
        private List<BlockPos> frontier;
        private long nextHop;

        private Wave(ClientLevel level, BlockPos seed) {
            // 预留同一连接组，避免一轮闪烁还没走完就从组内别处重新起波。
            var queue = new ArrayList<BlockPos>();
            queue.add(seed);
            component.add(seed.asLong());
            for (int i = 0; i < queue.size() && queue.size() < MAX_WAVE_BLOCKS; i++) {
                BlockPos pos = queue.get(i);
                for (Direction direction : DIRECTIONS) {
                    BlockPos neighbor = pos.relative(direction);
                    long key = neighbor.asLong();
                    if (!RESERVED.contains(key) && !component.contains(key) && isCasing(level, neighbor)) {
                        component.add(key);
                        queue.add(neighbor);
                        if (queue.size() == MAX_WAVE_BLOCKS) {
                            break;
                        }
                    }
                }
            }
            reached.add(seed.asLong());
            FLASHES.put(seed.asLong(), tick);
            frontier = List.of(seed);
            nextHop = tick + HOP_TICKS;
        }

        private boolean advance(ClientLevel level) {
            if (tick < nextHop) {
                return false;
            }
            if (frontier.isEmpty()) {
                return true;
            }
            var next = new ArrayList<BlockPos>();
            for (BlockPos pos : frontier) {
                if (!isCasing(level, pos)) {
                    continue;
                }
                for (Direction direction : DIRECTIONS) {
                    BlockPos neighbor = pos.relative(direction);
                    long key = neighbor.asLong();
                    if (component.contains(key) && !reached.contains(key) && isCasing(level, neighbor)) {
                        reached.add(key);
                        FLASHES.put(key, tick);
                        next.add(neighbor);
                    }
                }
            }
            frontier = next;
            nextHop = tick + (frontier.isEmpty() ? FRAME_COUNT - HOP_TICKS + QUIET_TICKS : HOP_TICKS);
            return false;
        }
    }
}
