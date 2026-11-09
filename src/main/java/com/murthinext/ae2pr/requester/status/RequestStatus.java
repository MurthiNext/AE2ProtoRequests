/*
 * SPDX-License-Identifier: LGPL-3.0-only
 * Derived from ME Requester (https://github.com/AlmostReliable/merequester),
 * Copyright (c) AlmostReliable, licensed under LGPL-3.0.
 * See licenses/me-requester in this repository.
 */
package com.murthinext.ae2pr.requester.status;

public enum RequestStatus {
    IDLE, MISSING, REQUEST, PLAN, LINK, EXPORT;

    public boolean locksRequest() {
        return this == LINK || this == EXPORT;
    }
}
