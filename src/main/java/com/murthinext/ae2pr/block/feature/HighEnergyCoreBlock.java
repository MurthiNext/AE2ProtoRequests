package com.murthinext.ae2pr.block.feature;

import com.murthinext.ae2pr.block.DisassemblableBlock;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorCoreBlockEntity;
import com.murthinext.ae2pr.machine.crystal_reactor.CrystalReactorStructure;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

// 高能核心，可作为结构方块/水晶反应器触发器
public class HighEnergyCoreBlock extends DisassemblableBlock implements EntityBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty PHASE_HIDDEN = BooleanProperty.create("phase_hidden");

    public HighEnergyCoreBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(LIT) ? 15 : 0));
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(PHASE_HIDDEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, PHASE_HIDDEN);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PHASE_HIDDEN) ? RenderShape.INVISIBLE : super.getRenderShape(state);
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(PHASE_HIDDEN) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.high_energy_core.desc"));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystalReactorCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof CrystalReactorCoreBlockEntity core) {
                core.serverTick();
            }
        };
    }

    // 反应进行中破坏核心不产生掉落
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(LIT)) {
            return List.of();
        }
        return super.getDrops(state, params);
    }

    // 空手右键即尝试触发水晶反应器
    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.getItemInHand(hand).isEmpty() || state.getValue(LIT)) {
            return InteractionResult.PASS;
        }
        if (!CrystalReactorStructure.matches(level, pos)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CrystalReactorCoreBlockEntity core && core.activate()) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
