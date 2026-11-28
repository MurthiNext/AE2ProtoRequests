package com.murthinext.ae2pr.api.machine;

import java.util.function.Supplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

public record MenuHandle<E extends BlockEntity, M extends AbstractContainerMenu>(
        RegistryObject<MenuType<M>> type, MenuFactory<E, M> factory) {

    public static <E extends BlockEntity, M extends AbstractContainerMenu> MenuHandle<E, M> register(
            MachineDefinition<E> definition, Supplier<MenuType<M>> typeFactory, MenuFactory<E, M> factory) {
        return new MenuHandle<>(definition.registerMenu(typeFactory), factory);
    }

    @FunctionalInterface
    public interface MenuFactory<E extends BlockEntity, M extends AbstractContainerMenu> {
        M create(MachineDefinition<E> definition, int id, Inventory inventory, @Nullable E controller);
    }
}