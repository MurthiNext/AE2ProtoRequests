package com.murthinext.ae2pr.requester.status;

public enum RequestStatus {
    IDLE, MISSING, REQUEST, PLAN, LINK, EXPORT;

    public boolean locksRequest() {
        return this == LINK || this == EXPORT;
    }
}
