package com.murthinext.ae2pr;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(
            ForgeRegistries.SOUND_EVENTS, ae2pr.MODID);

    public static final RegistryObject<SoundEvent> ASSEMBLY_LINE = register("assembly_line_processing");
    public static final RegistryObject<SoundEvent> LAVA_SMELTER = register("lava_smelter_processing");
    public static final RegistryObject<SoundEvent> MATTER_PHASE_TRANSFORMER = register("matter_phase_transformer_processing");
    public static final RegistryObject<SoundEvent> CRYSTAL_REACTION = register("crystal_reaction_processing");
    public static final RegistryObject<SoundEvent> CRYSTAL_WORKSTATION_ETCHING = register("crystal_workstation_etching");
    public static final RegistryObject<SoundEvent> CRYSTAL_WORKSTATION_CUTTING = register("crystal_workstation_cutting");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, name)));
    }
}
