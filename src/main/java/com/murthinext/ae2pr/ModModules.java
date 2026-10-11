package com.murthinext.ae2pr;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.multiblock.module.ModuleType;

/**
 * 模块功能类型注册入口。
 */
public final class ModModules {

    private ModModules() {
    }

    public static final ModuleType CHARGING = ModuleType.of("charging", 1);

    public static final ModuleType PARALLEL = ModuleType.of("parallel", -1);

    public static final ModuleType SPEED = ModuleType.of("speed", -1);

    private static final Map<ResourceLocation, ModuleType> BY_ID = new HashMap<>();

    static {
        register(CHARGING);
        register(PARALLEL);
        register(SPEED);
    }

    private static void register(ModuleType type) {
        BY_ID.put(type.id(), type);
    }

    @Nullable
    public static ModuleType byId(@Nullable ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }
}
