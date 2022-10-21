package org.drinko.models.audio.youtube;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class YoutubeSearchResult {
    private final YoutubeSearchResult.YoutubeSearchResultState result;
    private final List<AudioTrack> searchResults;

    public enum YoutubeSearchResultState {
        LOADED,
        FAILED_NO_MATCH,
        FAILED_LOADING
    }
}
