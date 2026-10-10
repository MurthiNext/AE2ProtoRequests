/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.requester.network.DragAndDropPacket;
import com.murthinext.ae2pr.requester.network.RequestUpdatePacket;
import com.murthinext.ae2pr.requester.network.RequesterNetwork;
import com.murthinext.ae2pr.requester.network.RequesterSyncPacket;

/**
 * 红石请求器的平台/网络辅助方法。
 * <p>
 * 移植自 ME Requester（https://github.com/AlmostReliable/merequester，LGPL-3.0）的 {@code Platform}，
 * 改为使用本模组的网络通道与配置。
 */
public final class RequesterPlatform {

    private RequesterPlatform() {
    }

    /** 单个请求器可标记的物品数量上限（可配置）。 */
    public static int getRequestLimit() {
        return Config.requesterSlots();
    }

    public static double getIdleEnergy() {
        return Config.requesterIdleEnergy();
    }

    public static boolean requireChannel() {
        return Config.requesterRequireChannel();
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
