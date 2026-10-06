package com.murthinext.ae2pr.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.murthinext.ae2pr.block.meteorite.MeteoriteOres;

import appeng.worldgen.meteorite.MeteoriteBlockPutter;
import appeng.worldgen.meteorite.MeteoritePlacer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 修正放大陨石后的循环边界，并按矿脉替换部分天空石为陨石矿石。
 */
@Mixin(value = MeteoritePlacer.class, remap = false)
public abstract class MeteoritePlacerMixin {

    /** 矿石占陨石方块的比例，对应矿石:天空石 = 1:3 */
    @Unique
    private static final float ORE_FRACTION = 0.1F;

    @Shadow
    private RandomSource random;
    @Shadow
    private double meteoriteSize;
    @Shadow
    private int x;
    @Shadow
    private int y;
    @Shadow
    private int z;
    @Shadow
    private BoundingBox boundingBox;

    @Unique
    private final Map<BlockPos, BlockState> ae2pr$pendingOres = new HashMap<>();
    @Unique
    private float ae2pr$oreCredit;

    @ModifyConstant(method = "placeMeteoriteSkyStone", constant = @Constant(intValue = 8))
    private int ae2pr$expandMeteoriteBody(int original) {
        return Math.max(8, ae2pr$bodyBound());
    }

    @ModifyConstant(method = "placeCrater", constant = @Constant(intValue = 5))
    private int ae2pr$deepenCraterLoop(int original) {
        return Math.max(5, ae2pr$bodyBound());
    }

    @ModifyConstant(method = "placeCraterLake", constant = @Constant(intValue = 5))
    private int ae2pr$deepenCraterLakeLoop(int original) {
        return Math.max(5, ae2pr$bodyBound());
    }

    @ModifyConstant(method = "decay", constant = @Constant(intValue = 9))
    private int ae2pr$deepenDecayLoop(int original) {
        return Math.max(9, ae2pr$bodyBound());
    }

    @Redirect(method = "placeMeteoriteSkyStone", at = @At(value = "INVOKE", target = "Lappeng/worldgen/meteorite/MeteoriteBlockPutter;put(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z", ordinal = 2), remap = false)
    private boolean ae2pr$placeOreOrSkyStone(MeteoriteBlockPutter putter, LevelAccessor level, BlockPos pos,
            BlockState skyStone) {
        ae2pr$oreCredit += ORE_FRACTION;
        BlockState ore = ae2pr$pendingOres.remove(pos);
        if (ore == null) {
            ore = ae2pr$tryStartVein(pos);
        }
        return putter.put(level, pos, ore != null ? ore : skyStone);
    }

    @Unique
    private int ae2pr$bodyBound() {
        return Math.max(8, (int) Math.ceil(meteoriteSize) + 1);
    }

    /**
     * 按累积额度决定是否开启一条矿脉，并预先登记整条矿脉的方块。
     */
    @Unique
    private BlockState ae2pr$tryStartVein(BlockPos pos) {
        int length = 2 + random.nextInt(5);
        if (ae2pr$oreCredit < length) {
            return null;
        }
        BlockState ore = MeteoriteOres.randomOre(random);
        int placed = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < length; i++) {
            if (ae2pr$isPlacedByCurrentPass(cursor)) {
                ae2pr$pendingOres.put(cursor.immutable(), ore);
                placed++;
            }
            // 只朝方块扫描顺序的前方延伸，保证矿脉在本次区块生成内放置完整
            if (random.nextFloat() < 0.75F) {
                cursor.move(0, 1, 0);
            } else if (random.nextBoolean()) {
                cursor.move(1, 0, 0);
            } else {
                cursor.move(0, 0, 1);
            }
        }
        ae2pr$oreCredit -= placed;
        return ae2pr$pendingOres.remove(pos);
    }

    @Unique
    private boolean ae2pr$isPlacedByCurrentPass(BlockPos pos) {
        int bound = ae2pr$bodyBound();
        int minX = Math.max(boundingBox.minX(), x - bound);
        int maxX = Math.min(boundingBox.maxX(), x + bound);
        int minZ = Math.max(boundingBox.minZ(), z - bound);
        int maxZ = Math.min(boundingBox.maxZ(), z + bound);
        if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ) {
            return false;
        }
        // 与原版一致，Y 循环上界为开区间且不按包围盒裁剪
        if (pos.getY() < y - bound || pos.getY() >= y + bound) {
            return false;
        }
        double dx = pos.getX() - x;
        double dy = pos.getY() - y;
        double dz = pos.getZ() - z;
        if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && Math.abs(dz) <= 1) {
            return false;
        }
        return dx * dx * 0.7 + dy * dy * (dy > 0 ? 1.4 : 0.8) + dz * dz * 0.7 < meteoriteSize * meteoriteSize;
    }
}
