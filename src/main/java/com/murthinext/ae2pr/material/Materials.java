package com.murthinext.ae2pr.material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Materials {

    private static final Map<String, Material> REGISTRY = new LinkedHashMap<>();

    public static final Material METEOR_STEEL = Material.builder("meteor_steel")
            .ingot().plate().dust().color(0xE1EBF8).modelSet("meteor").buildAndRegister();
    public static final Material ZIRCONIUM = Material.builder("zirconium")
            .ingot().plate().dust().color(0xDDEDF4).buildAndRegister();
    public static final Material ZIRCONIUM_IRON_ALLOY = Material.builder("zirconium_iron_alloy")
            .ingot().color(0x6B7BB5).buildAndRegister();
    public static final Material STRONTIUM = Material.builder("strontium")
            .ingot().plate().dust().color(0xFDFAE0).buildAndRegister();
    public static final Material BARIUM = Material.builder("barium")
            .ingot().plate().dust().color(0xF5FCFF).buildAndRegister();

    public static final Material COAL = Material.builder("coal").dust().color(0x8D7867).buildAndRegister();
    public static final Material CARBON = Material.builder("carbon").dust().color(0x222528).buildAndRegister();
    public static final Material CARBON_ZIRCONIUM_MIXTURE = Material.builder("carbon_zirconium_mixture")
            .dust().color(0x6F818D).buildAndRegister();
    public static final Material ZIRCON = Material.builder("zircon").dust().color(0xE8E8E8).buildAndRegister();
    public static final Material COPPER_OXIDE = Material.builder("copper_oxide")
            .dust().color(0x33363A).buildAndRegister();
    public static final Material IRON_OXIDE = Material.builder("iron_oxide").dust().color(0xA55837).buildAndRegister();
    public static final Material GOLD_OXIDE = Material.builder("gold_oxide").dust().color(0xAF8D2E).buildAndRegister();
    public static final Material PSEUDO_STRONTIANITE = Material.builder("pseudo_strontianite")
            .dust().color(0xB2BD9D).buildAndRegister();
    public static final Material STRONTIUM_CARBONATE = Material.builder("strontium_carbonate")
            .dust().color(0xD9D9D0).buildAndRegister();
    public static final Material BARIUM_SULFATE = Material.builder("barium_sulfate")
            .dust().color(0xDCE0E5).buildAndRegister();

    public static Collection<Material> all() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    public static Material get(String id) {
        Material material = REGISTRY.get(id);
        if (material == null) {
            throw new IllegalArgumentException("Unknown material: " + id);
        }
        return material;
    }

    static void register(Material material) {
        if (REGISTRY.putIfAbsent(material.id(), material) != null) {
            throw new IllegalStateException("Duplicate material: " + material.id());
        }
    }

    private Materials() {
    }
}
