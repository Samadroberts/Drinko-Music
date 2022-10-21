package org.drinko.models.audio.trackscheduler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RepeatMode {
    ALL("\uD83D\uDD01"),
    ONE("\uD83D\uDD02"),
    NONE("none")
    ;

    private final String emoji;
}
