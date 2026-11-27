package com.murthinext.ae2pr.data;

import com.murthinext.ae2pr.api.material.Form;
import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModMaterials;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 由材料形态生成 Forge 物品标签：每种形态一个子标签，并汇总到父标签。
 */
public final class MaterialTagProvider extends IntrinsicHolderTagsProvider<Item> {

    public MaterialTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            String modId,
            ExistingFileHelper existingFileHelper) {
        super(
                output,
                Registries.ITEM,
                lookupProvider,
                item -> item.builtInRegistryHolder().key(),
                modId,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Set<Form> aggregates = new LinkedHashSet<>();
        for (Material material : ModMaterials.ALL) {
            for (MaterialForm form : material.forms()) {
                if (form.origin() instanceof MaterialForm.Origin.Own own) {
                    tag(own.tag()).add(own.item().get());
                    aggregates.add(form.form());
                }
            }
        }

        for (Form form : aggregates) {
            for (Material material : ModMaterials.ALL) {
                material.form(form)
                        .flatMap(MaterialForm::ownTag)
                        .ifPresent(ownTag -> tag(form.aggregateTag()).addTag(ownTag));
            }
        }

        // 非材料物品，但属于同一父标签
        tag(Form.PLATE.aggregateTag()).add(ModItems.CARBON_FIBER_PLATE.get());
    }
}
