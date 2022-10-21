package org.drinko.models.audio.trackscheduler;

import lombok.Getter;

@Getter
public class SkipResult {
    private final State state;
    private final long numberSkipped;

    private final String message;

    private SkipResult(final State state, long numberSkipped, String message) {
        this.state = state;
        this.numberSkipped = numberSkipped;
        this.message = message;
    }

    public static SkipResult success(long numberSkipped) {
        return new SkipResult(State.SUCCESS, numberSkipped, "");
    }

    public static SkipResult invalid(String message) {
        return new SkipResult(State.INVALID, 0L, message);
    }

    public static SkipResult empty(String message) {
        return new SkipResult(State.EMPTY, 0L, message);
    }


    public enum State {
        SUCCESS,
        INVALID,
        EMPTY,
    }
}
