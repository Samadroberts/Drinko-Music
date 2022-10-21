package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.TrackQueueResult;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SupportedAudioPlaylistResultHandler implements SupportedAudioItemResultHandler<AudioPlaylist> {

    @Override
    public SchedulingTrackResult handle(TrackScheduler trackScheduler, AudioPlaylist audioPlaylist) {
        TrackQueuedState result = TrackQueuedState.FAILED;
        AudioTrackInfo audioTrackInfo = null;
        for (AudioTrack track : audioPlaylist.getTracks()) {
            TrackQueueResult tempResult = trackScheduler.queue(track);
            if (result != TrackQueuedState.PLAYING_NOW || result != TrackQueuedState.QUEUED) {
                if (tempResult == TrackQueueResult.PLAYING_NOW) {
                    result = TrackQueuedState.PLAYING_NOW;
                } else {
                    result = TrackQueuedState.QUEUED;
                }
                audioTrackInfo = track.getInfo();
            }
        }
        return new SchedulingTrackResult(audioTrackInfo, result);
    }
}
