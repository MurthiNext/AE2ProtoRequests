package com.murthinext.ae2pr.api.machine;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;

// 机器模板使用的延迟注册表，由模组侧在构造时注入
public final class MachineRegistries {

    private static DeferredRegister<Block> blocks;
    private static DeferredRegister<Item> items;
    private static DeferredRegister<BlockEntityType<?>> blockEntities;
    private static DeferredRegister<MenuType<?>> menus;
    private static DeferredRegister<RecipeType<?>> recipeTypes;
    private static DeferredRegister<RecipeSerializer<?>> recipeSerializers;

    private MachineRegistries() {
    }

    public static void install(DeferredRegister<Block> blocks, DeferredRegister<Item> items,
            DeferredRegister<BlockEntityType<?>> blockEntities, DeferredRegister<MenuType<?>> menus,
            DeferredRegister<RecipeType<?>> recipeTypes, DeferredRegister<RecipeSerializer<?>> recipeSerializers) {
        MachineRegistries.blocks = blocks;
        MachineRegistries.items = items;
        MachineRegistries.blockEntities = blockEntities;
        MachineRegistries.menus = menus;
        MachineRegistries.recipeTypes = recipeTypes;
        MachineRegistries.recipeSerializers = recipeSerializers;
    }

    public static DeferredRegister<Block> blocks() {
        return require(blocks, "blocks");
    }

    public static DeferredRegister<Item> items() {
        return require(items, "items");
    }

    public static DeferredRegister<BlockEntityType<?>> blockEntities() {
        return require(blockEntities, "blockEntities");
    }

    public static DeferredRegister<MenuType<?>> menus() {
        return require(menus, "menus");
    }

    public static DeferredRegister<RecipeType<?>> recipeTypes() {
        return require(recipeTypes, "recipeTypes");
    }

    public static DeferredRegister<RecipeSerializer<?>> recipeSerializers() {
        return require(recipeSerializers, "recipeSerializers");
    }

    private static <T> T require(T registry, String name) {
        if (registry == null) {
            throw new IllegalStateException("Machine registries not installed: " + name);
        }
        return registry;
    }
}
