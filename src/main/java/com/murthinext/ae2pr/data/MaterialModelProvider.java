package com.murthinext.ae2pr.data;

import com.murthinext.ae2pr.api.material.Material;
import com.murthinext.ae2pr.api.material.MaterialForm;
import com.murthinext.ae2pr.registry.ModMaterials;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 由材料形态生成物品模型；有流体的材料同时生成无方块桶模型。
 */
public final class MaterialModelProvider extends ItemModelProvider {

    public MaterialModelProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        super(output, modId, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (Material material : ModMaterials.ALL) {
            for (MaterialForm form : material.forms()) {
                if (form.origin() instanceof MaterialForm.Origin.Own) {
                    withExistingParent(
                            form.form().itemId(material.id()),
                            modLoc("item/material_sets/" + material.modelSet() + "/"
                                    + form.form().name()));
                }
            }
            material.fluid()
                    .ifPresent(fluid -> getBuilder(fluid.id() + "_bucket")
                            .parent(new ModelFile.UncheckedModelFile("forge:item/bucket"))
                            .customLoader(DynamicFluidContainerModelBuilder::begin)
                            .fluid(fluid.source().get()));
        }
    }
}
