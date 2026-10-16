package com.murthinext.ae2pr.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -> 客户端：同步水晶反应仓的当前产物（物品与流体）。
 */
public class CrystalReactionChamberJobPacket {

    private final BlockPos pos;
    private final ItemStack item;
    private final FluidStack fluid;

    public CrystalReactionChamberJobPacket(BlockPos pos, ItemStack item, FluidStack fluid) {
        this.pos = pos;
        this.item = item.copy();
        this.fluid = fluid.copy();
    }

    public static void encode(CrystalReactionChamberJobPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeItem(packet.item);
        buffer.writeFluidStack(packet.fluid);
    }

    public static CrystalReactionChamberJobPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        ItemStack item = buffer.readItem();
        FluidStack fluid = buffer.readFluidStack();
        return new CrystalReactionChamberJobPacket(pos, item, fluid);
    }

    public static void handle(CrystalReactionChamberJobPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.murthinext.ae2pr.client.gui.crystal_reaction_chamber.CrystalReactionChamberScreen
                        .applyJobSync(packet.pos, packet.item, packet.fluid)));
        context.setPacketHandled(true);
    }
}
