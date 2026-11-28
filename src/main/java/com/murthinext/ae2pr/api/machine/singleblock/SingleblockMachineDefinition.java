package com.murthinext.ae2pr.api.machine.singleblock;

import com.murthinext.ae2pr.api.machine.MachineDefinition;
import com.murthinext.ae2pr.api.machine.MenuHandle;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SingleblockMachineDefinition<E extends AbstractSingleblockMachineBlockEntity>
        extends MachineDefinition<E> {

    private final Supplier<Block.Properties> blockProperties;
    private final Function<SingleblockMachineDefinition<E>, AbstractSingleblockMachineBlock<E>> blockFactory;
    private final EntityFactory<E> entityFactory;
    private final int slots;
    private final DoubleSupplier powerCapacity;

    private SingleblockMachineDefinition(
            String id,
            Class<E> entityClass,
            Supplier<Block.Properties> blockProperties,
            Function<SingleblockMachineDefinition<E>, AbstractSingleblockMachineBlock<E>> blockFactory,
            EntityFactory<E> entityFactory,
            Function<MachineDefinition<E>, MenuHandle<E, ?>> menuFactory,
            int speedUpgrades,
            int slots,
            DoubleSupplier powerCapacity) {
        super(id, id, entityClass, menuFactory, speedUpgrades, List.of());
        this.blockProperties = blockProperties;
        this.blockFactory = blockFactory;
        this.entityFactory = entityFactory;
        this.slots = slots;
        this.powerCapacity = powerCapacity;
    }

    public static <E extends AbstractSingleblockMachineBlockEntity, M extends AbstractSingleblockMachineMenu<E>>
            SingleblockMachineDefinition<E> create(
                    String id,
                    Class<E> entityClass,
                    Supplier<Block.Properties> blockProperties,
                    Function<SingleblockMachineDefinition<E>, AbstractSingleblockMachineBlock<E>> blockFactory,
                    EntityFactory<E> entityFactory,
                    Supplier<MenuType<M>> menuTypeFactory,
                    MenuHandle.MenuFactory<E, M> menuFactory,
                    int speedUpgrades,
                    int slots,
                    DoubleSupplier powerCapacity) {
        return new SingleblockMachineDefinition<>(
                id,
                entityClass,
                blockProperties,
                blockFactory,
                entityFactory,
                definition -> MenuHandle.register(definition, menuTypeFactory, menuFactory),
                speedUpgrades,
                slots,
                powerCapacity);
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
    protected AbstractSingleblockMachineBlock<E> createBlock() {
        return blockFactory.apply(this);
    }

    @Override
    protected E createBlockEntity(BlockPos pos, BlockState state) {
        return entityFactory.create(this, pos, state);
    }

    @Override
    @SuppressWarnings("unchecked") // 方块由 blockFactory 创建，类型由构造参数保证
    public AbstractSingleblockMachineBlock<E> block() {
        return (AbstractSingleblockMachineBlock<E>) super.block();
    }

    @FunctionalInterface
    public interface EntityFactory<E extends AbstractSingleblockMachineBlockEntity> {
        E create(SingleblockMachineDefinition<E> definition, BlockPos pos, BlockState state);
    }
}
