package com.murthinext.ae2pr.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.ForgeFlowingFluid;

import com.murthinext.ae2pr.ModBlocks;

/**
 * 异星熔岩流体：行为接近岩浆（黏稠、发光、流动慢），但实体伤害与物品销毁由 {@code AlienLavaBlock} 另行处理。
 * <p>
 * 源方块额外携带 {@link #CONVERSIONS}（已完成的世界交互转化次数），达到 {@link #MAX_CONVERSIONS} 后变回普通熔岩。
 */
public abstract class AlienLavaFluid extends ForgeFlowingFluid {

    /** 源方块已完成的转化配方次数，达到上限后恢复为普通熔岩 */
    public static final IntegerProperty CONVERSIONS = IntegerProperty.create("conversions", 0, 32);
    /** 单个源方块最多可完成的转化次数 */
    public static final int MAX_CONVERSIONS = 32;

    protected AlienLavaFluid(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
        // 与原版岩浆一致的音效与粒子
        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && !level.getBlockState(above).isSolidRender(level, above)) {
            if (random.nextInt(100) == 0) {
                double x = pos.getX() + random.nextDouble();
                double y = pos.getY() + 1.0D;
                double z = pos.getZ() + random.nextDouble();
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0D, 0.0D, 0.0D);
                level.playLocalSound(x, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS,
                        0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
            }

            if (random.nextInt(200) == 0) {
                level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT,
                        SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F,
                        0.9F + random.nextFloat() * 0.15F, false);
            }
        }
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        level.levelEvent(1501, pos, 0);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return false;
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid,
            Direction direction) {
        return state.getHeight(level, pos) >= 0.44444445F && fluid.defaultFluidState().is(FluidTags.WATER);
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        BlockState block = ModBlocks.ALIEN_LAVA.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
        if (state.isSource()) {
            block = block.setValue(CONVERSIONS, state.getValue(CONVERSIONS));
        }
        return block;
    }

    /** 流动状态：不携带转化次数，仅记录液面高度。 */
    public static class Flowing extends AlienLavaFluid {

        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }

    /** 源状态：携带该方块已完成的转化次数。 */
    public static class Source extends AlienLavaFluid {

        public Source(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(CONVERSIONS);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }
}
