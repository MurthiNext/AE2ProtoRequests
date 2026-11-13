package com.murthinext.ae2pr.api.machine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record MachineStats(MachineStat speed, MachineStat energy, MachineStat parallel, double energyBase,
                           Component energyContext, boolean recipeEnergy) {
    public void write(FriendlyByteBuf buffer) {
        speed.write(buffer);
        energy.write(buffer);
        parallel.write(buffer);
        buffer.writeDouble(energyBase);
        buffer.writeComponent(energyContext);
        buffer.writeBoolean(recipeEnergy);
    }

    public static MachineStats read(FriendlyByteBuf buffer) {
        return new MachineStats(MachineStat.read(buffer), MachineStat.read(buffer), MachineStat.read(buffer),
                                buffer.readDouble(), buffer.readComponent(), buffer.readBoolean());
    }
}
