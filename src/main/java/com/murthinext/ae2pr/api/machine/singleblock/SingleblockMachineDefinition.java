package com.murthinext.ae2pr.api.machine.singleblock;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.api.machine.MachineDefinition;

public final class SingleblockMachineDefinition<B extends AbstractSingleblockMachineBlock<E>,
        E extends AbstractSingleblockMachineBlockEntity, M extends AbstractSingleblockMachineMenu<E>>
        extends MachineDefinition<B, E, M> {

    private final Supplier<Block.Properties> blockProperties;
    private final Function<SingleblockMachineDefinition<B, E, M>, B> blockFactory;
    private final EntityFactory<B, E, M> entityFactory;
    private final int slots;
    private final DoubleSupplier powerCapacity;

    private SingleblockMachineDefinition(String id, Class<E> entityClass, Supplier<Block.Properties> blockProperties,
            Function<SingleblockMachineDefinition<B, E, M>, B> blockFactory, EntityFactory<B, E, M> entityFactory,
            Supplier<MenuType<M>> menuTypeFactory, MenuFactory<E, M> menuFactory, int speedUpgrades, int slots,
            DoubleSupplier powerCapacity) {
        super(id, id, entityClass, definition -> menuTypeFactory.get(), menuFactory, speedUpgrades, List.of());
        this.blockProperties = blockProperties;
        this.blockFactory = blockFactory;
        this.entityFactory = entityFactory;
        this.slots = slots;
        this.powerCapacity = powerCapacity;
    }

    public static <B extends AbstractSingleblockMachineBlock<E>, E extends AbstractSingleblockMachineBlockEntity,
            M extends AbstractSingleblockMachineMenu<E>> SingleblockMachineDefinition<B, E, M> create(String id,
            Class<E> entityClass, Supplier<Block.Properties> blockProperties,
            Function<SingleblockMachineDefinition<B, E, M>, B> blockFactory, EntityFactory<B, E, M> entityFactory,
            Supplier<MenuType<M>> menuTypeFactory, MenuFactory<E, M> menuFactory, int speedUpgrades, int slots,
            DoubleSupplier powerCapacity) {
        return new SingleblockMachineDefinition<>(id, entityClass, blockProperties, blockFactory, entityFactory,
                menuTypeFactory, menuFactory, speedUpgrades, slots, powerCapacity);
    }

    public Block.Properties properties() {
        return blockProperties.get();
    }

    // 库存槽位数，包含不参与物流的模板槽
    public int slots() {
        return slots;
    }

    public double powerCapacity() {
        return powerCapacity.getAsDouble();
    }

    @Override
    protected B createBlock() {
        return blockFactory.apply(this);
    }

    @Override
    protected E createBlockEntity(BlockPos pos, BlockState state) {
        return entityFactory.create(this, pos, state);
    }

    @FunctionalInterface
    public interface EntityFactory<B extends AbstractSingleblockMachineBlock<E>,
            E extends AbstractSingleblockMachineBlockEntity, M extends AbstractSingleblockMachineMenu<E>> {
        E create(SingleblockMachineDefinition<B, E, M> definition, BlockPos pos, BlockState state);
    }
}
