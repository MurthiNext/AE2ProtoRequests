package com.murthinext.ae2pr.block.machine_part;

import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.api.text.AmountText;
import com.murthinext.ae2pr.api.text.Voltage;
import com.murthinext.ae2pr.api.wrench.WrenchTags;
import com.murthinext.ae2pr.api.wrench.Wrenchable;
import com.murthinext.ae2pr.block.StabilityCasingBlock;
import com.murthinext.ae2pr.logic.collapse.CollapseHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

public class MachinePartBlock extends Block implements EntityBlock, Wrenchable {

    public enum Tier {
        QUARTZ(1, 32 * 1024, 1, 16 * 1024 * 1000),
        AEV(4, 2048, 2, 1024 * 1000),
        XPV(9, 4096, 4, 4096 * 1000);

        private final int itemSlots;
        private final int itemCapacity;
        private final int fluidTanks;
        final int fluidCapacity;

        Tier(int itemSlots, int itemCapacity, int fluidTanks, int fluidCapacity) {
            this.itemSlots = itemSlots;
            this.itemCapacity = itemCapacity;
            this.fluidTanks = fluidTanks;
            this.fluidCapacity = fluidCapacity;
        }
    }

    public enum Storage {
        ITEMS,
        FLUIDS,
        BOTH
    }

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 1, MachinePartFacade.MAX_STYLE);
    public static final int DEFAULT_STYLE = 1;

    private final Tier tier;
    private final Storage storage;
    private final boolean output;

    public MachinePartBlock(Tier tier, Storage storage, boolean output) {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F)
                .sound(SoundType.METAL)
                .lightLevel(StabilityCasingBlock::fieldLight)
                .requiresCorrectToolForDrops());
        this.tier = tier;
        this.storage = storage;
        this.output = output;
        registerDefaultState(stateDefinition
                .any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(StabilityCasingBlock.FIELD_LIT, false)
                .setValue(STYLE, DEFAULT_STYLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, STYLE, StabilityCasingBlock.FIELD_LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(STYLE, DEFAULT_STYLE);
    }

    @Override
    public MutableComponent getName() {
        return Voltage.name(getDescriptionId());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    public static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return switch (part(state).storage) {
            case ITEMS -> new ItemBusBlockEntity(pos, state);
            case FLUIDS -> new FluidHatchBlockEntity(pos, state);
            case BOTH -> new DualHatchBlockEntity(pos, state);
        };
    }

    public Tier tier() {
        return tier;
    }

    private static MachinePartBlock part(BlockState state) {
        return (MachinePartBlock) state.getBlock();
    }

    static boolean isOutput(BlockState state) {
        return part(state).output;
    }

    static int fluidTankCount(BlockState state) {
        return part(state).tier.fluidTanks;
    }

    static int fluidCapacity(BlockState state) {
        return part(state).tier.fluidCapacity;
    }

    static int itemSlotCount(BlockState state) {
        return part(state).tier.itemSlots;
    }

    static int itemCapacity(BlockState state) {
        return part(state).tier.itemCapacity;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof FluidHatchBlockEntity hatch) {
                hatch.serverTick();
            } else if (blockEntity instanceof ItemBusBlockEntity bus) {
                bus.serverTick();
            }
        };
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // 扳手交互
        if (player.getItemInHand(hand).is(WrenchTags.WRENCH)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openMenu(ServerPlayer player, Level level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        MenuProvider provider = null;
        if (blockEntity instanceof ItemBusBlockEntity bus) {
            provider = new SimpleMenuProvider(
                    (id, inventory, p) -> new ItemBusMenu(id, inventory, bus),
                    state.getBlock().getName());
        } else if (blockEntity instanceof FluidHatchBlockEntity hatch) {
            provider = new SimpleMenuProvider(
                    (id, inventory, p) -> new FluidHatchMenu(id, inventory, hatch),
                    state.getBlock().getName());
        }
        if (provider != null) {
            NetworkHooks.openScreen(player, provider, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 破坏时掉落物品存储（成型切换不触发），流体不回收
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ItemBusBlockEntity bus) {
                dropContents(level, pos, state, bus.getStorage());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // 物品总量超量时整仓压成一个坍缩物品存储元件，否则逐个掉落
    private static void dropContents(Level level, BlockPos pos, BlockState state, IItemHandler handler) {
        ItemStack collapsed = CollapseHandler.tryCollapse(BuiltInRegistries.BLOCK.getKey(state.getBlock()), handler);
        if (collapsed != null) {
            Block.popResource(level, pos, collapsed);
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            while (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.split(Math.min(stack.getCount(), 64)));
            }
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        if (storage != Storage.FLUIDS) {
            tooltip.add(Component.translatable(
                    "tooltip.ae2pr.machine_part.stack_limit.items", AmountText.exact(tier.itemCapacity)));
            tooltip.add(Component.translatable(
                    "tooltip.ae2pr.machine_part.item_type_count", AmountText.exact(tier.itemSlots)));
        }
        if (storage != Storage.ITEMS) {
            tooltip.add(Component.translatable(
                    "tooltip.ae2pr.machine_part.stack_limit.fluid", AmountText.exact(tier.fluidCapacity)));
            tooltip.add(Component.translatable(
                    "tooltip.ae2pr.machine_part.fluid_type_count", AmountText.exact(tier.fluidTanks)));
        }
    }
}
