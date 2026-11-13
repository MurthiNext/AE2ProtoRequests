package com.murthinext.ae2pr.api.machine;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public record MachineStat(double value, List<Source> sources) {
    public MachineStat {
        sources = List.copyOf(sources);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeDouble(value);
        buffer.writeCollection(sources, (buf, source) -> {
            buf.writeComponent(source.name());
            buf.writeDouble(source.before());
            buf.writeDouble(source.after());
        });
    }

    public static MachineStat read(FriendlyByteBuf buffer) {
        return new MachineStat(
                buffer.readDouble(),
                buffer.readList(buf -> new Source(buf.readComponent(), buf.readDouble(), buf.readDouble())));
    }

    public record Source(Component name, double before, double after) {}
}
