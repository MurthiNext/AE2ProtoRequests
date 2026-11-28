package com.murthinext.ae2pr.api.machine;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
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
import org.jetbrains.annotations.Nullable;

public abstract class MachineDefinition<E extends BlockEntity> {

    private final String id;
    private final Class<E> entityClass;
    private final RegistryObject<? extends Block> block;
    private final RegistryObject<BlockItem> item;
    private final RegistryObject<BlockEntityType<E>> blockEntityType;

    @Nullable
    private final MenuHandle<E, ?> menu;

    private final int speedUpgrades;
    private final List<Mode> modes;

    protected MachineDefinition(
            String id,
            String entityId,
            Class<E> entityClass,
            @Nullable Function<MachineDefinition<E>, MenuHandle<E, ?>> menuFactory,
            int speedUpgrades,
            List<Mode> modes) {
        this.id = id;
        this.entityClass = entityClass;
        this.speedUpgrades = speedUpgrades;
        this.modes = modes;
        this.block = registerBlock(id, this::createBlock);
        this.item = MachineRegistries.items().register(id, () -> new BlockItem(block(), new Item.Properties()));
        this.blockEntityType = registerBlockEntity(
                entityId,
                () -> BlockEntityType.Builder.of(this::createBlockEntity, block())
                        .build(null));
        this.menu = menuFactory == null ? null : menuFactory.apply(this);
    }

    // 注册表以 DeferredRegister<Block> 保存，具体方块类型由定义子类协变收窄
    private static RegistryObject<? extends Block> registerBlock(String id, Supplier<? extends Block> factory) {
        DeferredRegister<Block> registry = MachineRegistries.blocks();
        return registry.register(id, factory);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity> RegistryObject<BlockEntityType<E>> registerBlockEntity(
            String id, Supplier<BlockEntityType<E>> factory) {
        DeferredRegister<BlockEntityType<?>> registry = MachineRegistries.blockEntities();
        return (RegistryObject<BlockEntityType<E>>)
                (RegistryObject<?>) registry.register(id, (Supplier<BlockEntityType<?>>) (Supplier<?>) factory);
    }

    final <M extends AbstractContainerMenu> RegistryObject<MenuType<M>> registerMenu(Supplier<MenuType<M>> factory) {
        return MachineRegistries.menus().register(id, factory);
    }

    public static <E extends BlockEntity> MachineDefinition<E> custom(
            String id,
            String entityId,
            Class<E> entityClass,
            Supplier<? extends Block> blockFactory,
            BiFunction<BlockPos, BlockState, E> entityFactory,
            @Nullable Function<MachineDefinition<E>, MenuHandle<E, ?>> menuFactory,
            int speedUpgrades) {
        return new MachineDefinition<>(id, entityId, entityClass, menuFactory, speedUpgrades, List.of()) {
            @Override
            protected Block createBlock() {
                return blockFactory.get();
            }

            @Override
            protected E createBlockEntity(BlockPos pos, BlockState state) {
                return entityFactory.apply(pos, state);
            }
        };
    }

    protected abstract Block createBlock();

    protected abstract E createBlockEntity(BlockPos pos, BlockState state);

    public final String id() {
        return id;
    }

    public Block block() {
        return block.get();
    }

    public final RegistryObject<? extends Block> blockHolder() {
        return block;
    }

    public final BlockItem item() {
        return item.get();
    }

    public final RegistryObject<BlockItem> itemHolder() {
        return item;
    }

    public final BlockEntityType<E> blockEntityType() {
        return blockEntityType.get();
    }

    public final RegistryObject<BlockEntityType<E>> blockEntityTypeHolder() {
        return blockEntityType;
    }

    public final <M extends AbstractContainerMenu> MenuType<M> menuType() {
        return this.<M>menu().type().get();
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
                ? entityClass.cast(entity)
                : null;
    }

    public final AbstractContainerMenu createMenu(int id, Inventory inventory, @Nullable E controller) {
        if (menu == null) {
            throw new IllegalStateException("Machine has no menu: " + this.id);
        }
        if (controller != null && controller(controller) != controller) {
            throw new IllegalArgumentException("Controller does not belong to machine: " + this.id);
        }
        return menu.factory().create(this, id, inventory, controller);
    }

    @SuppressWarnings("unchecked") // 声明点已保证 M 与实际注册的菜单类型一致
    public final <M extends AbstractContainerMenu> MenuHandle<E, M> menu() {
        if (menu == null) {
            throw new IllegalStateException("Machine has no menu: " + id);
        }
        return (MenuHandle<E, M>) menu;
    }
}
