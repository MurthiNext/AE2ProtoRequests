package com.murthinext.ae2pr.api.machine;

import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerBlockEntity;
import com.murthinext.ae2pr.api.machine.multiblock.AbstractMultiblockControllerMenu;
import com.murthinext.ae2pr.api.multiblock.AbstractStructureFacade;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class MachineSpec<E extends AbstractMultiblockControllerBlockEntity> {

    private final String id;
    private final Class<E> entityClass;
    private final MultiblockMachineDefinition.ControllerFactory<E> controller;
    private final Supplier<Block.Properties> properties;
    private final Supplier<? extends AbstractStructureFacade> structure;
    private final IntSupplier parallel;
    private final IntSupplier parallelPerRepeatSlice;

    @Nullable
    private final Class<? extends Enum<?>> modeType;

    @Nullable
    private final RecipeSpec<?> recipe;

    @Nullable
    private final Function<MachineDefinition<E>, MenuHandle<E, ?>> menu;

    @Nullable
    private final CoreLight coreLight;

    private MachineSpec(Builder<E> builder) {
        id = builder.id;
        entityClass = builder.entityClass;
        controller = builder.controller;
        properties = builder.properties;
        structure = builder.structure;
        parallel = builder.parallel;
        parallelPerRepeatSlice = builder.parallelPerRepeatSlice;
        modeType = builder.modeType;
        recipe = builder.recipe;
        menu = builder.menu;
        coreLight = builder.coreLight;
    }

    public static <E extends AbstractMultiblockControllerBlockEntity> Builder<E> builder(
            String id, Class<E> entityClass) {
        return new Builder<>(id, entityClass);
    }

    String id() {
        return id;
    }

    Class<E> entityClass() {
        return entityClass;
    }

    MultiblockMachineDefinition.ControllerFactory<E> controller() {
        return controller;
    }

    Supplier<Block.Properties> properties() {
        return properties;
    }

    Supplier<? extends AbstractStructureFacade> structure() {
        return structure;
    }

    IntSupplier parallel() {
        return parallel;
    }

    IntSupplier parallelPerRepeatSlice() {
        return parallelPerRepeatSlice;
    }

    @Nullable
    Class<? extends Enum<?>> modeType() {
        return modeType;
    }

    @Nullable
    RecipeSpec<?> recipe() {
        return recipe;
    }

    @Nullable
    Function<MachineDefinition<E>, MenuHandle<E, ?>> menu() {
        return menu;
    }

    @Nullable
    CoreLight coreLight() {
        return coreLight;
    }

    public static final class Builder<E extends AbstractMultiblockControllerBlockEntity> {

        private final String id;
        private final Class<E> entityClass;

        @Nullable
        private MultiblockMachineDefinition.ControllerFactory<E> controller;

        @Nullable
        private Supplier<Block.Properties> properties;

        @Nullable
        private Supplier<? extends AbstractStructureFacade> structure;

        @Nullable
        private IntSupplier parallel;

        private IntSupplier parallelPerRepeatSlice = () -> 0;

        @Nullable
        private Class<? extends Enum<?>> modeType;

        @Nullable
        private RecipeSpec<?> recipe;

        @Nullable
        private Function<MachineDefinition<E>, MenuHandle<E, ?>> menu;

        @Nullable
        private CoreLight coreLight;

        private Builder(String id, Class<E> entityClass) {
            this.id = id;
            this.entityClass = entityClass;
        }

        public Builder<E> controller(MultiblockMachineDefinition.ControllerFactory<E> controller) {
            this.controller = controller;
            return this;
        }

        public Builder<E> properties(Supplier<Block.Properties> properties) {
            this.properties = properties;
            return this;
        }

        public Builder<E> structure(Supplier<? extends AbstractStructureFacade> structure) {
            this.structure = structure;
            return this;
        }

        public Builder<E> parallel(IntSupplier parallel) {
            this.parallel = parallel;
            return this;
        }

        public Builder<E> parallelPerRepeatSlice(IntSupplier parallelPerRepeatSlice) {
            this.parallelPerRepeatSlice = parallelPerRepeatSlice;
            return this;
        }

        public <T extends Enum<T> & Mode> Builder<E> modes(Class<T> modeType) {
            this.modeType = modeType;
            return this;
        }

        public Builder<E> recipe(RecipeSpec<?> recipe) {
            this.recipe = recipe;
            return this;
        }

        public <M extends AbstractMultiblockControllerMenu<E>> Builder<E> menu(
                Supplier<MenuType<M>> menuType, MenuHandle.MenuFactory<E, M> menuFactory) {
            this.menu = definition -> MenuHandle.register(definition, menuType, menuFactory);
            return this;
        }

        public Builder<E> coreLight(CoreLight coreLight) {
            this.coreLight = coreLight;
            return this;
        }

        public MachineSpec<E> build() {
            if (controller == null) {
                throw new IllegalStateException("Missing controller: " + id);
            }
            if (properties == null) {
                throw new IllegalStateException("Missing properties: " + id);
            }
            if (structure == null) {
                throw new IllegalStateException("Missing structure: " + id);
            }
            if (parallel == null) {
                throw new IllegalStateException("Missing parallel: " + id);
            }
            return new MachineSpec<>(this);
        }
    }
}
