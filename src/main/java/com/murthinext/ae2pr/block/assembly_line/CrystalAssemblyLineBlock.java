package com.murthinext.ae2pr.block.assembly_line;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;

import com.murthinext.ae2pr.ModTags;
import com.murthinext.ae2pr.logic.wrench.Wrenchable;

/**
 * 水晶装配线控制器方块。
 */
public class CrystalAssemblyLineBlock extends Block implements EntityBlock, Wrenchable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** 是否正在运行配方（驱动工作态贴图）；成型但不运行时保持静态贴图。 */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    /** 电力或输出不足导致的暂停态（黄色暂停贴图，等待重试）。 */
    public static final BooleanProperty PAUSED = BooleanProperty.create("paused");

    public CrystalAssemblyLineBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(FORMED, false)
                .setValue(RUNNING, false)
                .setValue(PAUSED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, RUNNING, PAUSED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false)
                .setValue(RUNNING, false)
                .setValue(PAUSED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyLineControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof AssemblyLineControllerBlockEntity controller) {
                controller.serverTick();
            }
        };
    }

    @Override
    public DirectionProperty wrenchFacing() {
        return FACING;
    }

    /** 仅未成型时允许旋转，避免误操作破坏已建成的机器。 */
    @Override
    public boolean canRotateWithWrench(BlockState state) {
        return !state.getValue(FORMED);
    }

    @Override
    public void onWrenchRotated(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller) {
            controller.validateStructure();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        // 扳手交互
        if (player.getItemInHand(hand).is(ModTags.WRENCHES)) {
            return InteractionResult.PASS;
        }
        // 非扳手：打开主机界面（含玩家背包，ESC/E 关闭）
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new AssemblyLineMenu(id, inventory, controller),
                            Component.translatable("block.ae2pr.crystal_assembly_line")),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller) {
            controller.validateStructure();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof AssemblyLineControllerBlockEntity controller) {
            controller.onControllerRemoved();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        // 单个组件内的换行不会被工具提示拆行（Forge 仅在自动换行时按行切分），这里按行拆成多个组件
        for (String line : Component.translatable("tooltip.ae2pr.crystal_assembly_line.desc").getString().split("\n")) {
            tooltip.add(Component.literal(line));
        }
    }
}
