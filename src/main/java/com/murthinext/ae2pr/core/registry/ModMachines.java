package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.api.machine.MachineControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.api.machine.singleblock.SingleblockMachineDefinition;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationControllerBlockEntity;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.machine.integrated_mass_fabricator.IntegratedMassFabricatorControllerBlockEntity;
import com.murthinext.ae2pr.machine.jig.JigControllerBlockEntity;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryControllerBlockEntity;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlockEntity;
import java.util.List;

public final class ModMachines {

    private ModMachines() {}

    // 由模组构造阶段显式引导，避免首次访问落在注册事件之后
    public static void register() {
        MachineCatalog.bootstrap();
    }

    public static MultiblockMachineDefinition<MachineControllerBlockEntity> filterPress() {
        return MachineCatalog.get().filterPress;
    }

    public static MultiblockMachineDefinition<JigControllerBlockEntity> jig() {
        return MachineCatalog.get().jig;
    }

    public static MultiblockMachineDefinition<CrystalReactionChamberControllerBlockEntity> crystalReactionChamber() {
        return MachineCatalog.get().crystalReactionChamber;
    }

    public static MultiblockMachineDefinition<CrystalAssemblyLineControllerBlockEntity> crystalAssemblyLine() {
        return MachineCatalog.get().crystalAssemblyLine;
    }

    public static MultiblockMachineDefinition<MatterPhaseTransformerControllerBlockEntity> matterPhaseTransformer() {
        return MachineCatalog.get().matterPhaseTransformer;
    }

    public static MultiblockMachineDefinition<HighReactivityLavaSmelterControllerBlockEntity>
            highReactivityLavaSmelter() {
        return MachineCatalog.get().highReactivityLavaSmelter;
    }

    public static MultiblockMachineDefinition<CrystalWorkstationControllerBlockEntity> crystalWorkstation() {
        return MachineCatalog.get().crystalWorkstation;
    }

    public static MultiblockMachineDefinition<MultiFactoryControllerBlockEntity> multiFactory() {
        return MachineCatalog.get().multiFactory;
    }

    public static MultiblockMachineDefinition<IntegratedMassFabricatorControllerBlockEntity>
            integratedMassFabricator() {
        return MachineCatalog.get().integratedMassFabricator;
    }

    public static SingleblockMachineDefinition<NamingFactoryBlockEntity> namingFactory() {
        return MachineCatalog.get().namingFactory;
    }

    public static MachineDefinition<CrystalReactorCoreBlockEntity> crystalReactor() {
        return MachineCatalog.get().crystalReactor;
    }

    public static List<MachineDefinition<?>> all() {
        return MachineCatalog.get().all;
    }
}
