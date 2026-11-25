package com.murthinext.ae2pr.api.machine;

import net.minecraft.network.FriendlyByteBuf;

public record MachineStats(
        MachineStat speed,
        MachineStat energy,
        MachineStat parallel,
        double energyBase,
        boolean recipeBasis,
        boolean recipeEnergy) {
    public void write(FriendlyByteBuf buffer) {
        speed.write(buffer);
        energy.write(buffer);
        parallel.write(buffer);
        buffer.writeDouble(energyBase);
        buffer.writeBoolean(recipeBasis);
        buffer.writeBoolean(recipeEnergy);
    }

    public static MachineStats read(FriendlyByteBuf buffer) {
        return new MachineStats(
                MachineStat.read(buffer),
                MachineStat.read(buffer),
                MachineStat.read(buffer),
                buffer.readDouble(),
                buffer.readBoolean(),
                buffer.readBoolean());
    }
}
