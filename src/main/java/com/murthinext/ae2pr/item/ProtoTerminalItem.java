package com.murthinext.ae2pr.item;

import com.murthinext.ae2pr.client.proto_terminal.ProtoTerminalScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** 源始终端的手持交互入口。 */
public class ProtoTerminalItem extends Item {

    public ProtoTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ProtoTerminalScreen::open);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ProtoTerminalScreen::open);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
