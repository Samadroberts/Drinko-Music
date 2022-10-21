package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class SchedulingTrackResult {
    private final AudioTrackInfo audioTrackInfo;
    private final TrackQueuedState queueResult;
}
