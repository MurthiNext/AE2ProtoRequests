package com.murthinext.ae2pr.logic.repeat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * 服务端重复轮数登记表，带时效防止串单。
 */
public final class ServerRepeatRegistry {
    private static final long MAX_AGE_MILLIS = 5 * 60 * 1000L;
    private static final long RESTORE_MAX_AGE_MILLIS = 2 * 1000L;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Pending> RESTORE = new HashMap<>();

    private ServerRepeatRegistry() {
    }

    public static void put(UUID playerId, int rounds) {
        PENDING.put(playerId, new Pending(rounds, System.currentTimeMillis()));
    }

    @Nullable
    public static Integer take(UUID playerId) {
        var pending = PENDING.remove(playerId);
        if (pending == null || System.currentTimeMillis() - pending.timestamp > MAX_AGE_MILLIS) {
            return null;
        }
        return pending.rounds;
    }

    @Nullable
    public static Integer peek(UUID playerId) {
        var pending = PENDING.get(playerId);
        if (pending == null || System.currentTimeMillis() - pending.timestamp > MAX_AGE_MILLIS) {
            return null;
        }
        return pending.rounds;
    }

    public static void clear(UUID playerId) {
        PENDING.remove(playerId);
    }

    /**
     * 把待确认轮数转入恢复队列，供数量菜单回填。
     */
    public static void pushRestore(UUID playerId) {
        var pending = PENDING.remove(playerId);
        if (pending != null) {
            RESTORE.put(playerId, new Pending(pending.rounds, System.currentTimeMillis()));
        }
    }

    /**
     * 消费恢复队列，仅接受短时效值。
     */
    @Nullable
    public static Integer consumeRestore(UUID playerId) {
        var restore = RESTORE.remove(playerId);
        if (restore == null || System.currentTimeMillis() - restore.timestamp > RESTORE_MAX_AGE_MILLIS) {
            return null;
        }
        return restore.rounds;
    }

    public static void clearRestore(UUID playerId) {
        RESTORE.remove(playerId);
    }

    private record Pending(int rounds, long timestamp) {
    }
}
