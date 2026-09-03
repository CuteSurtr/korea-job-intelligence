package com.kji.crm;

import java.util.Set;

public enum ApplicationStatus {
    NOT_REVIEWED,
    INTERESTED,
    READY_TO_APPLY,
    APPLIED,
    INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN,
    IGNORED;

    private static final Set<ApplicationStatus> TERMINAL =
            Set.of(REJECTED, WITHDRAWN, IGNORED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    public boolean isActive() {
        return !isTerminal() && this != NOT_REVIEWED;
    }
}
