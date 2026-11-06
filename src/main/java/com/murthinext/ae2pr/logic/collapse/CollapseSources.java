package com.murthinext.ae2pr.logic.collapse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.capabilities.Capabilities;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import com.murthinext.ae2pr.api.collapse.CollapseProvider;

// 内容来源探测
public final class CollapseSources {

    private static final List<CollapseProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private CollapseSources() {
    }

    public static void register(CollapseProvider provider) {
        PROVIDERS.add(provider);
    }

    public static List<CollapseSource> of(BlockEntity blockEntity) {
        for (var provider : PROVIDERS) {
            var collapsible = provider.create(blockEntity);
            if (collapsible != null) {
                return List.of(CollapseSource.of(CollapseHandler.idOf(blockEntity), collapsible));
            }
        }
        if (blockEntity instanceof IPartHost partHost) {
            return ofParts(partHost);
        }
        var source = directSource(blockEntity);
        return source == null ? List.of() : List.of(source);
    }

    @Nullable
    public static CollapseSource of(IPart part) {
        GenericInternalInventory generic = part.getCapability(Capabilities.GENERIC_INTERNAL_INV).orElse(null);
        if (generic == null) {
            return null;
        }
        return CollapseSource.of(BuiltInRegistries.ITEM.getKey(part.getPartItem().asItem()), generic);
    }

    @Nullable
    private static CollapseSource directSource(BlockEntity blockEntity) {
        var id = CollapseHandler.idOf(blockEntity);
        GenericInternalInventory generic = blockEntity.getCapability(Capabilities.GENERIC_INTERNAL_INV, null)
                .orElse(null);
        if (generic != null) {
            return CollapseSource.of(id, generic);
        }
        var handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        return handler == null ? null : CollapseSource.of(id, handler);
    }

    private static List<CollapseSource> ofParts(IPartHost partHost) {
        var sources = new ArrayList<CollapseSource>(Direction.values().length + 1);
        addPart(partHost.getPart(null), sources);
        for (var side : Direction.values()) {
            addPart(partHost.getPart(side), sources);
        }
        return sources;
    }

    private static void addPart(@Nullable IPart part, List<CollapseSource> sources) {
        var source = part == null ? null : of(part);
        if (source != null) {
            sources.add(source);
        }
    }
}
