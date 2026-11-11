package com.murthinext.ae2pr.requester.abstraction;

import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.Requests;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public final class RequestTracker {

    public static final String SORT_BY_ID = "sort_by";
    public static final String UNIQUE_NAME_ID = "unique_name";

    private final long id;
    private final long sortBy;
    private final String name;
    private final Requests server;
    private final Requests lastSynced;

    RequestTracker(RedstoneRequesterBlockEntity requester, long id) {
        this.id = id;
        this.sortBy = requester.getSortValue();
        this.name = requester.getTerminalName().getString();
        this.server = requester.getRequests();
        this.lastSynced = new Requests();
    }

    public CompoundTag createFullUpdate() {
        var tag = server.serialize();
        lastSynced.deserialize(tag);
        tag.putString(UNIQUE_NAME_ID, name);
        tag.putLong(SORT_BY_ID, sortBy);
        return tag;
    }

    @Nullable
    public CompoundTag createPartialUpdate() {
        CompoundTag tag = null;
        for (var i = 0; i < server.size(); i++) {
            var serverRequest = server.get(i);
            var syncedRequest = lastSynced.get(i);
            if (!serverRequest.isDifferent(syncedRequest)) {
                continue;
            }
            if (tag == null) {
                tag = new CompoundTag();
                tag.putString(UNIQUE_NAME_ID, name);
                tag.putLong(SORT_BY_ID, sortBy);
            }
            var requestData = serverRequest.serialize();
            tag.put(String.valueOf(i), requestData);
            syncedRequest.deserialize(requestData);
        }
        return tag;
    }

    public long getId() {
        return id;
    }

    public Requests getServer() {
        return server;
    }
}
