package com.murthinext.ae2pr.block.machine_part;

import java.text.NumberFormat;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModTags;
import com.murthinext.ae2pr.logic.wrench.Wrenchable;

public class MachinePartBlock extends Block implements EntityBlock, Wrenchable {

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 1, MachinePartFacade.MAX_STYLE);
    public static final int DEFAULT_STYLE = 1;

    public MachinePartBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(STYLE, DEFAULT_STYLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, STYLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(STYLE, DEFAULT_STYLE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return createBlockEntity(pos, state);
    }

    public static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (isDualPart(state.getBlock())) {
            return new DualHatchBlockEntity(pos, state);
        }
        return isFluidPart(state) ? new FluidHatchBlockEntity(pos, state) : new ItemBusBlockEntity(pos, state);
    }

    public static boolean isDualPart(Block block) {
        return block == ModBlocks.AEV_DUAL_INPUT_HATCH.get()
                || block == ModBlocks.AEV_DUAL_OUTPUT_HATCH.get();
    }

    public static boolean isFluidPart(BlockState state) {
        return isFluidPart(state.getBlock());
    }

    public static boolean isFluidPart(Block block) {
        return block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get();
    }

    public static boolean isAevPart(Block block) {
        return block == ModBlocks.AEV_INPUT_BUS.get()
                || block == ModBlocks.AEV_OUTPUT_BUS.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get()
                || isDualPart(block);
    }

    public static int fluidTankCount(BlockState state) {
        return isAevPart(state.getBlock()) ? 2 : 1;
    }

    public static int fluidCapacity(BlockState state) {
        return isAevPart(state.getBlock()) ? FluidHatchBlockEntity.AEV_CAPACITY : FluidHatchBlockEntity.CAPACITY;
    }

    public static int itemSlotCount(BlockState state) {
        return isAevPart(state.getBlock()) ? 4 : 1;
    }

    public static int itemCapacity(BlockState state) {
        return isAevPart(state.getBlock()) ? ItemBusBlockEntity.AEV_CAPACITY : ItemBusBlockEntity.CAPACITY;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // 扳手交互
        if (player.getItemInHand(hand).is(ModTags.WRENCHES)) {
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
            provider = new SimpleMenuProvider((id, inventory, p) -> new ItemBusMenu(id, inventory, bus),
                    state.getBlock().getName());
        } else if (blockEntity instanceof FluidHatchBlockEntity hatch) {
            provider = new SimpleMenuProvider((id, inventory, p) -> new FluidHatchMenu(id, inventory, hatch),
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
                dropContents(level, pos, bus.getStorage());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private static void dropContents(Level level, BlockPos pos, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            while (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.split(Math.min(stack.getCount(), 64)));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        Block block = Block.byItem(stack.getItem());
        if (isDualPart(block)) {
            tooltip.add(Component.translatable("tooltip.ae2pr.machine_part.stack_limit.items",
                    NUMBER.format(ItemBusBlockEntity.AEV_CAPACITY)));
            tooltip.add(Component.translatable("tooltip.ae2pr.machine_part.item_type_count",
                    NUMBER.format(itemSlotCount(block.defaultBlockState()))));
            tooltip.add(Component.translatable("tooltip.ae2pr.machine_part.stack_limit.fluid",
                    NUMBER.format(FluidHatchBlockEntity.AEV_CAPACITY)));
            tooltip.add(Component.translatable("tooltip.ae2pr.machine_part.fluid_type_count",
                    NUMBER.format(fluidTankCount(block.defaultBlockState()))));
            return;
        }
        boolean fluid = isFluidPart(block);
        boolean aev = isAevPart(block);
        int types = fluid ? (aev ? 2 : 1) : (aev ? 4 : 1);
        int capacity = fluid
                ? (aev ? FluidHatchBlockEntity.AEV_CAPACITY : FluidHatchBlockEntity.CAPACITY)
                : (aev ? ItemBusBlockEntity.AEV_CAPACITY : ItemBusBlockEntity.CAPACITY);
        tooltip.add(Component.translatable(fluid
                ? "tooltip.ae2pr.machine_part.stack_limit.fluid"
                : "tooltip.ae2pr.machine_part.stack_limit.items",
                NUMBER.format(capacity)));
        tooltip.add(Component.translatable(fluid
                ? "tooltip.ae2pr.machine_part.fluid_type_count"
                : "tooltip.ae2pr.machine_part.item_type_count",
                NUMBER.format(types)));
    }
}
