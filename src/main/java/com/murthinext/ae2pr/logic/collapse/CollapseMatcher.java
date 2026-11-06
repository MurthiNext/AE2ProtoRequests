package com.murthinext.ae2pr.logic.collapse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.ModConfig;
import com.murthinext.ae2pr.ae2pr;

public final class CollapseMatcher {

    private static final Set<String> BUILT_IN_NAMESPACES = Set.of(ae2pr.MODID, "ae2");
    private static final String WILDCARD = "*";

    private static List<String> cachedRaw = List.of();
    private static Set<ResourceLocation> cachedExact = Set.of();
    private static Set<String> cachedNamespaces = Set.of();

    private CollapseMatcher() {
    }

    // 内建兼容
    public static boolean isBuiltIn(ResourceLocation id) {
        return id != null && BUILT_IN_NAMESPACES.contains(id.getNamespace());
    }

    public static boolean isExtra(ResourceLocation id) {
        if (id == null) {
            return false;
        }
        refresh();
        return cachedExact.contains(id) || cachedNamespaces.contains(id.getNamespace());
    }

    public static boolean allows(ResourceLocation id) {
        return isBuiltIn(id) || isExtra(id);
    }

    public static boolean allows(CollapseSource source) {
        return source.isAuthorized() || allows(source.id());
    }

    private static void refresh() {
        List<String> raw = ModConfig.collapseExtraBlocks();
        if (raw.equals(cachedRaw)) {
            return;
        }
        Set<ResourceLocation> exact = new HashSet<>();
        Set<String> namespaces = new HashSet<>();
        for (var entry : raw) {
            var trimmed = entry.trim();
            int split = trimmed.indexOf(':');
            if (split <= 0 || split == trimmed.length() - 1) {
                continue;
            }
            if (WILDCARD.equals(trimmed.substring(split + 1))) {
                namespaces.add(trimmed.substring(0, split));
                continue;
            }
            var parsed = ResourceLocation.tryParse(trimmed);
            if (parsed != null) {
                exact.add(parsed);
            }
        }
        cachedRaw = new ArrayList<>(raw);
        cachedExact = exact;
        cachedNamespaces = namespaces;
    }
}
