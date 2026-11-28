package com.murthinext.ae2pr.api.machine;

import appeng.api.stacks.AEKey;
import java.math.BigInteger;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

// 缓冲内容掉落的注入点，由塌缩实现安装
public final class CollapseSink {

    @FunctionalInterface
    public interface Sink {
        void dropBufferedContents(Level level, BlockPos pos, ResourceLocation id, Map<AEKey, BigInteger> keys);
    }

    private static Sink sink = (level, pos, id, keys) -> {};

    private static boolean installed;

    private CollapseSink() {}

    public static synchronized void install(Sink value) {
        if (installed) {
            throw new IllegalStateException("Collapse sink already installed");
        }
        installed = true;
        sink = value;
    }

    public static void dropBufferedContents(
            Level level, BlockPos pos, ResourceLocation id, Map<AEKey, BigInteger> keys) {
        sink.dropBufferedContents(level, pos, id, keys);
    }
}
