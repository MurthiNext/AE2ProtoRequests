package com.murthinext.ae2pr.effect.screen_corruption.network;

import com.murthinext.ae2pr.api.ClientHooks;
import com.murthinext.ae2pr.effect.screen_corruption.ScreenCorruptionImpact;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ScreenCorruptionImpactPacket() {
    public static void encode(ScreenCorruptionImpactPacket packet, FriendlyByteBuf buffer) {}

    public static ScreenCorruptionImpactPacket decode(FriendlyByteBuf buffer) {
        return new ScreenCorruptionImpactPacket();
    }

    public static void handle(ScreenCorruptionImpactPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> ClientHooks.run(() -> ScreenCorruptionImpact::trigger));
        context.setPacketHandled(true);
    }
}
