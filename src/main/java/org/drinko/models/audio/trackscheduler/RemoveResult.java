package org.drinko.models.audio.trackscheduler;

import lombok.Getter;

@Getter
public class RemoveResult {
    private final State state;

    private final String message;

    private RemoveResult(final State state, String message) {
        this.state = state;
        this.message = message;
    }

    public static RemoveResult success() {
        return new RemoveResult(State.SUCCESS, "");
    }

    public static RemoveResult invalid(String message) {
        return new RemoveResult(State.INVALID, message);
    }

    public static RemoveResult empty(String message) {
        return new RemoveResult(State.EMPTY, message);
    }


    public enum State {
        SUCCESS,
        INVALID,
        EMPTY,
    }
}
