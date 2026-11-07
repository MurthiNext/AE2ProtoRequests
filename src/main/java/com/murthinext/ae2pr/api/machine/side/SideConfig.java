package com.murthinext.ae2pr.api.machine.side;

import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class SideConfig {

    // 菜单按钮 id：0-5 对应 Direction.values() 顺序，其余为全局开关
    public static final int BUTTON_AUTO_PULL = 6;
    public static final int BUTTON_AUTO_PUSH = 7;
    public static final int BUTTON_CLEAR = 8;

    private static final String TAG_MODES = "modes";
    private static final String TAG_AUTO_PULL = "autoPull";
    private static final String TAG_AUTO_PUSH = "autoPush";

    private final EnumMap<Direction, SideMode> modes = new EnumMap<>(Direction.class);
    private boolean autoPull;
    private boolean autoPush;

    public SideConfig() {
        reset(SideMode.INPUT);
    }

    public SideMode get(Direction dir) {
        return modes.getOrDefault(dir, SideMode.INPUT);
    }

    public void reset(SideMode mode) {
        for (Direction dir : Direction.values()) {
            modes.put(dir, mode);
        }
    }

    public boolean isAutoPull() {
        return autoPull;
    }

    public boolean isAutoPush() {
        return autoPush;
    }

    // 菜单按钮：0-5 循环切换对应面，6/7 切换自动拉取/推出，8 全部清空
    public boolean pressButton(int id) {
        Direction[] directions = Direction.values();
        if (id >= 0 && id < directions.length) {
            Direction dir = directions[id];
            modes.put(dir, get(dir).next());
            return true;
        }
        switch (id) {
            case BUTTON_AUTO_PULL -> {
                autoPull = !autoPull;
                return true;
            }
            case BUTTON_AUTO_PUSH -> {
                autoPush = !autoPush;
                return true;
            }
            case BUTTON_CLEAR -> {
                reset(SideMode.NONE);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static int directionButton(Direction dir) {
        return dir.ordinal();
    }

    public void save(CompoundTag tag) {
        CompoundTag modesTag = new CompoundTag();
        for (Direction dir : Direction.values()) {
            modesTag.putString(dir.getName(), get(dir).name());
        }
        tag.put(TAG_MODES, modesTag);
        tag.putBoolean(TAG_AUTO_PULL, autoPull);
        tag.putBoolean(TAG_AUTO_PUSH, autoPush);
    }

    public void load(CompoundTag tag) {
        reset(SideMode.INPUT);
        if (tag.contains(TAG_MODES)) {
            CompoundTag modesTag = tag.getCompound(TAG_MODES);
            for (Direction dir : Direction.values()) {
                String name = modesTag.getString(dir.getName());
                if (name.isEmpty()) {
                    continue;
                }
                try {
                    modes.put(dir, SideMode.valueOf(name));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        autoPull = tag.getBoolean(TAG_AUTO_PULL);
        autoPush = tag.getBoolean(TAG_AUTO_PUSH);
    }

    public void writeToStream(FriendlyByteBuf buffer) {
        for (Direction dir : Direction.values()) {
            buffer.writeByte(get(dir).ordinal());
        }
        buffer.writeBoolean(autoPull);
        buffer.writeBoolean(autoPush);
    }

    public void readFromStream(FriendlyByteBuf buffer) {
        SideMode[] values = SideMode.values();
        for (Direction dir : Direction.values()) {
            int ordinal = buffer.readUnsignedByte();
            modes.put(dir, ordinal < values.length ? values[ordinal] : SideMode.INPUT);
        }
        autoPull = buffer.readBoolean();
        autoPush = buffer.readBoolean();
    }
}
