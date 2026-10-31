package com.murthinext.ae2pr.machine;

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
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.registry.ModBlockEntities;
import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModMenus;

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

    protected MachineDefinition(String id, String entityId, Class<E> entityClass,
            @Nullable Function<MachineDefinition<B, E, M>, MenuType<M>> menuTypeFactory,
            @Nullable MenuFactory<E, M> menuFactory, int speedUpgrades) {
        if ((menuTypeFactory == null) != (menuFactory == null)) {
            throw new IllegalArgumentException("Menu type and factory must be configured together: " + id);
        }
        this.id = id;
        this.entityClass = entityClass;
        this.menuFactory = menuFactory;
        this.speedUpgrades = speedUpgrades;
        this.block = ModBlocks.BLOCKS.register(id, this::createBlock);
        this.item = ModItems.ITEMS.register(id, () -> new BlockItem(block(), new Item.Properties()));
        this.blockEntityType = ModBlockEntities.BLOCK_ENTITIES.register(entityId,
                () -> BlockEntityType.Builder.of(this::createBlockEntity, block()).build(null));
        this.menuType = menuTypeFactory == null ? null
                : ModMenus.MENUS.register(id, () -> menuTypeFactory.apply(this));
    }

    public static <B extends Block, E extends BlockEntity, M extends AbstractContainerMenu>
            MachineDefinition<B, E, M> custom(String id, String entityId, Class<E> entityClass,
                    Supplier<B> blockFactory, BiFunction<BlockPos, BlockState, E> entityFactory,
                    @Nullable Supplier<MenuType<M>> menuTypeFactory, @Nullable MenuFactory<E, M> menuFactory,
                    int speedUpgrades) {
        return new MachineDefinition<>(id, entityId, entityClass,
                menuTypeFactory == null ? null : definition -> menuTypeFactory.get(), menuFactory, speedUpgrades) {
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
