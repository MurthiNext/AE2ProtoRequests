package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.extensions.IForgeMenuType;
import org.jetbrains.annotations.Nullable;

public final class MultiblockMachineDefinition<E extends AbstractMultiblockControllerBlockEntity>
        extends MachineDefinition<E> {

    private final ControllerFactory<E> controllerFactory;
    private final Supplier<Block.Properties> properties;
    private final Supplier<? extends AbstractStructureFacade> structure;
    private final IntSupplier parallel;
    private final IntSupplier parallelPerRepeatSlice;

    @Nullable
    private final Class<? extends Enum<?>> modeType;

    @Nullable
    private final RecipeDefinition<?> recipe;

    private final Map<Mode, RecipeDefinition<?>> modeRecipes;

    @Nullable
    private final CoreLight coreLight;

    private Consumer<E> clientTicker = controller -> {};

    private MultiblockMachineDefinition(
            String id,
            Class<E> entityClass,
            ControllerFactory<E> controllerFactory,
            Supplier<Block.Properties> properties,
            Supplier<? extends AbstractStructureFacade> structure,
            IntSupplier parallel,
            IntSupplier parallelPerRepeatSlice,
            @Nullable Class<? extends Enum<?>> modeType,
            @Nullable RecipeSpec<?> recipeSpec,
            List<Mode> modes,
            Function<MachineDefinition<E>, MenuHandle<E, ?>> menuFactory,
            @Nullable CoreLight coreLight) {
        super(id, id, entityClass, menuFactory, 4, modes);
        this.controllerFactory = controllerFactory;
        this.properties = properties;
        this.structure = structure;
        this.parallel = parallel;
        this.parallelPerRepeatSlice = parallelPerRepeatSlice;
        this.modeType = modeType;
        this.coreLight = coreLight;
        this.recipe = recipeSpec == null ? null : recipeSpec.declare(id);
        Map<Mode, RecipeDefinition<?>> modeRecipes = new HashMap<>();
        for (Mode mode : modes) {
            RecipeSpec<?> modeRecipe = mode.recipe();
            if (modeRecipe == null) {
                continue;
            }
            RecipeDefinition<?> definition = modeRecipe.declare(id + "_" + mode.id());
            modeRecipes.put(mode, definition);
            MachineRecipeTypes.bind(mode, definition);
        }
        this.modeRecipes = Map.copyOf(modeRecipes);
    }

    public static <E extends AbstractMultiblockControllerBlockEntity> MultiblockMachineDefinition<E> of(
            MachineSpec<E> spec) {
        Mode[] modes =
                spec.modeType() == null ? new Mode[0] : (Mode[]) spec.modeType().getEnumConstants();
        if (spec.modeType() != null && modes.length == 0) {
            throw new IllegalArgumentException("Machine must have at least one mode: " + spec.id());
        }
        Function<MachineDefinition<E>, MenuHandle<E, ?>> menu =
                spec.menu() == null ? MultiblockMachineDefinition::defaultMenu : spec.menu();
        return new MultiblockMachineDefinition<>(
                spec.id(),
                spec.entityClass(),
                spec.controller(),
                spec.properties(),
                spec.structure(),
                spec.parallel(),
                spec.parallelPerRepeatSlice(),
                spec.modeType(),
                spec.recipe(),
                List.of(modes),
                menu,
                spec.coreLight());
    }

    private static <E extends AbstractMultiblockControllerBlockEntity> MenuHandle<E, MachineMenu<E>> defaultMenu(
            MachineDefinition<E> definition) {
        return MenuHandle.register(
                definition,
                () -> IForgeMenuType.create(
                        (menuId, inventory, buffer) -> new MachineMenu<>(definition, menuId, inventory, buffer)),
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

    @SuppressWarnings("unchecked") // modeType 与 modes 同源，由 MachineSpec 在构造期保证
    public <T extends Enum<T> & Mode> Class<T> modeType() {
        if (modeType == null) {
            throw new IllegalStateException("Machine has no modes: " + id());
        }
        return (Class<T>) modeType;
    }

    public int parallelPerRepeatSlice() {
        return parallelPerRepeatSlice.getAsInt();
    }

    @Nullable
    public RecipeDefinition<?> recipe() {
        return recipe;
    }

    public RecipeDefinition<?> recipe(Mode mode) {
        RecipeDefinition<?> modeRecipe = modeRecipes.get(mode);
        if (modeRecipe == null) {
            throw new IllegalStateException("Mode has no recipe: " + id() + "/" + mode.id());
        }
        return modeRecipe;
    }

    public RecipeType<?> recipeType() {
        if (recipe != null) {
            return recipe.type();
        }
        if (!modes().isEmpty()) {
            return modes().get(0).recipeType();
        }
        throw new IllegalStateException("Machine has no recipe: " + id());
    }

    public void clientTicker(Consumer<E> ticker) {
        clientTicker = ticker;
    }

    public void clientTick(E controller) {
        clientTicker.accept(controller);
    }

    @Nullable
    public CoreLight coreLight() {
        return coreLight;
    }

    @FunctionalInterface
    public interface ControllerFactory<E extends AbstractMultiblockControllerBlockEntity> {
        E create(MultiblockMachineDefinition<E> definition, BlockPos pos, BlockState state);
    }
}
