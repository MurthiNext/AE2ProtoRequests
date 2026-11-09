package com.murthinext.ae2pr.integration.jei.plugin;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;

import appeng.recipes.handlers.ChargerRecipe;
import appeng.recipes.handlers.InscriberRecipe;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.integration.jei.category.ComplexWorldInteractionCategory;
import com.murthinext.ae2pr.integration.jei.category.CrystalAssemblyLineCategory;
import com.murthinext.ae2pr.integration.jei.category.CrystalReactionChamberCategory;
import com.murthinext.ae2pr.integration.jei.category.CrystalWorkstationCategory;
import com.murthinext.ae2pr.integration.jei.category.FilterPressCategory;
import com.murthinext.ae2pr.integration.jei.category.HighReactivityLavaSmelterCategory;
import com.murthinext.ae2pr.integration.jei.category.JigCategory;
import com.murthinext.ae2pr.integration.jei.category.MatterPhaseTransformerCategory;
import com.murthinext.ae2pr.integration.jei.category.MultiblockPreviewCategory;
import com.murthinext.ae2pr.integration.jei.gui.MachineGuiHandler;
import com.murthinext.ae2pr.integration.jei.recipe.MultiblockPreviewRecipe;
import com.murthinext.ae2pr.integration.jei.transfer.CrystalAssemblyLinePatternTransferHandler;
import com.murthinext.ae2pr.integration.jei.transfer.MultiblockPatternTransferHandler;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationMode;
import com.murthinext.ae2pr.machine.multi_factory.MultiFactoryMode;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.registry.ModRecipes;

// 机器分类共用同一条注册流程，差异集中在 machines 数据表
@JeiPlugin
public class Ae2prJeiPlugin implements IModPlugin {

    private static final mezz.jei.api.recipe.RecipeType<InscriberRecipe> AE2_INSCRIBER =
            mezz.jei.api.recipe.RecipeType.create("ae2", "inscriber", InscriberRecipe.class);
    private static final mezz.jei.api.recipe.RecipeType<ChargerRecipe> AE2_CHARGER =
            mezz.jei.api.recipe.RecipeType.create("ae2", "charger", ChargerRecipe.class);

    private record MachineCategory(mezz.jei.api.recipe.RecipeType<?> jeiType, RecipeType<?> vanillaType,
            Function<IGuiHelper, IRecipeCategory<?>> factory, ItemStack catalyst) {
    }

    private static List<MachineCategory> machines() {
        return List.of(
                new MachineCategory(JigCategory.RECIPE_TYPE, ModMachines.JIG_RECIPE.type(), JigCategory::new,
                        ModMachines.JIG.item().getDefaultInstance()),
                new MachineCategory(FilterPressCategory.RECIPE_TYPE, ModMachines.FILTER_PRESS_RECIPE.type(),
                        FilterPressCategory::new, ModMachines.FILTER_PRESS.item().getDefaultInstance()),
                new MachineCategory(CrystalReactionChamberCategory.RECIPE_TYPE,
                        ModMachines.CRYSTAL_REACTION_CHAMBER_RECIPE.type(), CrystalReactionChamberCategory::new,
                        ModMachines.CRYSTAL_REACTION_CHAMBER.item().getDefaultInstance()),
                new MachineCategory(HighReactivityLavaSmelterCategory.RECIPE_TYPE,
                        ModMachines.HIGH_REACTIVITY_LAVA_SMELTER_RECIPE.type(),
                        HighReactivityLavaSmelterCategory::new,
                        ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.item().getDefaultInstance()),
                new MachineCategory(MatterPhaseTransformerCategory.RECIPE_TYPE,
                        ModMachines.MATTER_PHASE_TRANSFORMER_RECIPE.type(), MatterPhaseTransformerCategory::new,
                        ModMachines.MATTER_PHASE_TRANSFORMER.item().getDefaultInstance()),
                new MachineCategory(CrystalAssemblyLineCategory.RECIPE_TYPE,
                        ModMachines.CRYSTAL_ASSEMBLY_LINE_RECIPE.type(), CrystalAssemblyLineCategory::new,
                        ModMachines.CRYSTAL_ASSEMBLY_LINE.block().asItem().getDefaultInstance()));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("machines");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (MachineCategory machine : machines()) {
            registration.addRecipeCategories(machine.factory().apply(guiHelper));
        }
        for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
            registration.addRecipeCategories(new CrystalWorkstationCategory(guiHelper, mode));
        }
        registration.addRecipeCategories(new MultiblockPreviewCategory(guiHelper));
        registration.addRecipeCategories(new ComplexWorldInteractionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (MachineCategory machine : machines()) {
            addRecipes(registration, machine.jeiType(), machine.vanillaType());
        }
        for (CrystalWorkstationMode mode : CrystalWorkstationMode.values()) {
            addRecipes(registration, CrystalWorkstationCategory.recipeType(mode), mode.recipeType());
        }
        registration.addRecipes(MultiblockPreviewCategory.RECIPE_TYPE, MultiblockPreviewRecipe.recipes());
        addRecipes(registration, ComplexWorldInteractionCategory.RECIPE_TYPE,
                ModRecipes.COMPLEX_WORLD_INTERACTION_TYPE.get());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineCategory machine : machines()) {
            addCatalyst(registration, machine.jeiType(), machine.catalyst());
        }
        ItemStack workstation = ModMachines.CRYSTAL_WORKSTATION.item().getDefaultInstance();
        addCatalyst(registration, CrystalWorkstationCategory.ETCHING, workstation);
        addCatalyst(registration, CrystalWorkstationCategory.CUTTING, workstation);
        ItemStack multiFactory = ModMachines.MULTI_FACTORY.item().getDefaultInstance();
        addCatalyst(registration, AE2_INSCRIBER, multiFactory);
        addCatalyst(registration, AE2_CHARGER, multiFactory);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CrystalAssemblyLinePatternTransferHandler(),
                CrystalAssemblyLineCategory.RECIPE_TYPE);
        registration.addRecipeTransferHandler(new MultiblockPatternTransferHandler(),
                MultiblockPreviewCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractMachineScreen.class, new MachineGuiHandler());
    }

    public static mezz.jei.api.recipe.RecipeType<?> multiFactoryRecipeType(MultiFactoryMode mode) {
        return mode == MultiFactoryMode.CHARGING ? AE2_CHARGER : AE2_INSCRIBER;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addRecipes(IRecipeRegistration registration, mezz.jei.api.recipe.RecipeType<?> jeiType,
            RecipeType<?> vanillaType) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Collection<?> recipes = level.getRecipeManager().getAllRecipesFor((RecipeType) vanillaType);
        registration.addRecipes((mezz.jei.api.recipe.RecipeType) jeiType, List.copyOf(recipes));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void addCatalyst(IRecipeCatalystRegistration registration,
            mezz.jei.api.recipe.RecipeType<?> jeiType, ItemStack catalyst) {
        registration.addRecipeCatalysts((mezz.jei.api.recipe.RecipeType) jeiType, catalyst);
    }
}
