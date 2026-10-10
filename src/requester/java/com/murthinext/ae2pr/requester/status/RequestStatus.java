/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/LGPL-3.0.txt and licenses/ME-Requester-NOTICE.txt in this repository.
 */
package com.murthinext.ae2pr.requester.status;

public enum RequestStatus {
    IDLE, MISSING, REQUEST, PLAN, LINK, EXPORT;

    public RequestStatus translateToClient() {
        if (this == REQUEST || this == PLAN) return IDLE;
        return this;
    }

    public boolean locksRequest() {
        return this == LINK || this == EXPORT;
    }
}
