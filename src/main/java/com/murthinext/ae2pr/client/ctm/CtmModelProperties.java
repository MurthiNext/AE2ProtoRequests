package com.murthinext.ae2pr.client.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

/**
 * 连接纹理使用的模型数据属性。
 */
public final class CtmModelProperties {

    public static final ModelProperty<BlockAndTintGetter> LEVEL = new ModelProperty<>();
    public static final ModelProperty<BlockPos> POS = new ModelProperty<>();
    public static final ModelProperty<ModelData> PARENT = new ModelProperty<>();

    private CtmModelProperties() {
    }
}
