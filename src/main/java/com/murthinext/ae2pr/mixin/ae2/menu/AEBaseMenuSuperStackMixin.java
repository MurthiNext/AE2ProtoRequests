package com.murthinext.ae2pr.mixin.ae2.menu;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.murthinext.ae2pr.logic.upgrade.SuperStackSupport;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.Upgrades;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.CraftingMatrixSlot;
import appeng.menu.slot.DisabledSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.InaccessibleSlot;
import appeng.util.ConfigMenuInventory;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// 超级堆叠卡：接管返回/存储槽的玩家交互，避免界面层按原版堆叠上限截断或吞掉超量物品
@Mixin(value = AEBaseMenu.class, priority = 500, remap = false)
public abstract class AEBaseMenuSuperStackMixin {

    @Shadow(remap = false)
    private boolean isPlayerSideSlot(Slot slot) {
        throw new AssertionError();
    }

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void ae2pr$onClicked(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        var menu = (AEBaseMenu) (Object) this;
        if (slotId < 0 || slotId >= menu.slots.size()) {
            return;
        }
        var slot = menu.slots.get(slotId);
        var inv = ae2pr$managedInv(slot);
        if (inv == null) {
            return;
        }
        if (!menu.isValidMenu() || player.isSpectator() || !menu.stillValid(player)) {
            ci.cancel();
            return;
        }
        if (clickType == ClickType.QUICK_MOVE) {
            // 返回槽对自动化禁止抽出
            if (!menu.isClientSide()) {
                menu.quickMoveStack(player, slotId);
            }
            ci.cancel();
            return;
        }
        if (clickType == ClickType.CLONE) {
            // 创造模式中键复制
            if (!menu.isClientSide()) {
                ae2pr$clone(menu, slot, inv, player);
            }
            ci.cancel();
            return;
        }
        if (clickType == ClickType.SWAP) {
            if (!menu.isClientSide()) {
                ae2pr$swapWithHotbar(menu, slot, inv, button, player);
            }
            ci.cancel();
            return;
        }
        if (clickType == ClickType.THROW) {
            if (!menu.isClientSide()) {
                ae2pr$throwStack(menu, slot, inv, button, player);
            }
            ci.cancel();
            return;
        }
        if (clickType == ClickType.PICKUP_ALL) {
            // 双击合并：与左键同语义，只搬一个常规堆叠
            if (!menu.isClientSide()) {
                ae2pr$pickup(menu, slot, inv, false);
            }
            ci.cancel();
            return;
        }
        if (clickType != ClickType.PICKUP || (button != 0 && button != 1)) {
            ci.cancel();
            return;
        }
        if (menu.isClientSide()) {
            // 客户端不做预测，等待服务端广播结果
            ci.cancel();
            return;
        }
        ae2pr$pickup(menu, slot, inv, button == 1);
        ci.cancel();
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = true)
    private void ae2pr$onQuickMoveStack(Player player, int idx, CallbackInfoReturnable<ItemStack> cir) {
        var menu = (AEBaseMenu) (Object) this;
        if (menu.isClientSide() || !menu.isValidMenu() || !menu.stillValid(player) || player.isSpectator() || idx < 0
                || idx >= menu.slots.size()) {
            return;
        }
        var source = menu.slots.get(idx);
        if (source instanceof DisabledSlot || source instanceof InaccessibleSlot) {
            return;
        }
        var inv = ae2pr$managedInv(source);
        if (inv != null) {
            ae2pr$slotToPlayer(menu, source, inv, cir);
            return;
        }
        if (source.container != menu.getPlayerInventory()) {
            // 只接管玩家背包/快捷栏，合成格等特殊语义保持原版
            return;
        }
        var stack = source.getItem();
        if (!stack.isEmpty()) {
            ae2pr$playerToMenu(menu, source, stack, cir);
        }
    }

    /**
     * 玩家背包 -> 菜单。非存储槽永远先于返回/存储槽。
     */
    @Unique
    private void ae2pr$playerToMenu(AEBaseMenu menu, Slot source, ItemStack stack,
            CallbackInfoReturnable<ItemStack> cir) {
        var key = AEItemKey.of(stack);
        if (key == null || GenericStack.isWrapped(stack)) {
            return;
        }
        // 只接管真正带超级堆叠槽的菜单
        if (!ae2pr$hasSuperStackStorage(menu)) {
            return;
        }
        boolean pattern = PatternDetailsHelper.isEncodedPattern(stack);
        boolean storageLike = !(Upgrades.isUpgradeCardItem(stack) || pattern);
        // 没有任何返回/存储槽可写时，普通物品交回原版完整流程
        if (storageLike && !ae2pr$hasManagedTarget(menu, key)) {
            return;
        }
        int remaining = stack.getCount();
        // 第一遍：升级卡槽、样板槽等非存储槽
        remaining = ae2pr$mergeIntoMenu(menu, stack, key, remaining, false);
        remaining = ae2pr$fillMenuEmpty(menu, stack, key, remaining, false);
        // 第二遍：返回/存储槽兜底；样板例外
        if (!pattern) {
            remaining = ae2pr$mergeIntoMenu(menu, stack, key, remaining, true);
            remaining = ae2pr$fillMenuEmpty(menu, stack, key, remaining, true);
        }
        if (remaining >= stack.getCount()) {
            // 普通物品、或还有别的槽能用原版语义接收（如 FakeSlot 样板槽）时交回原版
            if (storageLike || ae2pr$hasNonStorageDestination(menu, stack)) {
                return;
            }
            // 升级卡/样板无处可放时留在玩家背包里
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        var rest = stack.copy();
        rest.setCount(remaining);
        source.set(remaining > 0 ? rest : ItemStack.EMPTY);
        source.setChanged();
        menu.broadcastChanges();
        cir.setReturnValue(ItemStack.EMPTY);
    }

    /**
     * 其它槽能否用原版语义接收该物品（样板槽、FakeSlot 等）。
     */
    @Unique
    private boolean ae2pr$hasNonStorageDestination(AEBaseMenu menu, ItemStack stack) {
        for (Slot dest : menu.slots) {
            if (isPlayerSideSlot(dest) || ae2pr$isStorageSlot(dest)) {
                continue;
            }
            if (dest.mayPlace(stack)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean ae2pr$hasSuperStackStorage(AEBaseMenu menu) {
        for (Slot slot : menu.slots) {
            if (ae2pr$isStorageSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private boolean ae2pr$isStorageSlot(Slot slot) {
        return slot instanceof AppEngSlot aeSlot
                && aeSlot.getInventory() instanceof ConfigMenuInventory wrapper
                && wrapper.getDelegate().getMode() == GenericStackInv.Mode.STORAGE;
    }

    @Inject(method = "canDragTo", at = @At("HEAD"), cancellable = true, remap = true)
    private void ae2pr$canDragTo(Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (ae2pr$managedInv(slot) != null) {
            cir.setReturnValue(false);
        }
    }

    /**
     * @return 该槽接管的存储库存；未扩容、无超量存量或内容不是物品时返回 null，交回原版
     */
    @Unique
    @Nullable
    private GenericStackInv ae2pr$managedInv(Slot slot) {
        // isActive 同时覆盖槽位被禁用与临时置灰
        if (!(slot instanceof AppEngSlot aeSlot) || !aeSlot.isActive()) {
            return null;
        }
        if (slot instanceof FakeSlot || slot instanceof CraftingMatrixSlot || slot instanceof InaccessibleSlot) {
            return null;
        }
        if (!(aeSlot.getInventory() instanceof ConfigMenuInventory wrapper)) {
            return null;
        }
        var delegate = wrapper.getDelegate();
        if (delegate.getMode() != GenericStackInv.Mode.STORAGE) {
            return null;
        }
        if (!(delegate instanceof SuperStackSupport.Target target) || !target.ae2pr$isManaged()) {
            return null;
        }
        // 真实库存索引取自槽位自身，菜单 index 只代表显示顺序
        int invSlot = aeSlot.getContainerSlot();
        if (invSlot < 0 || invSlot >= delegate.size()) {
            return null;
        }
        var stack = delegate.getStack(invSlot);
        if (stack != null) {
            if (!(stack.what() instanceof AEItemKey key)) {
                return null;
            }
            if (stack.amount() > key.getMaxStackSize()) {
                return delegate;
            }
        }
        // 空槽按物品通道容量判断是否已被扩容
        return delegate.getCapacity(AEKeyType.items()) > Container.LARGE_MAX_STACK_SIZE ? delegate : null;
    }

    /**
     * 界面包装层的放置判定。超量槽在界面里表现为包装物品，因此不能用 {@code slot.mayPlace}。
     */
    @Unique
    private boolean ae2pr$accepts(Slot slot, ItemStack stack) {
        return slot instanceof AppEngSlot aeSlot
                && aeSlot.getInventory() instanceof ConfigMenuInventory wrapper
                && wrapper.isItemValid(aeSlot.getContainerSlot(), stack);
    }

    /**
     * 创造模式中键复制：复制一个常规堆叠到光标。
     */
    @Unique
    private void ae2pr$clone(AEBaseMenu menu, Slot slot, GenericStackInv inv, Player player) {
        if (!player.getAbilities().instabuild || !menu.getCarried().isEmpty()) {
            return;
        }
        var stack = inv.getStack(((AppEngSlot) slot).getContainerSlot());
        if (stack == null || !(stack.what() instanceof AEItemKey key)) {
            return;
        }
        menu.setCarried(key.toStack(key.getMaxStackSize()));
    }

    /**
     * 数字键交换：把快捷栏物品换进槽位。放不下或存量超过一组时整体不交换。
     */
    @Unique
    private void ae2pr$swapWithHotbar(AEBaseMenu menu, Slot slot, GenericStackInv inv, int hotbarSlot,
            Player player) {
        if (hotbarSlot < 0 || hotbarSlot >= 9) {
            return;
        }
        int invSlot = ((AppEngSlot) slot).getContainerSlot();
        var existing = inv.getStack(invSlot);
        var hotbar = player.getInventory().getItem(hotbarSlot);
        if (existing == null && hotbar.isEmpty()) {
            return;
        }
        // 槽内超量存量无法完整放进快捷栏，拒绝交换
        if (existing != null && !(existing.what() instanceof AEItemKey existingKey
                && existing.amount() <= existingKey.getMaxStackSize())) {
            return;
        }
        if (!hotbar.isEmpty()) {
            var key = AEItemKey.of(hotbar);
            if (key == null || !ae2pr$accepts(slot, hotbar)) {
                return;
            }
            // 放不下就整体不交换，避免物品被吞
            if (inv.insert(invSlot, key, hotbar.getCount(), Actionable.SIMULATE) < hotbar.getCount()) {
                return;
            }
            inv.setStack(invSlot, null);
            hotbar.shrink((int) inv.insert(invSlot, key, hotbar.getCount(), Actionable.MODULATE));
        } else {
            inv.setStack(invSlot, null);
        }
        var intoHotbar = existing != null ? ((AEItemKey) existing.what()).toStack((int) existing.amount()) : hotbar;
        player.getInventory().setItem(hotbarSlot, intoHotbar);
        menu.broadcastChanges();
    }

    /**
     * 丢弃键：只丢出一个常规堆叠，超量存量留在槽内。
     */
    @Unique
    private void ae2pr$throwStack(AEBaseMenu menu, Slot slot, GenericStackInv inv, int button, Player player) {
        int invSlot = ((AppEngSlot) slot).getContainerSlot();
        var stack = inv.getStack(invSlot);
        if (stack == null || !(stack.what() instanceof AEItemKey key)) {
            return;
        }
        int limit = key.getMaxStackSize();
        long drop = button == 0 ? Math.min(1, stack.amount()) : Math.min(limit, stack.amount());
        if (drop <= 0) {
            return;
        }
        long remaining = stack.amount() - drop;
        inv.setStack(invSlot, remaining > 0 ? new GenericStack(key, remaining) : null);
        player.drop(key.toStack((int) drop), true);
        menu.broadcastChanges();
    }

    @Unique
    private void ae2pr$pickup(AEBaseMenu menu, Slot slot, GenericStackInv inv, boolean single) {
        int invSlot = ((AppEngSlot) slot).getContainerSlot();
        var carried = menu.getCarried();
        if (!carried.isEmpty()) {
            if (GenericStack.isWrapped(carried) || !ae2pr$accepts(slot, carried)) {
                return;
            }
            var key = AEItemKey.of(carried);
            if (key == null) {
                return;
            }
            // 不同 key 不交换：insert 只接受同 key 或空槽，返回 0 时保持手上物品不变
            long inserted = inv.insert(invSlot, key, single ? 1 : carried.getCount(), Actionable.MODULATE);
            if (inserted <= 0) {
                return;
            }
            carried.shrink((int) inserted);
            menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            menu.broadcastChanges();
            return;
        }
        var stack = inv.getStack(invSlot);
        if (stack == null || !(stack.what() instanceof AEItemKey key)) {
            return;
        }
        // 左键取一个正常堆叠，右键取半组（均不超物品上限）
        int limit = key.getMaxStackSize();
        long take = single ? Math.min((stack.amount() + 1) / 2, limit) : Math.min(stack.amount(), limit);
        if (take <= 0) {
            return;
        }
        long remaining = stack.amount() - take;
        inv.setStack(invSlot, remaining > 0 ? new GenericStack(key, remaining) : null);
        menu.setCarried(key.toStack((int) take));
        menu.broadcastChanges();
    }

    /**
     * 返回槽 -> 玩家背包：只搬一个普通堆叠，按实际放入量扣减槽内缓存，背包满时保持不变。
     */
    @Unique
    private void ae2pr$slotToPlayer(AEBaseMenu menu, Slot slot, GenericStackInv inv,
            CallbackInfoReturnable<ItemStack> cir) {
        int invSlot = ((AppEngSlot) slot).getContainerSlot();
        var stack = inv.getStack(invSlot);
        if (stack == null || !(stack.what() instanceof AEItemKey key)) {
            // 流体等非物品内容保持原版搬运
            return;
        }
        var template = key.toStack((int) Math.min(stack.amount(), key.getMaxStackSize()));
        // 与原版一致：先合并已有同类堆叠，再占用空槽
        int moved = ae2pr$mergeIntoPlayer(menu, template);
        moved += ae2pr$fillPlayerEmpty(menu, template);
        if (moved > 0) {
            long remaining = stack.amount() - moved;
            inv.setStack(invSlot, remaining > 0 ? new GenericStack(key, remaining) : null);
            menu.broadcastChanges();
        }
        cir.setReturnValue(ItemStack.EMPTY);
    }

    @Unique
    private int ae2pr$mergeIntoPlayer(AEBaseMenu menu, ItemStack template) {
        int moved = 0;
        for (Slot dest : menu.slots) {
            if (template.isEmpty()) {
                break;
            }
            if (!ae2pr$isPlayerDestination(dest)) {
                continue;
            }
            var current = dest.getItem();
            if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, template) || !dest.mayPlace(template)) {
                continue;
            }
            int limit = Math.min(dest.getMaxStackSize(), template.getMaxStackSize());
            int move = Math.min(limit - current.getCount(), template.getCount());
            if (move <= 0) {
                continue;
            }
            var placed = template.copy();
            placed.setCount(current.getCount() + move);
            dest.set(placed);
            dest.setChanged();
            template.shrink(move);
            moved += move;
        }
        return moved;
    }

    @Unique
    private int ae2pr$fillPlayerEmpty(AEBaseMenu menu, ItemStack template) {
        int moved = 0;
        for (Slot dest : menu.slots) {
            if (template.isEmpty()) {
                break;
            }
            if (!ae2pr$isPlayerDestination(dest) || !dest.getItem().isEmpty() || !dest.mayPlace(template)) {
                continue;
            }
            int limit = Math.min(dest.getMaxStackSize(), template.getMaxStackSize());
            int move = Math.min(limit, template.getCount());
            if (move <= 0) {
                continue;
            }
            var placed = template.copy();
            placed.setCount(move);
            dest.set(placed);
            dest.setChanged();
            template.shrink(move);
            moved += move;
        }
        return moved;
    }

    @Unique
    private int ae2pr$mergeIntoMenu(AEBaseMenu menu, ItemStack stack, AEItemKey key, int remaining,
            boolean allowStorage) {
        for (Slot dest : menu.slots) {
            if (remaining <= 0) {
                break;
            }
            if (!ae2pr$isMenuDestination(dest)) {
                continue;
            }
            if (!allowStorage && ae2pr$isStorageSlot(dest)) {
                continue;
            }
            var inv = ae2pr$managedInv(dest);
            if (inv != null) {
                int invSlot = ((AppEngSlot) dest).getContainerSlot();
                if (!key.equals(inv.getKey(invSlot))) {
                    continue;
                }
                // 直接写真实库存，绕过包装层按物品上限做的放置判定
                remaining -= (int) inv.insert(invSlot, key, remaining, Actionable.MODULATE);
                continue;
            }
            var current = dest.getItem();
            if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, stack) || !dest.mayPlace(stack)) {
                continue;
            }
            int limit = Math.min(dest.getMaxStackSize(), stack.getMaxStackSize());
            int move = Math.min(limit - current.getCount(), remaining);
            if (move <= 0) {
                continue;
            }
            var merged = stack.copy();
            merged.setCount(current.getCount() + move);
            dest.set(merged);
            dest.setChanged();
            remaining -= move;
        }
        return remaining;
    }

    @Unique
    private int ae2pr$fillMenuEmpty(AEBaseMenu menu, ItemStack stack, AEItemKey key, int remaining,
            boolean storagePass) {
        for (Slot dest : menu.slots) {
            if (remaining <= 0) {
                break;
            }
            if (!ae2pr$isMenuDestination(dest) || !dest.getItem().isEmpty()) {
                continue;
            }
            if (ae2pr$isStorageSlot(dest) != storagePass) {
                continue;
            }
            var inv = ae2pr$managedInv(dest);
            if (inv != null) {
                if (!ae2pr$accepts(dest, stack)) {
                    continue;
                }
                // 直接写真实库存，绕过包装层按物品上限做的放置判定
                remaining -= (int) inv.insert(((AppEngSlot) dest).getContainerSlot(), key, remaining,
                        Actionable.MODULATE);
                continue;
            }
            if (!dest.mayPlace(stack)) {
                continue;
            }
            int limit = Math.min(dest.getMaxStackSize(), stack.getMaxStackSize());
            int move = Math.min(limit, remaining);
            if (move <= 0) {
                continue;
            }
            var placed = stack.copy();
            placed.setCount(move);
            dest.set(placed);
            dest.setChanged();
            remaining -= move;
        }
        return remaining;
    }

    @Unique
    private boolean ae2pr$isPlayerDestination(Slot slot) {
        return slot.isActive() && isPlayerSideSlot(slot) && !(slot instanceof DisabledSlot)
                && !(slot instanceof InaccessibleSlot) && !(slot instanceof FakeSlot)
                && !(slot instanceof CraftingMatrixSlot);
    }

    @Unique
    private boolean ae2pr$isMenuDestination(Slot slot) {
        return slot.isActive() && !isPlayerSideSlot(slot) && !(slot instanceof FakeSlot)
                && !(slot instanceof CraftingMatrixSlot) && !(slot instanceof DisabledSlot)
                && !(slot instanceof InaccessibleSlot);
    }

    @Unique
    private boolean ae2pr$hasManagedTarget(AEBaseMenu menu, AEItemKey key) {
        for (Slot dest : menu.slots) {
            if (isPlayerSideSlot(dest)) {
                continue;
            }
            var inv = ae2pr$managedInv(dest);
            if (inv == null) {
                continue;
            }
            var existing = inv.getKey(((AppEngSlot) dest).getContainerSlot());
            if (existing == null || key.equals(existing)) {
                return true;
            }
        }
        return false;
    }
}
