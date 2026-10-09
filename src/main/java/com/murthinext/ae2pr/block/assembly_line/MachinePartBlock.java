package com.murthinext.ae2pr.block.assembly_line;

import java.text.NumberFormat;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModTags;

/**
 * 机器部件方块（赛特斯石英水晶 / AEV 的输入总线、输入仓、输出总线、输出仓）。
 * <p>
 * 仓为流体存储（AEV 2 槽、石英 1 槽），总线为物品存储（AEV 4 槽、石英 1 槽）；
 * 扳手右键旋转，Shift+右键拆卸。
 */
public class MachinePartBlock extends Block implements EntityBlock {

    /** 扳手旋转顺序 */
    private static final Direction[] ROTATION_ORDER = { Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST };

    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /**
     * 部件存放的多方块外观 id（仅 FORMED=true 时由渲染端解读），见 {@link MachinePartFacade}；
     * 未成型时为 {@link #DEFAULT_STYLE}。
     */
    public static final IntegerProperty STYLE = IntegerProperty.create("style", 1, MachinePartFacade.MAX_STYLE);
    /** STYLE 默认值（未成型 / 未登记外观） */
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
        return isFluidPart(state) ? new FluidHatchBlockEntity(pos, state) : new ItemBusBlockEntity(pos, state);
    }

    /** 是否为流体部件（输入仓 / 输出仓）。 */
    public static boolean isFluidPart(BlockState state) {
        return isFluidPart(state.getBlock());
    }

    /** 静态判定：该方块是否为流体部件。 */
    public static boolean isFluidPart(Block block) {
        return block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get()
                || block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get();
    }

    /** 静态判定：该方块是否为 AEV 部件。 */
    public static boolean isAevPart(Block block) {
        return block == ModBlocks.AEV_INPUT_BUS.get()
                || block == ModBlocks.AEV_OUTPUT_BUS.get()
                || block == ModBlocks.AEV_INPUT_HATCH.get()
                || block == ModBlocks.AEV_OUTPUT_HATCH.get();
    }

    /** 流体槽数：AEV 仓 2 槽，石英仓 1 槽。 */
    public static int fluidTankCount(BlockState state) {
        return isAevPart(state.getBlock()) ? 2 : 1;
    }

    /** 单槽流体容量（mB）：AEV 仓 1024 桶，石英仓 16K 桶。 */
    public static int fluidCapacity(BlockState state) {
        return isAevPart(state.getBlock()) ? FluidHatchBlockEntity.AEV_CAPACITY : FluidHatchBlockEntity.CAPACITY;
    }

    /** 物品槽数：AEV 总线 4 槽，石英总线 1 槽。 */
    public static int itemSlotCount(BlockState state) {
        return isAevPart(state.getBlock()) ? 4 : 1;
    }

    /** 单槽物品上限：AEV 总线 2048 件，石英总线 32K 件。 */
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModTags.WRENCHES)) {
            if (player.isSecondaryUseActive()) {
                return disassemble(level, pos, state, player);
            }
            return rotate(level, pos, state);
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            openMenu(serverPlayer, level, pos, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 非扳手右键打开部件界面：总线为物品存储，仓为流体存储。 */
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

    /** 扳手旋转：成型后也可调整朝向面（仅影响该部件的拉取/推出方向，不影响结构检测）。 */
    private static InteractionResult rotate(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(FACING, nextFacing(state.getValue(FACING))), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** 扳手 Shift+右键快速拆卸：部件本体回收到玩家背包，内部物品经 {@link #onRemove} 照常掉落。 */
    private static InteractionResult disassemble(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            ItemStack part = new ItemStack(state.getBlock());
            ItemHandlerHelper.giveItemToPlayer(player, part);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Shift+右键手持扳手时放行方块交互。
     * <p>
     * 原版在潜行且手持物品时会跳过 {@link Block#use}，导致 {@link #disassemble} 永远无法触发；
     * 这里把事件的使用方块结果改为 ALLOW，让两侧照常走 {@link Block#use}。
     */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() == Event.Result.DENY) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive() || !event.getItemStack().is(ModTags.WRENCHES)) {
            return;
        }
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MachinePartBlock) {
            event.setUseBlock(Event.Result.ALLOW);
        }
    }

    private static Direction nextFacing(Direction current) {
        for (int i = 0; i < ROTATION_ORDER.length; i++) {
            if (ROTATION_ORDER[i] == current) {
                return ROTATION_ORDER[(i + 1) % ROTATION_ORDER.length];
            }
        }
        return Direction.NORTH;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // 破坏时把机器存储掉落出来（成型状态切换不触发）；大堆叠按 64 一组散落，罐内流体不回收
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof ItemBusBlockEntity bus) {
                dropContents(level, pos, bus.getStorage());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** 把物品处理器内容按 64 一组散落到地面。 */
    private static void dropContents(Level level, BlockPos pos, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            while (!stack.isEmpty()) {
                Block.popResource(level, pos, stack.split(Math.min(stack.getCount(), 64)));
            }
        }
    }

    /** 物品 Tooltip：显示该部件的单槽堆叠上限与类型数量。 */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        Block block = Block.byItem(stack.getItem());
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
        tooltip.add(Component.translatable("tooltip.ae2pr.machine_part.type_count", NUMBER.format(types)));
    }
}
