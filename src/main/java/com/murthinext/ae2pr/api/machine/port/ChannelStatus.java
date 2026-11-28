package com.murthinext.ae2pr.api.machine.port;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record ChannelStatus(int required, int allocated, int capacity, Supply supply) {

    public static final ChannelStatus NONE = new ChannelStatus(0, 0, 0, Supply.HOST_MISSING);

    public enum Supply {
        HOST_MISSING("gui.ae2pr.multiblock.channels.host_missing", ChatFormatting.RED),
        INSUFFICIENT("gui.ae2pr.multiblock.channels.insufficient", ChatFormatting.GOLD),
        NORMAL("gui.ae2pr.multiblock.channels.normal", ChatFormatting.GREEN);

        private static final Supply[] VALUES = values();

        private final String translationKey;
        private final ChatFormatting color;

        Supply(String translationKey, ChatFormatting color) {
            this.translationKey = translationKey;
            this.color = color;
        }

        public static Supply byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : HOST_MISSING;
        }

        public MutableComponent text() {
            return Component.translatable(translationKey);
        }

        public Component coloredText() {
            return text().withStyle(color);
        }
    }
}
