package com.murthinext.ae2pr.core.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * 一次声明即完成注册的方块及其物品。get() 与 RegistryObject.get() 同签名，调用点无需区分。
 */
public final class BlockDef<T extends Block> {

    private final String path;
    private final BlockGroup group;
    private final RegistryObject<T> block;
    @Nullable
    private final RegistryObject<BlockItem> item;

    BlockDef(String path, BlockGroup group, RegistryObject<T> block, @Nullable RegistryObject<BlockItem> item) {
        this.path = path;
        this.group = group;
        this.block = block;
        this.item = item;
    }

    public T get() {
        return block.get();
    }

    public RegistryObject<T> holder() {
        return block;
    }

    public BlockItem item() {
        if (item == null) {
            throw new IllegalStateException("block has no item: " + path);
        }
        return item.get();
    }

    @Nullable
    public RegistryObject<BlockItem> itemHolder() {
        return item;
    }

    public boolean hasItem() {
        return item != null;
    }

    public String path() {
        return path;
    }

    public BlockGroup group() {
        return group;
    }
}
