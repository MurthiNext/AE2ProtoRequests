package com.murthinext.ae2pr;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.multiblock.StructureHooks;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.registry.ModNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLLoader;

public final class ModSelfCheck {

    private ModSelfCheck() {}

    public static void run() {
        if (FMLLoader.isProduction()) {
            return;
        }
        List<String> missing = new ArrayList<>();
        List<MachineDefinition<?, ?, ?>> machines = ModMachines.ALL;
        if (machines == null || machines.isEmpty()) {
            missing.add("ModMachines.ALL");
            machines = List.of();
        }
        for (var machine : machines) {
            if (!machine.blockHolder().isPresent()) {
                missing.add(machine.id() + ".block");
            }
            if (!machine.itemHolder().isPresent()) {
                missing.add(machine.id() + ".item");
            }
            if (!machine.blockEntityTypeHolder().isPresent()) {
                missing.add(machine.id() + ".blockEntityType");
            }
        }
        List<Block> parts;
        try {
            parts = StructureHooks.parts();
        } catch (IllegalStateException exception) {
            missing.add(exception.getMessage());
            parts = List.of();
        }
        if (parts.isEmpty()) {
            missing.add("StructureHooks.parts");
        }
        if (ModNetwork.messageCount() <= 0) {
            missing.add("ModNetwork messages");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("AE2PR self-check failed: " + String.join(", ", missing));
        }
        ae2pr.LOGGER.info(
                "AE2PR self-check passed: {} machines, {} structure parts, {} packets",
                machines.size(),
                parts.size(),
                ModNetwork.messageCount());
    }
}
