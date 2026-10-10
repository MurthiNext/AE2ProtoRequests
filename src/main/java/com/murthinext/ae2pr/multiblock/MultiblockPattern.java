package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.multiblock.module.ModuleInstance;
import com.murthinext.ae2pr.multiblock.module.ModuleSnapshot;

/**
 * 可复用的多方块结构模式：声明式描述 + 匹配。
 * <p>
 * 三个 {@link RelativeDirection} 轴依次展开：片（slice）→ 行（string）→ 列（char）；
 * 片可设置最小 / 最大重复次数。匹配时尝试四种镜像组合，可重复片按最大到最小重复数回溯，
 * 返回首个完全匹配；均不匹配时返回不符方块最少的结果用于诊断。
 * <p>
 * 单元格坐标以 {@link Builder#origin(int, int)} 指定的控制器单元格为原点。
 */
public final class MultiblockPattern {

    /** 匹配时尝试的镜像组合：{左右镜像, 前后镜像}。 */
    public static final boolean[][] MIRRORS = { { false, false }, { true, false }, { false, true }, { true, true } };

    private record Slice(String[] rows, int minRepeats, int maxRepeats) {
    }

    private final RelativeDirection sliceDir;
    private final RelativeDirection stringDir;
    private final RelativeDirection charDir;
    private final List<Slice> slices;
    private final Map<Character, StructurePredicate> predicates;
    private final List<StructurePredicate> leaves;
    private final int rows;
    private final int cols;
    private final int originRow;
    private final int originCol;

    private MultiblockPattern(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir,
            List<Slice> slices, Map<Character, StructurePredicate> predicates, List<StructurePredicate> leaves,
            int rows, int cols, int originRow, int originCol) {
        this.sliceDir = sliceDir;
        this.stringDir = stringDir;
        this.charDir = charDir;
        this.slices = slices;
        this.predicates = predicates;
        this.leaves = leaves;
        this.rows = rows;
        this.cols = cols;
        this.originRow = originRow;
        this.originCol = originCol;
    }

    public static Builder builder(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir) {
        return new Builder(sliceDir, stringDir, charDir);
    }

    /** 结构最小片数。 */
    public int minSlices() {
        int total = 0;
        for (Slice slice : slices) {
            total += slice.minRepeats();
        }
        return total;
    }

    /** 沿片 → 行 → 列方向的坐标比较器，保证部件顺序稳定（对应 GT 的 partSorter）。 */
    public Comparator<BlockPos> posComparator(Direction front, Direction up, boolean mirrorSide, boolean mirrorFront) {
        return sliceDir.getPosComparator(front, up, mirrorSide, mirrorFront)
                .thenComparing(stringDir.getPosComparator(front, up, mirrorSide, mirrorFront))
                .thenComparing(charDir.getPosComparator(front, up, mirrorSide, mirrorFront));
    }

    /** 按给定片数（可重复片尽量取满）计算结构内全部单元格，供状态复位等使用。 */
    public List<BlockPos> cells(BlockPos controllerPos, Direction front, Direction up, boolean mirrorSide,
            boolean mirrorFront, int totalSlices) {
        Direction sliceFacing = sliceDir.getFacing(front, up, mirrorSide, mirrorFront);
        Direction stringFacing = stringDir.getFacing(front, up, mirrorSide, mirrorFront);
        Direction charFacing = charDir.getFacing(front, up, mirrorSide, mirrorFront);
        int[] repeats = expandCounts(totalSlices);
        List<BlockPos> result = new ArrayList<>();
        int sliceIndex = 0;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            for (int rep = 0; rep < repeats[i]; rep++, sliceIndex++) {
                for (int r = 0; r < rows; r++) {
                    String row = slice.rows()[r];
                    for (int c = 0; c < cols; c++) {
                        if (predicates.get(row.charAt(c)).isAny()) {
                            continue;
                        }
                        result.add(cell(controllerPos, sliceFacing, stringFacing, charFacing, sliceIndex, r, c));
                    }
                }
            }
        }
        return result;
    }

    /**
     * 以控制器为原点匹配结构（只读，不修改世界）：依次尝试四种镜像组合，
     * 可重复片从最大重复数向最小回溯，返回首个完全匹配；均不匹配时返回最接近的结果。
     */
    public StructureResult match(Level level, BlockPos controllerPos, Direction front, Direction up, int maxSlices) {
        Attempt attempt = new Attempt();
        for (boolean[] mirror : MIRRORS) {
            Direction sliceFacing = sliceDir.getFacing(front, up, mirror[0], mirror[1]);
            Direction stringFacing = stringDir.getFacing(front, up, mirror[0], mirror[1]);
            Direction charFacing = charDir.getFacing(front, up, mirror[0], mirror[1]);
            expand(level, controllerPos, sliceFacing, stringFacing, charFacing, 0, 0, maxSlices,
                    new int[slices.size()], mirror[0], mirror[1], attempt);
            if (attempt.formed != null) {
                return attempt.formed;
            }
        }
        return attempt.best != null ? attempt.best : StructureResult.EMPTY;
    }

    /** 递归展开可重复片：从最大重复数向最小回溯，受总片数上限约束。 */
    private void expand(Level level, BlockPos controllerPos, Direction sliceFacing, Direction stringFacing,
            Direction charFacing, int index, int count, int maxSlices, int[] repeats, boolean mirrorSide,
            boolean mirrorFront, Attempt attempt) {
        if (attempt.formed != null) {
            return;
        }
        if (index == slices.size()) {
            checkCombination(level, controllerPos, sliceFacing, stringFacing, charFacing, repeats, mirrorSide,
                    mirrorFront, attempt);
            return;
        }
        Slice slice = slices.get(index);
        int remainingMin = 0;
        for (int i = index + 1; i < slices.size(); i++) {
            remainingMin += slices.get(i).minRepeats();
        }
        int maxRepeats = Math.min(slice.maxRepeats(), maxSlices - count - remainingMin);
        for (int rep = maxRepeats; rep >= slice.minRepeats(); rep--) {
            repeats[index] = rep;
            expand(level, controllerPos, sliceFacing, stringFacing, charFacing, index + 1, count + rep, maxSlices,
                    repeats, mirrorSide, mirrorFront, attempt);
            if (attempt.formed != null) {
                return;
            }
        }
    }

    /** 校验一种片数组合：逐格匹配谓词、计数并校验数量上下限。 */
    private void checkCombination(Level level, BlockPos controllerPos, Direction sliceFacing, Direction stringFacing,
            Direction charFacing, int[] repeats, boolean mirrorSide, boolean mirrorFront, Attempt attempt) {
        for (StructurePredicate leaf : leaves) {
            leaf.resetCount();
        }
        List<BlockPos> cells = new ArrayList<>();
        List<ModuleInstance> modules = new ArrayList<>();
        int mismatches = 0;
        BlockPos firstPos = null;
        char firstExpected = ' ';
        Block firstFound = null;
        int sliceIndex = 0;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            for (int rep = 0; rep < repeats[i]; rep++, sliceIndex++) {
                for (int r = 0; r < rows; r++) {
                    String row = slice.rows()[r];
                    for (int c = 0; c < cols; c++) {
                        char symbol = row.charAt(c);
                        StructurePredicate predicate = predicates.get(symbol);
                        if (predicate.isAny()) {
                            continue;
                        }
                        BlockPos pos = cell(controllerPos, sliceFacing, stringFacing, charFacing, sliceIndex, r, c);
                        BlockState state = level.getBlockState(pos);
                        StructurePredicate matched = predicate.test(level, pos, state);
                        if (matched != null) {
                            matched.incrementCount();
                            if (matched.maxCount() >= 0 && matched.count() > matched.maxCount()) {
                                matched = null;
                            } else {
                                matched.collectModules(pos, state, modules);
                            }
                        }
                        if (matched == null) {
                            mismatches++;
                            if (firstPos == null) {
                                firstPos = pos;
                                firstExpected = symbol;
                                firstFound = state.getBlock();
                            }
                            continue;
                        }
                        cells.add(pos);
                    }
                }
            }
        }
        for (StructurePredicate leaf : leaves) {
            if (leaf.minCount() > 0 && leaf.count() < leaf.minCount()) {
                mismatches++;
                if (firstPos == null) {
                    firstPos = controllerPos;
                    firstExpected = '?';
                }
            }
        }
        // 模块安装上限校验
        ModuleInstance conflict = ModuleSnapshot.firstConflict(modules);
        if (conflict != null) {
            mismatches++;
            if (firstPos == null) {
                firstPos = conflict.pos();
                firstExpected = '?';
                firstFound = level.getBlockState(conflict.pos()).getBlock();
            }
        }
        int totalSlices = 0;
        for (int repeat : repeats) {
            totalSlices += repeat;
        }
        if (mismatches == 0) {
            attempt.formed = new StructureResult(true, totalSlices, mirrorSide, mirrorFront, 0, null, ' ', null,
                    List.copyOf(cells), ModuleSnapshot.of(modules), false);
        } else if (attempt.best == null || mismatches < attempt.best.mismatches()) {
            attempt.best = new StructureResult(false, 0, mirrorSide, mirrorFront, mismatches, firstPos, firstExpected,
                    firstFound, List.of(), ModuleSnapshot.EMPTY, false);
        }
    }

    /** 按总片数把重复数尽量分配给可重复片。 */
    private int[] expandCounts(int totalSlices) {
        int[] repeats = new int[slices.size()];
        int used = 0;
        for (int i = 0; i < slices.size(); i++) {
            repeats[i] = slices.get(i).minRepeats();
            used += repeats[i];
        }
        for (int i = 0; i < slices.size() && used < totalSlices; i++) {
            int extra = Math.min(slices.get(i).maxRepeats() - repeats[i], totalSlices - used);
            repeats[i] += extra;
            used += extra;
        }
        return repeats;
    }

    /** 计算单元格世界坐标：片沿 slice 方向、行沿 string 方向、列沿 char 方向，并以控制器单元格为原点。 */
    private BlockPos cell(BlockPos controllerPos, Direction sliceFacing, Direction stringFacing, Direction charFacing,
            int sliceIndex, int row, int col) {
        return controllerPos.relative(sliceFacing, sliceIndex)
                .relative(stringFacing, row - originRow)
                .relative(charFacing, col - originCol);
    }

    /** 一次匹配的中间状态。 */
    private static final class Attempt {

        @Nullable
        StructureResult formed;
        @Nullable
        StructureResult best;
    }

    /** 模式构建器。 */
    public static final class Builder {

        private final RelativeDirection sliceDir;
        private final RelativeDirection stringDir;
        private final RelativeDirection charDir;
        private final List<Slice> slices = new ArrayList<>();
        private final Map<Character, StructurePredicate> predicates = new HashMap<>();
        private final Set<Character> usedChars = new HashSet<>();
        private int rows = -1;
        private int cols = -1;
        private int originRow;
        private int originCol;

        private Builder(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir) {
            this.sliceDir = sliceDir;
            this.stringDir = stringDir;
            this.charDir = charDir;
        }

        /** 添加固定片。 */
        public Builder slice(String... rows) {
            return sliceRepeatable(1, 1, rows);
        }

        /** 添加可重复片。 */
        public Builder sliceRepeatable(int minRepeats, int maxRepeats, String... rows) {
            if (minRepeats < 1 || maxRepeats < minRepeats) {
                throw new IllegalArgumentException("重复次数非法：" + minRepeats + "~" + maxRepeats);
            }
            validateRows(rows);
            for (String row : rows) {
                for (int i = 0; i < row.length(); i++) {
                    usedChars.add(row.charAt(i));
                }
            }
            slices.add(new Slice(rows.clone(), minRepeats, maxRepeats));
            return this;
        }

        /** 指定控制器所在的单元格（行、列），默认为 (0, 0)。 */
        public Builder origin(int row, int col) {
            this.originRow = row;
            this.originCol = col;
            return this;
        }

        /** 绑定字符对应的单元格谓词。 */
        public Builder where(char symbol, StructurePredicate predicate) {
            predicates.put(symbol, predicate);
            return this;
        }

        public MultiblockPattern build() {
            if (slices.isEmpty()) {
                throw new IllegalStateException("结构模式至少需要一个片");
            }
            if (originRow < 0 || originRow >= rows || originCol < 0 || originCol >= cols) {
                throw new IllegalStateException("控制器原点超出结构范围");
            }
            for (char symbol : usedChars) {
                if (!predicates.containsKey(symbol)) {
                    throw new IllegalStateException("字符 '" + symbol + "' 缺少谓词");
                }
            }
            int axes = 0;
            for (RelativeDirection dir : new RelativeDirection[] { sliceDir, stringDir, charDir }) {
                axes |= 1 << dir.axisIndex();
            }
            if (axes != 0b111) {
                throw new IllegalStateException("三个方向必须各占一个轴");
            }
            List<StructurePredicate> leaves = new ArrayList<>();
            for (StructurePredicate predicate : predicates.values()) {
                leaves.addAll(predicate.leaves());
            }
            return new MultiblockPattern(sliceDir, stringDir, charDir, List.copyOf(slices), Map.copyOf(predicates),
                    List.copyOf(leaves), rows, cols, originRow, originCol);
        }

        private void validateRows(String[] sliceRows) {
            if (sliceRows.length == 0 || sliceRows[0].isEmpty()) {
                throw new IllegalArgumentException("结构片不能为空");
            }
            if (rows == -1) {
                rows = sliceRows.length;
                cols = sliceRows[0].length();
            }
            if (sliceRows.length != rows) {
                throw new IllegalArgumentException("结构片行数不一致：期望 " + rows + "，实际 " + sliceRows.length);
            }
            for (String row : sliceRows) {
                if (row.length() != cols) {
                    throw new IllegalArgumentException("结构片行宽不一致：期望 " + cols + "，实际 " + row.length());
                }
            }
        }
    }
}
