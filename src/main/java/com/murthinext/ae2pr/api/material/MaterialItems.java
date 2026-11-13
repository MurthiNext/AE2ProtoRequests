package com.murthinext.ae2pr.api.material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class MaterialItems {

    private static final Map<Key, RegistryObject<Item>> REGISTRY = new LinkedHashMap<>();

    /**
     * 按材质声明的形态向给定注册表登记物品，返回该材质本身，便于在注册类里直接声明字段。
     */
    public static Material register(DeferredRegister<Item> items, Material material) {
        for (TagPrefix prefix : TagPrefix.VALUES) {
            if (!material.has(prefix)) {
                continue;
            }
            Key key = new Key(prefix, material);
            if (REGISTRY.containsKey(key)) {
                throw new IllegalStateException("Duplicate item for %s %s".formatted(prefix.name(), material.id()));
            }
            REGISTRY.put(key, items.register(prefix.idFor(material.id()), () -> prefix.createItem(material)));
        }
        return material;
    }

    public static Collection<RegistryObject<Item>> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static RegistryObject<Item> get(TagPrefix prefix, Material material) {
        RegistryObject<Item> item = REGISTRY.get(new Key(prefix, material));
        if (item == null) {
            throw new IllegalArgumentException("No item for %s %s".formatted(prefix.name(), material.id()));
        }
        return item;
    }

    private record Key(TagPrefix prefix, Material material) {}

    private MaterialItems() {
    }
}
