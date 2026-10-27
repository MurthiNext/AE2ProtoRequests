package com.murthinext.ae2pr.multiblock.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

// 结构成型时收集的模块快照：提供数量、等级查询与要求校验
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

    public List<ModuleInstance> modules() {
        return modules;
    }

    public int count(ModuleType type) {
        return counts.getOrDefault(type, 0);
    }

    public int level(ModuleType type) {
        return levels.getOrDefault(type, 0);
    }

    public boolean satisfies(List<ModuleRequirement> requirements) {
        for (ModuleRequirement requirement : requirements) {
            if (level(requirement.type()) < requirement.minLevel()) {
                return false;
            }
        }
        return true;
    }

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
