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

public final class MultiblockPattern {

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
    private final int originSlice;
    private final int originRow;
    private final int originCol;

    private MultiblockPattern(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir,
            List<Slice> slices, Map<Character, StructurePredicate> predicates, List<StructurePredicate> leaves,
            int rows, int cols, int originSlice, int originRow, int originCol) {
        this.sliceDir = sliceDir;
        this.stringDir = stringDir;
        this.charDir = charDir;
        this.slices = slices;
        this.predicates = predicates;
        this.leaves = leaves;
        this.rows = rows;
        this.cols = cols;
        this.originSlice = originSlice;
        this.originRow = originRow;
        this.originCol = originCol;
    }

    public static Builder builder(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir) {
        return new Builder(sliceDir, stringDir, charDir);
    }

    public int minSlices() {
        int total = 0;
        for (Slice slice : slices) {
            total += slice.minRepeats();
        }
        return total;
    }

    public int maxSlices() {
        long total = 0;
        for (Slice slice : slices) {
            total += slice.maxRepeats();
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    public StructurePredicate predicate(char symbol) {
        return predicates.get(symbol);
    }

    public Comparator<BlockPos> posComparator(Direction front, Direction up, boolean mirrorSide, boolean mirrorFront) {
        return sliceDir.getPosComparator(front, up, mirrorSide, mirrorFront)
                .thenComparing(stringDir.getPosComparator(front, up, mirrorSide, mirrorFront))
                .thenComparing(charDir.getPosComparator(front, up, mirrorSide, mirrorFront));
    }

    public record PatternCell(BlockPos pos, char symbol, int slice, int row, int col) {
    }

    public List<BlockPos> cells(BlockPos controllerPos, Direction front, Direction up, boolean mirrorSide,
            boolean mirrorFront, int totalSlices) {
        List<BlockPos> result = new ArrayList<>();
        for (PatternCell cell : patternCells(controllerPos, front, up, mirrorSide, mirrorFront, totalSlices)) {
            result.add(cell.pos());
        }
        return result;
    }

    public List<PatternCell> patternCells(BlockPos controllerPos, Direction front, Direction up, boolean mirrorSide,
            boolean mirrorFront, int totalSlices) {
        Direction sliceFacing = sliceDir.getFacing(front, up, mirrorSide, mirrorFront);
        Direction stringFacing = stringDir.getFacing(front, up, mirrorSide, mirrorFront);
        Direction charFacing = charDir.getFacing(front, up, mirrorSide, mirrorFront);
        int[] repeats = expandCounts(totalSlices);
        List<PatternCell> result = new ArrayList<>();
        int sliceIndex = 0;
        for (int i = 0; i < slices.size(); i++) {
            Slice slice = slices.get(i);
            for (int rep = 0; rep < repeats[i]; rep++, sliceIndex++) {
                for (int r = 0; r < rows; r++) {
                    String row = slice.rows()[r];
                    for (int c = 0; c < cols; c++) {
                        char symbol = row.charAt(c);
                        if (predicates.get(symbol).isAny()) {
                            continue;
                        }
                        BlockPos pos = cell(controllerPos, sliceFacing, stringFacing, charFacing, sliceIndex, r, c);
                        result.add(new PatternCell(pos, symbol, sliceIndex, r, c));
                    }
                }
            }
        }
        return result;
    }

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

    private BlockPos cell(BlockPos controllerPos, Direction sliceFacing, Direction stringFacing, Direction charFacing,
            int sliceIndex, int row, int col) {
        return controllerPos.relative(sliceFacing, sliceIndex - originSlice)
                .relative(stringFacing, row - originRow)
                .relative(charFacing, col - originCol);
    }

    private static final class Attempt {

        @Nullable
        StructureResult formed;
        @Nullable
        StructureResult best;
    }


    public static final class Builder {

        private final RelativeDirection sliceDir;
        private final RelativeDirection stringDir;
        private final RelativeDirection charDir;
        private final List<Slice> slices = new ArrayList<>();
        private final Map<Character, StructurePredicate> predicates = new HashMap<>();
        private final Set<Character> usedChars = new HashSet<>();
        private int rows = -1;
        private int cols = -1;
        private int originSlice;
        private int originRow;
        private int originCol;

        private Builder(RelativeDirection sliceDir, RelativeDirection stringDir, RelativeDirection charDir) {
            this.sliceDir = sliceDir;
            this.stringDir = stringDir;
            this.charDir = charDir;
        }

        public Builder slice(String... rows) {
            return sliceRepeatable(1, 1, rows);
        }

        public Builder sliceRepeatable(int minRepeats, int maxRepeats, String... rows) {
            if (minRepeats < 1 || maxRepeats < minRepeats) {
                throw new IllegalArgumentException("Invalid repeat count: " + minRepeats + "~" + maxRepeats);
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

        public Builder origin(int row, int col) {
            this.originRow = row;
            this.originCol = col;
            return this;
        }

        public Builder sliceOrigin(int sliceIndex) {
            this.originSlice = sliceIndex;
            return this;
        }

        public Builder where(char symbol, StructurePredicate predicate) {
            predicates.put(symbol, predicate);
            return this;
        }

        public MultiblockPattern build() {
            if (slices.isEmpty()) {
                throw new IllegalStateException("Structure pattern needs at least one slice");
            }
            if (originRow < 0 || originRow >= rows || originCol < 0 || originCol >= cols) {
                throw new IllegalStateException("Controller origin is outside the structure");
            }
            int minSlices = 0;
            for (Slice slice : slices) {
                minSlices += slice.minRepeats();
            }
            if (originSlice < 0 || originSlice >= minSlices) {
                throw new IllegalStateException("Controller slice is outside the structure");
            }
            for (char symbol : usedChars) {
                if (!predicates.containsKey(symbol)) {
                    throw new IllegalStateException("Missing predicate for symbol '" + symbol + "'");
                }
            }
            int axes = 0;
            for (RelativeDirection dir : new RelativeDirection[] { sliceDir, stringDir, charDir }) {
                axes |= 1 << dir.axisIndex();
            }
            if (axes != 0b111) {
                throw new IllegalStateException("The three axes must each occupy a distinct direction");
            }
            List<StructurePredicate> leaves = new ArrayList<>();
            for (StructurePredicate predicate : predicates.values()) {
                leaves.addAll(predicate.leaves());
            }
            return new MultiblockPattern(sliceDir, stringDir, charDir, List.copyOf(slices), Map.copyOf(predicates),
                    List.copyOf(leaves), rows, cols, originSlice, originRow, originCol);
        }

        private void validateRows(String[] sliceRows) {
            if (sliceRows.length == 0 || sliceRows[0].isEmpty()) {
                throw new IllegalArgumentException("Structure slice must not be empty");
            }
            if (rows == -1) {
                rows = sliceRows.length;
                cols = sliceRows[0].length();
            }
            if (sliceRows.length != rows) {
                throw new IllegalArgumentException(
                        "Structure slice row count mismatch: expected " + rows + ", got " + sliceRows.length);
            }
            for (String row : sliceRows) {
                if (row.length() != cols) {
                    throw new IllegalArgumentException(
                            "Structure slice row width mismatch: expected " + cols + ", got " + row.length());
                }
            }
        }
    }
}
