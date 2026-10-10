package com.murthinext.ae2pr;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.multiblock.module.ModuleType;

/**
 * 模块功能类型注册入口：配方与结构通过功能标识引用模块，与具体方块注册名解耦。
 */
public final class ModModules {

    private ModModules() {
    }

    /** 充能：解锁充能配方；同结构内只允许安装一个（不同等级共享上限） */
    public static final ModuleType CHARGING = ModuleType.of("charging", 1);

    /** 并行：提升并行上限，可叠加 */
    public static final ModuleType PARALLEL = ModuleType.of("parallel", -1);

    /** 速度：缩短加工时间，可叠加 */
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

    /** 按功能标识查询；未知返回 null。 */
    @Nullable
    public static ModuleType byId(@Nullable ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }
}
