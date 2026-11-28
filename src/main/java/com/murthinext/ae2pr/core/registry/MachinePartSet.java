package com.murthinext.ae2pr.core.registry;

import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock.Tier;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * 一个电压等级的全部仓室。QUARTZ 没有双用仓室，对应字段为 null。
 */
public record MachinePartSet(
        Tier tier,
        BlockDef<MachinePartBlock> inputBus,
        BlockDef<MachinePartBlock> outputBus,
        BlockDef<MachinePartBlock> inputHatch,
        BlockDef<MachinePartBlock> outputHatch,
        @Nullable BlockDef<MachinePartBlock> dualInputHatch,
        @Nullable BlockDef<MachinePartBlock> dualOutputHatch) {

    public List<BlockDef<MachinePartBlock>> all() {
        return Stream.of(inputBus, outputBus, inputHatch, outputHatch, dualInputHatch, dualOutputHatch)
                .filter(Objects::nonNull)
                .toList();
    }
}
