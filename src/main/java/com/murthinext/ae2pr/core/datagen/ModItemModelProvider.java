package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.api.material.MaterialItem;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.core.registry.ModItems;
import com.murthinext.ae2pr.core.registry.ModMaterials;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * 物品模型：方块物品指向现有方块模型，普通物品指向 item/&lt;id&gt; 贴图。
 * 自定义模型、桶与材料形态由手写文件或 MaterialModelProvider 负责。
 */
public final class ModItemModelProvider extends ItemModelProvider {

    // 方块模型放在 block/<id>/<id>.json 的条目
    private static final Set<String> NESTED = Set.of(
            "crystal_assembly_line",
            "crystal_reaction_chamber",
            "crystal_workstation",
            "filter_press",
            "filter_press_plate",
            "high_reactivity_lava_smelter",
            "jig",
            "matter_phase_transformer",
            "me_network_access_hatch",
            "multi_factory",
            "naming_factory");

    private static final Map<String, String> PARENT_OVERRIDES =
            Map.of("meteor_steel_pipe_block", "meteor_steel_pipe_block/meteor_steel_pipe_block_axis_y");

    private static final Set<String> CROSS_ITEMS = Set.of(
            "small_sculk_crystal_bud",
            "medium_sculk_crystal_bud",
            "large_sculk_crystal_bud",
            "sculk_crystal_cluster");

    private static final Set<String> BESPOKE = Set.of(
            "entangled_casing",
            "alien_lava_bucket",
            "coarse_meteorite_slurry_bucket",
            "coarse_meteorite_solution_bucket",
            "matter_confinement_frame",
            "multi_level_emitter",
            "multi_threshold_level_emitter",
            "sky_dome_annihilation_plane");

    public ModItemModelProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        super(output, modId, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            String path = entry.getId().getPath();
            if (entry.get().asItem() == Items.AIR || BESPOKE.contains(path)) {
                continue;
            }
            if (CROSS_ITEMS.contains(path)) {
                withExistingParent(path, mcLoc("item/generated")).texture("layer0", modLoc("block/" + path));
                continue;
            }
            withExistingParent(path, modLoc("block/" + blockParent(path)));
        }

        Set<String> materialModelPaths = materialModelPaths();
        for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof BlockItem || materialModelPaths.contains(entry.getId().getPath())
                    || BESPOKE.contains(entry.getId().getPath())) {
                continue;
            }
            basicItem(item);
        }
    }

    private static String blockParent(String path) {
        String override = PARENT_OVERRIDES.get(path);
        if (override != null) {
            return override;
        }
        if (path.startsWith("crystal_assembly_line_")) {
            return "crystal_assembly_line/" + path;
        }
        return NESTED.contains(path) ? path + "/" + path : path;
    }

    private static Set<String> materialModelPaths() {
        Set<String> paths = new HashSet<>();
        for (Material material : ModMaterials.ALL) {
            for (MaterialForm form : material.forms()) {
                if (form.origin() instanceof MaterialForm.Origin.Own own
                        && own.item().get() instanceof MaterialItem) {
                    paths.add(form.form().itemId(material.id()));
                }
            }
            material.fluid().ifPresent(fluid -> paths.add(fluid.id() + "_bucket"));
        }
        return paths;
    }

    @Override
    public String getName() {
        return "AE2PR Item Models";
    }
}
