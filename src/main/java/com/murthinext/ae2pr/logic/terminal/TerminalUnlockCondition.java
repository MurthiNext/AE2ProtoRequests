package com.murthinext.ae2pr.logic.terminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public record TerminalUnlockCondition(ResourceLocation id, boolean tag, int count) {

    public static final int MAX_COUNT = 9999;
    public static final int MAX_KEY_LENGTH = 160;

    public static String key(ResourceLocation id, boolean tag, int count) {
        return (tag ? "tag|" : "item|") + id + "|" + count;
    }

    public static TerminalUnlockCondition parse(String key) {
        if (key == null || key.length() > MAX_KEY_LENGTH) {
            return null;
        }
        String[] parts = key.split("\\|", -1);
        if (parts.length != 3) {
            return null;
        }
        boolean tag;
        if (parts[0].equals("item")) {
            tag = false;
        } else if (parts[0].equals("tag")) {
            tag = true;
        } else {
            return null;
        }
        ResourceLocation id;
        try {
            id = ResourceLocation.parse(parts[1]);
        } catch (RuntimeException exception) {
            return null;
        }
        int count;
        try {
            count = Integer.parseInt(parts[2]);
        } catch (NumberFormatException exception) {
            return null;
        }
        return count < 1 || count > MAX_COUNT ? null : new TerminalUnlockCondition(id, tag, count);
    }

    // 逐条判定条件是否被该玩家满足，返回其中已满足的标识
    public static List<String> satisfied(ServerPlayer player, List<String> keys) {
        List<String> satisfied = new ArrayList<>();
        for (String key : keys) {
            TerminalUnlockCondition condition = parse(key);
            if (condition != null && condition.matches(player) && !satisfied.contains(key)) {
                satisfied.add(key);
            }
        }
        return satisfied;
    }

    public boolean matches(ServerPlayer player) {
        int remaining = count - count(player.getInventory().items);
        if (remaining <= 0) {
            return true;
        }
        remaining -= count(player.getInventory().armor);
        if (remaining <= 0) {
            return true;
        }
        return remaining - count(player.getInventory().offhand) <= 0;
    }

    private int count(Iterable<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && matches(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean matches(ItemStack stack) {
        if (tag) {
            return stack.is(TagKey.create(Registries.ITEM, id));
        }
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item != null && stack.is(item);
    }
}
