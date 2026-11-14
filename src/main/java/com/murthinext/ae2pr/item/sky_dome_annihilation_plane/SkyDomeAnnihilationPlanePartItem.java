package com.murthinext.ae2pr.item.sky_dome_annihilation_plane;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import appeng.items.parts.PartItem;

import com.murthinext.ae2pr.block.sky_dome_annihilation_plane.SkyDomeAnnihilationPlanePart;

public class SkyDomeAnnihilationPlanePartItem extends PartItem<SkyDomeAnnihilationPlanePart> {

    public SkyDomeAnnihilationPlanePartItem(Properties properties) {
        super(properties, SkyDomeAnnihilationPlanePart.class, SkyDomeAnnihilationPlanePart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ae2pr.sky_dome_annihilation_plane.placement")
                            .withStyle(ChatFormatting.GRAY));
    }
}
