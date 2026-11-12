package com.murthinext.ae2pr.requester.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import com.murthinext.ae2pr.ServerConfig;
import com.murthinext.ae2pr.requester.network.DragAndDropPacket;
import com.murthinext.ae2pr.requester.network.RequestUpdatePacket;
import com.murthinext.ae2pr.requester.network.RequesterNetwork;
import com.murthinext.ae2pr.requester.network.RequesterSyncPacket;

public final class RequesterPlatform {

    private RequesterPlatform() {
    }

    // 单个请求器可标记的物品数量上限（可配置）
    public static int getRequestLimit() {
        return ServerConfig.requesterSlots();
    }

    public static double getIdleEnergy() {
        return ServerConfig.requesterIdleEnergy();
    }

    public static boolean requireChannel() {
        return ServerConfig.requesterRequireChannel();
    }

    public static void sendRequestState(long requesterId, int requestIndex, boolean state) {
        RequesterNetwork.CHANNEL.sendToServer(new RequestUpdatePacket(requesterId, requestIndex, state));
    }

    public static void sendRequestAmount(long requesterId, int requestIndex, long amount) {
        RequesterNetwork.CHANNEL.sendToServer(new RequestUpdatePacket(requesterId, requestIndex, amount));
    }

    public static void sendDragAndDrop(long requesterId, int requestIndex, ItemStack item) {
        RequesterNetwork.CHANNEL.sendToServer(new DragAndDropPacket(requesterId, requestIndex, item));
    }

    public static void sendClearData(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            RequesterNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    RequesterSyncPacket.clearData());
        }
    }

    public static void sendInventoryData(Player player, long requesterId, CompoundTag data) {
        if (player instanceof ServerPlayer serverPlayer) {
            RequesterNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    RequesterSyncPacket.inventory(requesterId, data));
        }
    }
}
