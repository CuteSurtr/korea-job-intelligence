package com.kji.job;

public enum LifecycleState {
    DISCOVERED,
    ACTIVE,
    UNVERIFIED,
    STALE,
    CLOSED,
    REOPENED;

    public boolean isOpen() {
        return this != CLOSED;
    }
}
