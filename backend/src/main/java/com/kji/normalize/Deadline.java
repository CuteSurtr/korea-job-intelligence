package com.kji.normalize;

import java.time.Instant;

public record Deadline(Instant closesAt, boolean openEnded) {

    public static Deadline unbounded() {
        return new Deadline(null, true);
    }

    public static Deadline at(Instant closesAt) {
        return new Deadline(closesAt, false);
    }
}
