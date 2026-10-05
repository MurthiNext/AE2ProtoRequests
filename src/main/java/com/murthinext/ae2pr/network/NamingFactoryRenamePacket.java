package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import com.murthinext.ae2pr.ModTags;
import com.murthinext.ae2pr.block.naming_factory.NamingFactoryBlockEntity;

/**
 * 客户端 -> 服务端：为名称压印工厂写入自定义名称（空串表示清除）。
 * <p>
 * 服务端校验玩家距离与手持的切割刀标签，避免伪造包修改远处机器。
 */
public record NamingFactoryRenamePacket(BlockPos pos, String name) {

    private static final int MAX_NAME_LENGTH = 64;

    public static void encode(NamingFactoryRenamePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.name, MAX_NAME_LENGTH);
    }

    public static NamingFactoryRenamePacket decode(FriendlyByteBuf buffer) {
        return new NamingFactoryRenamePacket(buffer.readBlockPos(), buffer.readUtf(MAX_NAME_LENGTH));
    }

    public static void handle(NamingFactoryRenamePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> apply(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void apply(NamingFactoryRenamePacket packet, @Nullable ServerPlayer player) {
        if (player == null || !holdsKnife(player)) {
            return;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(packet.pos)) > 64.0) {
            return;
        }
        if (player.level().getBlockEntity(packet.pos) instanceof NamingFactoryBlockEntity factory) {
            factory.setName(packet.name);
            factory.setChanged();
            factory.markForUpdate();
        }
    }

    private static boolean holdsKnife(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(ModTags.KNIVES)
                || player.getItemInHand(InteractionHand.OFF_HAND).is(ModTags.KNIVES);
    }
}
