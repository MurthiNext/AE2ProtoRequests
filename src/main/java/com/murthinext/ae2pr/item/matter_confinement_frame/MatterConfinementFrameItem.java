package com.murthinext.ae2pr.item.matter_confinement_frame;

import appeng.api.util.AEColor;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class MatterConfinementFrameItem extends Item {

    private static final String MATTER_TAG = "Matter";

    public MatterConfinementFrameItem(Properties properties) {
        super(properties);
    }

    public static Map<AEColor, Integer> getContents(ItemStack stack) {
        var contents = new EnumMap<AEColor, Integer>(AEColor.class);
        var tag = stack.getTag();
        if (tag != null && tag.contains(MATTER_TAG, Tag.TAG_COMPOUND)) {
            var matter = tag.getCompound(MATTER_TAG);
            for (var color : AEColor.VALID_COLORS) {
                if (matter.contains(color.registryPrefix, Tag.TAG_INT)) {
                    int count = matter.getInt(color.registryPrefix);
                    if (count > 0) {
                        contents.put(color, count);
                    }
                }
            }
        }
        return Map.copyOf(contents);
    }

    public static void setContents(ItemStack stack, Map<AEColor, Integer> contents) {
        var matter = new CompoundTag();
        for (var color : AEColor.VALID_COLORS) {
            int count = contents.getOrDefault(color, 0);
            if (count > 0) {
                matter.putInt(color.registryPrefix, count);
            }
        }
        if (matter.isEmpty()) {
            stack.removeTagKey(MATTER_TAG);
        } else {
            stack.getOrCreateTag().put(MATTER_TAG, matter);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var contents = getContents(stack);
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ae2pr.matter_confinement_frame.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            for (var color : AEColor.VALID_COLORS) {
                int count = contents.getOrDefault(color, 0);
                if (count > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.ae2pr.matter_confinement_frame.content",
                                    Component.translatable(color.translationKey),
                                    count)
                            .withStyle(style -> style.withColor(color.whiteVariant)));
                }
            }
        }
    }
}
