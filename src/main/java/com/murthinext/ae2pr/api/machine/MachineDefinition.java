package com.murthinext.ae2pr.api.machine;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;


import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;


public abstract class MachineDefinition<B extends Block, E extends BlockEntity, M extends AbstractContainerMenu> {

    private final String id;
    private final Class<E> entityClass;
    private final RegistryObject<B> block;
    private final RegistryObject<BlockItem> item;
    private final RegistryObject<BlockEntityType<E>> blockEntityType;
    @Nullable
    private final RegistryObject<MenuType<M>> menuType;
    @Nullable
    private final MenuFactory<E, M> menuFactory;
    private final int speedUpgrades;
    private final List<Mode> modes;

    protected MachineDefinition(String id, String entityId, Class<E> entityClass,
            @Nullable Function<MachineDefinition<B, E, M>, MenuType<M>> menuTypeFactory,
            @Nullable MenuFactory<E, M> menuFactory, int speedUpgrades, List<Mode> modes) {
        if ((menuTypeFactory == null) != (menuFactory == null)) {
            throw new IllegalArgumentException("Menu type and factory must be configured together: " + id);
        }
        this.id = id;
        this.entityClass = entityClass;
        this.menuFactory = menuFactory;
        this.speedUpgrades = speedUpgrades;
        this.modes = modes;
        this.block = registerBlock(id, this::createBlock);
        this.item = MachineRegistries.items().register(id, () -> new BlockItem(block(), new Item.Properties()));
        this.blockEntityType = registerBlockEntity(entityId,
                () -> BlockEntityType.Builder.of(this::createBlockEntity, block()).build(null));
        this.menuType = menuTypeFactory == null ? null
                : registerMenu(id, () -> menuTypeFactory.apply(this));
    }

    // 注册表以 DeferredRegister<Block> 保存，按具体方块类型转发
    @SuppressWarnings("unchecked")
    private static <B extends Block> RegistryObject<B> registerBlock(String id, Supplier<? extends B> factory) {
        DeferredRegister<Block> registry = MachineRegistries.blocks();
        return (RegistryObject<B>) registry.register(id, (Supplier<Block>) factory);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity> RegistryObject<BlockEntityType<E>> registerBlockEntity(String id,
            Supplier<BlockEntityType<E>> factory) {
        DeferredRegister<BlockEntityType<?>> registry = MachineRegistries.blockEntities();
        return (RegistryObject<BlockEntityType<E>>) (RegistryObject<?>) registry.register(id,
                (Supplier<BlockEntityType<?>>) (Supplier<?>) factory);
    }

    @SuppressWarnings("unchecked")
    private static <M extends AbstractContainerMenu> RegistryObject<MenuType<M>> registerMenu(String id,
            Supplier<MenuType<M>> factory) {
        DeferredRegister<MenuType<?>> registry = MachineRegistries.menus();
        return (RegistryObject<MenuType<M>>) (RegistryObject<?>) registry.register(id,
                (Supplier<MenuType<?>>) (Supplier<?>) factory);
    }

    public static <B extends Block, E extends BlockEntity, M extends AbstractContainerMenu>
            MachineDefinition<B, E, M> custom(String id, String entityId, Class<E> entityClass,
                    Supplier<B> blockFactory, BiFunction<BlockPos, BlockState, E> entityFactory,
                    @Nullable Supplier<MenuType<M>> menuTypeFactory, @Nullable MenuFactory<E, M> menuFactory,
                    int speedUpgrades) {
        return new MachineDefinition<>(id, entityId, entityClass,
                menuTypeFactory == null ? null : definition -> menuTypeFactory.get(), menuFactory, speedUpgrades,
                List.of()) {
            @Override
            protected B createBlock() {
                return blockFactory.get();
            }

            @Override
            protected E createBlockEntity(BlockPos pos, BlockState state) {
                return entityFactory.apply(pos, state);
            }
        };
    }

    protected abstract B createBlock();

    protected abstract E createBlockEntity(BlockPos pos, BlockState state);

    public final String id() {
        return id;
    }

    public final B block() {
        return block.get();
    }

    public final BlockItem item() {
        return item.get();
    }

    public final BlockEntityType<E> blockEntityType() {
        return blockEntityType.get();
    }

    public final MenuType<M> menuType() {
        if (menuType == null) {
            throw new IllegalStateException("Machine has no menu: " + id);
        }
        return menuType.get();
    }

    public final int speedUpgrades() {
        return speedUpgrades;
    }

    public final List<Mode> modes() {
        return modes;
    }

    @Nullable
    public final E controller(@Nullable BlockEntity entity) {
        return entity != null && entity.getType() == blockEntityType() && entityClass.isInstance(entity)
                ? entityClass.cast(entity) : null;
    }

    public final M createMenu(int id, Inventory inventory, @Nullable E controller) {
        if (menuFactory == null) {
            throw new IllegalStateException("Machine has no menu: " + this.id);
        }
        if (controller != null && controller(controller) != controller) {
            throw new IllegalArgumentException("Controller does not belong to machine: " + this.id);
        }
        return menuFactory.create(this, id, inventory, controller);
    }

    @FunctionalInterface
    public interface MenuFactory<E extends BlockEntity, M extends AbstractContainerMenu> {
        M create(MachineDefinition<?, E, M> definition, int id, Inventory inventory, @Nullable E controller);
    }
}
