package com.murthinext.ae2pr.client;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.murthinext.ae2pr.registry.ModGuide;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;

import appeng.core.definitions.AEBlocks;

import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.sound.MachineProcessingSounds;
import com.murthinext.ae2pr.machine.AbstractMachineScreen;
import com.murthinext.ae2pr.machine.MachineDefinition;
import com.murthinext.ae2pr.machine.Mode;
import com.murthinext.ae2pr.machine.MachineModeButton;
import com.murthinext.ae2pr.machine.MachineScreen;
import com.murthinext.ae2pr.machine.MultiblockMachineDefinition;
import com.murthinext.ae2pr.machine.crystal_reaction_chamber.CrystalReactionChamberRenderer;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationRenderer;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationShell;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterRenderer;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterScreen;
import com.murthinext.ae2pr.machine.jig.JigRenderer;
import com.murthinext.ae2pr.machine.matter_phase_transformer.MatterPhaseTransformerRenderer;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryRenderer;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryRenderer;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryScreen;
import com.murthinext.ae2pr.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.machine.multiblock.AbstractMultiblockControllerMenu;

public final class MachineClients {

    private static final Map<MachineDefinition<?, ?, ?>, Style> STYLES = new IdentityHashMap<>();

    private MachineClients() {
    }

    public static void registerScreens() {
        register(ModMachines.FILTER_PRESS, Style.crystal(ModGuide.FILTER_PRESS_PAGE, 0x9CD3FF), MachineScreen::new,
                controller -> MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.FILTER_PRESS));
        register(ModMachines.JIG, Style.crystal(ModGuide.JIG_PAGE, 0x9CD3FF), MachineScreen::new,
                controller -> MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.JIG));
        register(ModMachines.CRYSTAL_REACTION_CHAMBER,
                Style.crystal(ModGuide.CRYSTAL_REACTION_CHAMBER_PAGE, 0x8EE6E0), MachineScreen::new,
                controller -> MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.CRYSTAL_REACTION));
        register(ModMachines.CRYSTAL_ASSEMBLY_LINE,
                Style.crystal(ModGuide.CRYSTAL_ASSEMBLY_LINE_PAGE, 0x55FFFF), MachineScreen::new,
                controller -> MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.CRYSTAL_ASSEMBLY_LINE));
        register(ModMachines.MATTER_PHASE_TRANSFORMER,
                Style.ember(ModGuide.MATTER_PHASE_TRANSFORMER_PAGE, 0xFFC46B), MachineScreen::new, controller -> {
                    controller.clientTick();
                    MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.MATTER_PHASE_TRANSFORMER);
                });
        register(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER,
                Style.ember(ModGuide.HIGH_REACTIVITY_LAVA_SMELTER_PAGE, 0xFFC46B), HighReactivityLavaSmelterScreen::new,
                controller -> MachineProcessingSounds.tick(controller, MachineProcessingSounds.Type.HIGH_REACTIVITY_LAVA_SMELTER));
        register(ModMachines.CRYSTAL_WORKSTATION,
                Style.crystal(ModGuide.CRYSTAL_WORKSTATION_PAGE, 0x8EE6E0).withModeIcons(Map.of(
                        CrystalWorkstationMode.ETCHING, new MachineModeButton.TextureIcon(ResourceLocation.fromNamespaceAndPath(
                                ae2pr.MODID, "textures/guis/workstation_etching.png")),
                        CrystalWorkstationMode.CUTTING, new MachineModeButton.TextureIcon(ResourceLocation.fromNamespaceAndPath(
                                ae2pr.MODID, "textures/guis/workstation_cutting.png")))), MachineScreen::new, controller -> {
                    CrystalWorkstationShell.update(controller);
                    MachineProcessingSounds.tick(controller, controller.getMode() == CrystalWorkstationMode.ETCHING
                            ? MachineProcessingSounds.Type.CRYSTAL_WORKSTATION_ETCHING
                            : MachineProcessingSounds.Type.CRYSTAL_WORKSTATION_CUTTING);
                });
        register(ModMachines.MULTI_FACTORY, Style.crystal(ModGuide.MULTI_FACTORY_PAGE, 0xCDA6F0).withModeIcons(Map.of(
                        MultiFactoryMode.INSCRIBING, new MachineModeButton.ItemIcon(new ItemStack(AEBlocks.INSCRIBER.block())),
                        MultiFactoryMode.CHARGING, new MachineModeButton.ItemIcon(new ItemStack(AEBlocks.CHARGER.block())))), MachineScreen::new,
                controller -> MachineProcessingSounds.tick(controller, controller.getMode() == MultiFactoryMode.INSCRIBING
                        ? MachineProcessingSounds.Type.MULTI_FACTORY_INSCRIBING : MachineProcessingSounds.Type.MULTI_FACTORY_CHARGING));
        MenuScreens.register(ModMachines.NAMING_FACTORY.menuType(), NamingFactoryScreen::new);
    }

    private static <E extends AbstractMultiblockControllerBlockEntity, M extends AbstractMultiblockControllerMenu<E>>
            void register(MultiblockMachineDefinition<E, M> definition, Style style, ScreenFactory<M> screenFactory,
                    Consumer<E> clientTicker) {
        STYLES.put(definition, style);
        definition.clientTicker(clientTicker);
        MenuScreens.register(definition.menuType(), screenFactory::create);
    }

    public static Style style(MachineDefinition<?, ?, ?> definition) {
        Style style = STYLES.get(definition);
        if (style == null) {
            throw new IllegalStateException("Missing machine screen style: " + definition.id());
        }
        return style;
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModMachines.NAMING_FACTORY.blockEntityType(), NamingFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.blockEntityType(), HighReactivityLavaSmelterRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.MATTER_PHASE_TRANSFORMER.blockEntityType(), MatterPhaseTransformerRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.CRYSTAL_REACTION_CHAMBER.blockEntityType(), CrystalReactionChamberRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.CRYSTAL_WORKSTATION.blockEntityType(), CrystalWorkstationRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.JIG.blockEntityType(), JigRenderer::new);
        event.registerBlockEntityRenderer(ModMachines.MULTI_FACTORY.blockEntityType(), MultiFactoryRenderer::new);
    }

    public record Style(ResourceLocation guidePage, int titleColor, int accent, int accentDark,
            Map<Mode, MachineModeButton.Icon> modeIcons) {
        public Style {
            modeIcons = Map.copyOf(modeIcons);
        }

        public static Style crystal(ResourceLocation guidePage, int titleColor) {
            return new Style(guidePage, titleColor, AbstractMachineScreen.ACCENT_CRYSTAL, AbstractMachineScreen.ACCENT_CRYSTAL_DARK,
                    Map.of());
        }

        public static Style ember(ResourceLocation guidePage, int titleColor) {
            return new Style(guidePage, titleColor, AbstractMachineScreen.ACCENT_EMBER, AbstractMachineScreen.ACCENT_EMBER_DARK,
                    Map.of());
        }

        public Style withModeIcons(Map<Mode, MachineModeButton.Icon> icons) {
            return new Style(guidePage, titleColor, accent, accentDark, icons);
        }
    }

    @FunctionalInterface
    private interface ScreenFactory<M extends AbstractMultiblockControllerMenu<?>> {
        AbstractContainerScreen<M> create(M menu, Inventory inventory, Component title);
    }
}
