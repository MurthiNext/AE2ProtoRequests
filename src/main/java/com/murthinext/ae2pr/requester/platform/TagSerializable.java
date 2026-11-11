package com.murthinext.ae2pr.requester.platform;

import net.minecraft.nbt.Tag;

public interface TagSerializable<T extends Tag> {
    T serialize();

    void deserialize(T tag);
}
