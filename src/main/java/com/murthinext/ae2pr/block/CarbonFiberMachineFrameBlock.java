package com.murthinext.ae2pr.block;

import com.murthinext.ae2pr.api.BlockProperties;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

public final class CarbonFiberMachineFrameBlock extends DisassemblableBlock {

    public CarbonFiberMachineFrameBlock() {
        super(BlockProperties.metal(MapColor.COLOR_GRAY).noOcclusion());
    }

    @Override
    public void appendHoverText(
            ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.carbon_fiber_machine_frame.desc")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
