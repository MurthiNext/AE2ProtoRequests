package com.murthinext.ae2pr.api.machine;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.api.machine.CoreLight;
import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.Mode;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;

public final class MultiblockMachineDefinition<E extends AbstractMultiblockControllerBlockEntity, M
                                                       extends AbstractMultiblockControllerMenu<E>>
        extends MachineDefinition<MachineBlock<E>, E, M> {

    private final ControllerFactory<E> controllerFactory;
    private final Supplier<Block.Properties> properties;
    private final Supplier<? extends AbstractStructureFacade> structure;
    private final IntSupplier parallel;
    private final Supplier<? extends RecipeType<?>> recipeType;
    private Consumer<E> clientTicker = controller -> {
    };
    // 核心方块运行指示灯谓词，未配置表示不处理
    @Nullable
    private CoreLight coreLight;

    private MultiblockMachineDefinition(String id, Class<E> entityClass, ControllerFactory<E> controllerFactory,
                                        Supplier<Block.Properties> properties,
                                        Supplier<? extends AbstractStructureFacade> structure, IntSupplier parallel,
                                        Supplier<? extends RecipeType<?>> recipeType, List<Mode> modes,
                                        Function<MachineDefinition<MachineBlock<E>, E, M>, MenuType<M>> menuTypeFactory,
                                        MenuFactory<E, M> menuFactory) {
        super(id, id, entityClass, menuTypeFactory, menuFactory, 4, modes);
        this.controllerFactory = controllerFactory;
        this.properties = properties;
        this.structure = structure;
        this.parallel = parallel;
        this.recipeType = recipeType;
    }

    public static MultiblockMachineDefinition<MachineControllerBlockEntity, MachineMenu<MachineControllerBlockEntity>>
    standard(String id, Supplier<Block.Properties> properties, Supplier<? extends AbstractStructureFacade> structure,
             IntSupplier parallel, Supplier<? extends RecipeType<?>> recipeType) {
        return build(id, MachineControllerBlockEntity.class, MachineControllerBlockEntity::new, properties, structure,
                     parallel, recipeType, List.of());
    }

    public static <E extends AbstractMultiblockControllerBlockEntity> MultiblockMachineDefinition<E, MachineMenu<E>>
    standard(String id, Class<E> entityClass, BiFunction<BlockPos, BlockState, E> controllerFactory,
             Supplier<Block.Properties> properties, Supplier<? extends AbstractStructureFacade> structure,
             IntSupplier parallel, Supplier<? extends RecipeType<?>> recipeType) {
        return build(id, entityClass,
                     (definition, pos, state)
                             -> controllerFactory.apply(pos, state),
                     properties, structure, parallel, recipeType, List.of());
    }

    public static <T extends Enum<T> & Mode, E extends AbstractMultiblockControllerBlockEntity>
            MultiblockMachineDefinition<E, MachineMenu<E>>
            withModes(String id, Class<E> entityClass, BiFunction<BlockPos, BlockState, E> controllerFactory,
                      Supplier<Block.Properties> properties, Supplier<? extends AbstractStructureFacade> structure,
                      IntSupplier parallel, Supplier<? extends RecipeType<?>> recipeType, List<T> modes) {
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("Machine must have at least one mode: " + id);
        }
        for (int index = 0; index < modes.size(); index++) {
            if (modes.get(index).ordinal() != index) {
                throw new IllegalArgumentException("Machine modes must follow enum order: " + id);
            }
        }
        return build(id, entityClass,
                     (definition, pos, state)
                             -> controllerFactory.apply(pos, state),
                     properties, structure, parallel, recipeType, List.copyOf(modes));
    }

    public static <E extends AbstractMultiblockControllerBlockEntity, M extends AbstractMultiblockControllerMenu<E>>
            MultiblockMachineDefinition<E, M>
            customMenu(String id, Class<E> entityClass, BiFunction<BlockPos, BlockState, E> controllerFactory,
                       Supplier<Block.Properties> properties, Supplier<? extends AbstractStructureFacade> structure,
                       IntSupplier parallel, Supplier<? extends RecipeType<?>> recipeType,
                       Supplier<MenuType<M>> menuTypeFactory, MenuFactory<E, M> menuFactory) {
        return new MultiblockMachineDefinition<>(id, entityClass,
                                                 (definition, pos, state)
                                                         -> controllerFactory.apply(pos, state),
                                                 properties, structure, parallel, recipeType, List.of(),
                                                 definition -> menuTypeFactory.get(), menuFactory);
    }

    private static <E extends AbstractMultiblockControllerBlockEntity> MultiblockMachineDefinition<E, MachineMenu<E>>
    build(String id, Class<E> entityClass, ControllerFactory<E> controllerFactory,
          Supplier<Block.Properties> properties, Supplier<? extends AbstractStructureFacade> structure,
          IntSupplier parallel, Supplier<? extends RecipeType<?>> recipeType, List<Mode> modes) {
        return new MultiblockMachineDefinition<>(
                id, entityClass, controllerFactory, properties, structure, parallel, recipeType, modes,
                definition
                -> IForgeMenuType.create((menuId, inventory,
                                          buffer) -> new MachineMenu<>(definition, menuId, inventory, buffer)),
                MachineMenu::new);
    }

    @Override
    protected MachineBlock<E> createBlock() {
        return new MachineBlock<>(this, properties.get());
    }

    @Override
    protected E createBlockEntity(BlockPos pos, BlockState state) {
        return controllerFactory.create(this, pos, state);
    }

    public AbstractStructureFacade structure() {
        return structure.get();
    }

    public int maxParallel() {
        return parallel.getAsInt();
    }

    public RecipeType<?> recipeType() {
        return recipeType.get();
    }

    public void clientTicker(Consumer<E> ticker) {
        clientTicker = ticker;
    }

    public void clientTick(E controller) {
        clientTicker.accept(controller);
    }

    // 声明核心方块使用 HighEnergyCoreBlock 的运行指示灯
    public MultiblockMachineDefinition<E, M> coreLight(CoreLight predicate) {
        this.coreLight = predicate;
        return this;
    }

    @Nullable
    public CoreLight coreLight() {
        return coreLight;
    }

    @FunctionalInterface
    private interface ControllerFactory<E extends AbstractMultiblockControllerBlockEntity> {
        E create(MultiblockMachineDefinition<E, ?> definition, BlockPos pos, BlockState state);
    }
}
