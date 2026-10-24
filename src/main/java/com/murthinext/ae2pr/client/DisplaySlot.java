package com.murthinext.ae2pr.client;

import java.util.function.Supplier;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 只读展示槽：内容由供应器提供，仅显示单个图标，数量与提示由界面负责。
 */
public class DisplaySlot extends Slot {

    private final Supplier<ItemStack> source;

    public DisplaySlot(Supplier<ItemStack> source, int x, int y) {
        super(new SimpleContainer(1), 0, x, y);
        this.source = source;
    }

    @Override
    public ItemStack getItem() {
        ItemStack stack = source.get();
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }
}
