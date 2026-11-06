package com.murthinext.ae2pr.logic.collapse;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.murthinext.ae2pr.api.collapse.Collapsible;

public final class CollapseSource {

    private final ResourceLocation id;
    private final boolean authorized;
    private final Collapsible content;

    private CollapseSource(ResourceLocation id, boolean authorized, Collapsible content) {
        this.id = id;
        this.authorized = authorized;
        this.content = content;
    }

    @Nullable
    public ResourceLocation id() {
        return id;
    }

    // 第三方主动注册的来源视为已授权
    public boolean isAuthorized() {
        return authorized;
    }

    public List<GenericStack> read() {
        return content.readContents();
    }

    public void clear() {
        content.clearContents();
    }

    public static CollapseSource of(ResourceLocation id, Collapsible content) {
        return new CollapseSource(id, false, content);
    }

    public static CollapseSource authorized(ResourceLocation id, Collapsible content) {
        return new CollapseSource(id, true, content);
    }

    public static CollapseSource of(ResourceLocation id, GenericInternalInventory inventory) {
        return of(id, new Collapsible() {
            @Override
            public List<GenericStack> readContents() {
                var stacks = new ArrayList<GenericStack>(inventory.size());
                for (int slot = 0; slot < inventory.size(); slot++) {
                    var stack = inventory.getStack(slot);
                    if (stack != null) {
                        stacks.add(stack);
                    }
                }
                return stacks;
            }

            @Override
            public void clearContents() {
                inventory.beginBatch();
                for (int slot = 0; slot < inventory.size(); slot++) {
                    if (inventory.getStack(slot) != null) {
                        inventory.setStack(slot, null);
                    }
                }
                inventory.endBatchSuppressed();
            }
        });
    }

    public static CollapseSource of(ResourceLocation id, IItemHandler handler) {
        return of(id, new Collapsible() {
            @Override
            public List<GenericStack> readContents() {
                var stacks = new ArrayList<GenericStack>(handler.getSlots());
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    var stack = handler.getStackInSlot(slot);
                    var key = stack.isEmpty() ? null : AEItemKey.of(stack);
                    if (key != null) {
                        stacks.add(new GenericStack(key, stack.getCount()));
                    }
                }
                return stacks;
            }

            @Override
            public void clearContents() {
                clearItems(handler);
            }
        });
    }

    private static void clearItems(IItemHandler handler) {
        if (handler instanceof IItemHandlerModifiable modifiable) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                modifiable.setStackInSlot(slot, ItemStack.EMPTY);
            }
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            while (!handler.extractItem(slot, Integer.MAX_VALUE, false).isEmpty()) {
                // 只读库存第一次就返回空，循环立即结束
            }
        }
    }
}
