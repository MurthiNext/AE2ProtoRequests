package com.murthinext.ae2pr.api.machine.part;

public record ChannelStatus(int required, int allocated, int capacity) {

    public static final ChannelStatus NONE = new ChannelStatus(0, 0, 0);
}
