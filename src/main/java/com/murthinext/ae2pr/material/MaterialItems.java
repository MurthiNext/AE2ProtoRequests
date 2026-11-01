package com.murthinext.ae2pr.material;

import com.murthinext.ae2pr.ae2pr;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MaterialItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ae2pr.MODID);

    private static final Map<Key, RegistryObject<Item>> REGISTRY = new LinkedHashMap<>();

    static {
        for (Material material : Materials.all()) {
            for (TagPrefix prefix : TagPrefix.VALUES) {
                if (material.has(prefix)) {
                    REGISTRY.put(new Key(prefix, material),
                            ITEMS.register(prefix.idFor(material.id()), () -> prefix.createItem(material)));
                }
            }
        }
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

    private record Key(TagPrefix prefix, Material material) {
    }

    private MaterialItems() {
    }
}
