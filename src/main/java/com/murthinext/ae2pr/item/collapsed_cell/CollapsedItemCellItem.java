package com.murthinext.ae2pr.item.collapsed_cell;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.murthinext.ae2pr.network.ScreenCorruptionImpactPacket;
import com.murthinext.ae2pr.registry.ModEffects;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModNetwork;

import appeng.api.stacks.GenericStack;
import appeng.core.localization.PlayerMessages;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;
import appeng.util.InteractionUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class CollapsedItemCellItem extends AEBaseItem {

    private static final String CONTENT_TAG = "content";
    private static final String DISASSEMBLY_TAG = "disassembly";
    private static final int DISASSEMBLY_TIMEOUT = 20 * 10;
    private static final float EXPLOSION_POWER = 1.5F;
    private static final int TOOLTIP_LINES = 6;

    public CollapsedItemCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var result = tryDisassemble(player.getItemInHand(hand), level, player, hand);
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return tryDisassemble(stack, context.getLevel(), context.getPlayer(), context.getHand());
    }

    private InteractionResult tryDisassemble(ItemStack stack, Level level, @Nullable Player player,
            InteractionHand hand) {
        if (player == null || hand != InteractionHand.MAIN_HAND || !InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (readContents(stack).stream().anyMatch(entry -> entry.amount() > 0)) {
            player.displayClientMessage(PlayerMessages.OnlyEmptyCellsCanBeDisassembled.text(), true);
            return InteractionResult.CONSUME;
        }

        long now = level.getServer().overworld().getGameTime();
        int attempt = getDisassemblyAttempts(stack, now) + 1;
        if (attempt < 4) {
            var progress = stack.getOrCreateTagElement(DISASSEMBLY_TAG);
            progress.putInt("attempts", attempt);
            progress.putLong("lastAttempt", now);
            player.displayClientMessage(Component.translatable("message.ae2pr.collapsed_item_cell.disassemble." + attempt),
                    false);
        } else {
            stack.shrink(1);
            var effect = ModEffects.SCREEN_CORRUPTION.get();
            var previous = player.getEffect(effect);
            int amplifier = previous == null ? 0 : Math.min(Byte.MAX_VALUE, previous.getAmplifier() + 1);
            player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier,
                    false, false, true));
            if (player instanceof ServerPlayer serverPlayer) {
                ModNetwork.sendToPlayer(serverPlayer, new ScreenCorruptionImpactPacket());
            }
            level.explode(null, player.getX(), player.getY() + 0.5, player.getZ(), EXPLOSION_POWER,
                    Level.ExplosionInteraction.TNT);
        }
        return InteractionResult.CONSUME;
    }

    private static int getDisassemblyAttempts(ItemStack stack, long now) {
        var progress = stack.getTagElement(DISASSEMBLY_TAG);
        if (progress == null) {
            return 0;
        }
        long elapsed = now - progress.getLong("lastAttempt");
        return elapsed < 0 || elapsed >= DISASSEMBLY_TIMEOUT ? 0 : Math.max(0, Math.min(3, progress.getInt("attempts")));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && stack.getTagElement(DISASSEMBLY_TAG) != null
                && getDisassemblyAttempts(stack, level.getServer().overworld().getGameTime()) == 0) {
            stack.removeTagKey(DISASSEMBLY_TAG);
        }
    }

    public static ItemStack create(List<GenericStack> contents) {
        var cell = new ItemStack(ModItems.COLLAPSED_ITEM_CELL.get());
        writeContents(cell, contents);
        return cell;
    }

    public static List<GenericStack> readContents(ItemStack cell) {
        var tag = cell.getTag();
        if (tag == null || !tag.contains(CONTENT_TAG, Tag.TAG_LIST)) {
            return List.of();
        }
        var entries = tag.getList(CONTENT_TAG, Tag.TAG_COMPOUND);
        var contents = new ArrayList<GenericStack>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            var stack = GenericStack.readTag(entries.getCompound(i));
            if (stack != null) {
                contents.add(stack);
            }
        }
        return contents;
    }

    public static void writeContents(ItemStack cell, List<GenericStack> contents) {
        if (contents.isEmpty()) {
            cell.removeTagKey(CONTENT_TAG);
            return;
        }
        var entries = new ListTag();
        for (var stack : contents) {
            entries.add(GenericStack.writeTag(stack));
        }
        cell.getOrCreateTag().put(CONTENT_TAG, entries);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        var contents = readContents(stack);
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        var sorted = new ArrayList<>(contents);
        sorted.sort((left, right) -> Long.compare(right.amount(), left.amount()));
        boolean hasMore = sorted.size() > TOOLTIP_LINES;
        return Optional.of(new StorageCellTooltipComponent(List.of(),
                hasMore ? new ArrayList<>(sorted.subList(0, TOOLTIP_LINES)) : sorted, hasMore, true));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ae2pr.collapsed_item_cell.desc"));
    }
}
