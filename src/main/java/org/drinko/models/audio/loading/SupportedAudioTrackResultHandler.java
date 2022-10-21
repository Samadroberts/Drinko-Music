package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedAudioTrackResultHandler implements SupportedAudioItemResultHandler<AudioTrack> {

    @Override
    public SchedulingTrackResult handle(TrackScheduler trackScheduler, AudioTrack audioTrack) {
        switch (trackScheduler.queue(audioTrack)) {
            case QUEUED:
                return new SchedulingTrackResult(audioTrack.getInfo(), TrackQueuedState.QUEUED);
            case PLAYING_NOW:
                return new SchedulingTrackResult(audioTrack.getInfo(), TrackQueuedState.PLAYING_NOW);
        }
        return new SchedulingTrackResult(null, TrackQueuedState.FAILED);
    }
}
