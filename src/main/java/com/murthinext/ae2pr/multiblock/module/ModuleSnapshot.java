package com.murthinext.ae2pr.multiblock.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * 结构成型时收集的模块快照：提供按功能的数量、等级查询，以及配方要求校验。
 * <p>
 * 快照只描述结构内实际安装了什么，具体的并行、速度等增益由使用方按功能类型换算。
 */
public final class ModuleSnapshot {

    public static final ModuleSnapshot EMPTY = new ModuleSnapshot(List.of());

    private final List<ModuleInstance> modules;
    private final Map<ModuleType, Integer> counts = new HashMap<>();
    private final Map<ModuleType, Integer> levels = new HashMap<>();

    private ModuleSnapshot(List<ModuleInstance> modules) {
        this.modules = modules;
        for (ModuleInstance module : modules) {
            ModuleDefinition definition = module.definition();
            counts.merge(definition.type(), 1, Integer::sum);
            levels.merge(definition.type(), definition.level(), Math::max);
        }
    }

    public static ModuleSnapshot of(List<ModuleInstance> modules) {
        return modules.isEmpty() ? EMPTY : new ModuleSnapshot(List.copyOf(modules));
    }

    /** 结构内的全部模块。 */
    public List<ModuleInstance> modules() {
        return modules;
    }

    /** 指定功能已安装的数量。 */
    public int count(ModuleType type) {
        return counts.getOrDefault(type, 0);
    }

    /** 指定功能的最高等级；未安装返回 0。 */
    public int level(ModuleType type) {
        return levels.getOrDefault(type, 0);
    }

    /** 是否满足全部模块要求（未声明要求视为满足）。 */
    public boolean satisfies(List<ModuleRequirement> requirements) {
        for (ModuleRequirement requirement : requirements) {
            if (level(requirement.type()) < requirement.minLevel()) {
                return false;
            }
        }
        return true;
    }

    /** 首个超出安装上限的模块；全部合法时返回 null。 */
    @Nullable
    public static ModuleInstance firstConflict(List<ModuleInstance> modules) {
        Map<ModuleType, Integer> seen = new HashMap<>();
        for (ModuleInstance module : modules) {
            ModuleType type = module.definition().type();
            int count = seen.merge(type, 1, Integer::sum);
            if (type.maxCount() >= 0 && count > type.maxCount()) {
                return module;
            }
        }
        return null;
    }
}
