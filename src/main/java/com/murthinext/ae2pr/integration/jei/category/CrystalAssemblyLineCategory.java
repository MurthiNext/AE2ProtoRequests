package com.murthinext.ae2pr.integration.jei.category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.multiblock.module.ModuleDefinition;
import com.murthinext.ae2pr.api.multiblock.module.ModuleProvider;
import com.murthinext.ae2pr.api.multiblock.module.ModuleRequirement;
import com.murthinext.ae2pr.integration.jei.JeiRecipeInfo;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineRecipe;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;

public class CrystalAssemblyLineCategory extends AbstractMachineCategory<CrystalAssemblyLineRecipe> {

    public static final RecipeType<CrystalAssemblyLineRecipe> RECIPE_TYPE = RecipeType.create(ae2pr.MODID,
            "crystal_assembly_line", CrystalAssemblyLineRecipe.class);

    private static final int SLOT = SlotArea.SIZE;
    private static final int PADDING = 4;
    private static final int ITEM_COLS = 4;
    private static final int ITEM_ROWS = 4;
    private static final int FLUID_SLOT_COUNT = 4;
    private static final int ITEMS_X = PADDING;
    private static final int ITEMS_Y = PADDING;
    private static final int FLUID_X = PADDING + 89;
    private static final int OUTPUT_X = PADDING + 126;
    private static final int MODULE_Y = PADDING + SLOT + 2;
    private static final int MODULE_SLOT_COUNT = 5;
    private static final int PIPE_X = PADDING + 71;
    private static final int PIPE_Y = PADDING;
    private static final int PIPE_WIDTH = 54;
    private static final int PIPE_HEIGHT = 72;
    private static final int PIPE_TEXTURE_HEIGHT = 144;
    private static final long PIPE_CYCLE_MS = 2000L;
    private static final int INFO_Y = PADDING + ITEM_ROWS * SLOT + 8;
    private static final int WIDTH = OUTPUT_X + SLOT + PADDING;
    private static final int HEIGHT = INFO_Y + 3 * 10 + 2;
    private static final int COLOR_TEXT = 0x404040;
    private static final int COLOR_HINT = 0x808080;
    private static final ResourceLocation PIPE_TEXTURE = Ids.of(
            "textures/gui/jei/crystal_assembly_line_pipes.png");

    public CrystalAssemblyLineCategory(IGuiHelper guiHelper) {
        super(guiHelper, new ItemStack(ModMachines.CRYSTAL_ASSEMBLY_LINE.block()), WIDTH, HEIGHT);
    }

    @Override
    public RecipeType<CrystalAssemblyLineRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.ae2pr.crystal_assembly_line");
    }

    @Override
    protected SlotArea itemInputs() {
        return new SlotArea(ITEM_COLS * ITEM_ROWS, ITEM_COLS, ITEMS_X, ITEMS_Y);
    }

    @Override
    protected SlotArea fluidInputs() {
        return SlotArea.column(FLUID_SLOT_COUNT, FLUID_X, ITEMS_Y);
    }

    @Override
    protected SlotArea itemOutputs() {
        return new SlotArea(1, 1, OUTPUT_X, ITEMS_Y);
    }

    @Override
    protected int infoY() {
        return INFO_Y;
    }

    @Override
    protected void addExtraSlots(IRecipeLayoutBuilder builder, CrystalAssemblyLineRecipe recipe) {
        List<ModuleRequirement> requirements = recipe.getModuleRequirements();
        for (int index = 0; index < requirements.size() && index < MODULE_SLOT_COUNT; index++) {
            ModuleRequirement requirement = requirements.get(index);
            List<ItemStack> candidates = moduleCandidates(requirement);
            if (candidates.isEmpty()) {
                continue;
            }
            builder.addSlot(RecipeIngredientRole.INPUT, OUTPUT_X, MODULE_Y + index * SLOT)
                    .addItemStacks(candidates)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(requirementHint(requirement)));
        }
    }

    @Override
    protected void drawProcess(GuiGraphics graphics) {
        float progress = (Util.getMillis() % PIPE_CYCLE_MS) / (float) PIPE_CYCLE_MS;
        graphics.blit(PIPE_TEXTURE, PIPE_X, PIPE_Y, 0.0F, 0.0F, PIPE_WIDTH, PIPE_HEIGHT,
                PIPE_WIDTH, PIPE_TEXTURE_HEIGHT);
        int filled = Math.round(PIPE_WIDTH * progress);
        if (filled > 0) {
            graphics.blit(PIPE_TEXTURE, PIPE_X, PIPE_Y, 0.0F, PIPE_HEIGHT, filled, PIPE_HEIGHT,
                    PIPE_WIDTH, PIPE_TEXTURE_HEIGHT);
        }
    }

    // 装配线只展示一个产物，超出部分单独提示
    @Override
    protected void drawInfo(GuiGraphics graphics, CrystalAssemblyLineRecipe recipe) {
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, JeiRecipeInfo.duration(recipe.getDuration()), PADDING, INFO_Y, COLOR_TEXT, false);
        graphics.drawString(font, JeiRecipeInfo.energy(recipe.getEnergyPerParallel()), PADDING, INFO_Y + 10,
                COLOR_TEXT, false);
        int hidden = recipe.getItemInputs().size() - ITEM_COLS * ITEM_ROWS;
        if (hidden > 0) {
            graphics.drawString(font, JeiRecipeInfo.moreInputs(hidden), PADDING, INFO_Y + 20, COLOR_HINT, false);
        } else if (recipe.getItemOutputs().size() > 1) {
            graphics.drawString(font, JeiRecipeInfo.moreOutputs(recipe.getItemOutputs().size() - 1), PADDING,
                    INFO_Y + 20, COLOR_HINT, false);
        }
    }

    // 满足要求的模块物品，按等级升序排列
    private static List<ItemStack> moduleCandidates(ModuleRequirement requirement) {
        List<Block> blocks = new ArrayList<>();
        for (Block block : ForgeRegistries.BLOCKS.getValues()) {
            if (block instanceof ModuleProvider provider) {
                ModuleDefinition definition = provider.moduleDefinition();
                if (definition.type().equals(requirement.type()) && definition.level() >= requirement.minLevel()) {
                    blocks.add(block);
                }
            }
        }
        blocks.sort(Comparator.comparingInt(block -> ((ModuleProvider) block).moduleDefinition().level()));
        List<ItemStack> stacks = new ArrayList<>(blocks.size());
        for (Block block : blocks) {
            if (block.asItem() != Items.AIR) {
                stacks.add(new ItemStack(block));
            }
        }
        return stacks;
    }

    private static Component requirementHint(ModuleRequirement requirement) {
        return Component.translatable("jei.ae2pr.crystal_assembly_line.module_requirement.hint",
                requirement.minLevel(), Component.translatable(requirement.type().translationKey()));
    }
}
