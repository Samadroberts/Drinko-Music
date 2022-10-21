package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import org.drinko.models.audio.trackscheduler.TrackScheduler;

public interface SupportedAudioItemResultHandler<T extends AudioItem> {
        SchedulingTrackResult handle(TrackScheduler trackScheduler, T t);
    }
