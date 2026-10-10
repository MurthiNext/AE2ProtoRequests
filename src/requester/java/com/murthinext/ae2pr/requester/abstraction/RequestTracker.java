/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.abstraction;

import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.Requests;
import com.murthinext.ae2pr.requester.Requests.Request;

/**
 * Simplified representation of a {@link Request} and its parent {@link RedstoneRequesterBlockEntity}
 * for synchronization in menus.
 */
public final class RequestTracker {

    private final long id;
    private final long sortBy;
    private final String name;
    private final Requests server;
    private final Requests client;

    RequestTracker(RedstoneRequesterBlockEntity requester, long id) {
        this.id = id;
        this.sortBy = requester.getSortValue();
        this.name = requester.getTerminalName().getString();
        this.server = requester.getRequests();
        this.client = new Requests();
    }

    public long getId() {
        return id;
    }

    long getSortBy() {
        return sortBy;
    }

    public String getName() {
        return name;
    }

    public Requests getServer() {
        return server;
    }

    public Requests getClient() {
        return client;
    }
}
