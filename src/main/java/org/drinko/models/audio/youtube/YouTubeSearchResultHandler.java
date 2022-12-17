package org.drinko.models.audio.youtube;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collections;

@RequiredArgsConstructor
public class YouTubeSearchResultHandler implements AudioLoadResultHandler {
    private final int MAX_SEARCH_LIST = 5;
    private final Sinks.One<YoutubeSearchResult> loadResult = Sinks.one();

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        loadResult.tryEmitValue(new YoutubeSearchResult(YoutubeSearchResult.YoutubeSearchResultState.LOADED, Collections.singletonList(audioTrack)));
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        if(!audioPlaylist.isSearchResult()) {
           throw new IllegalArgumentException("This Handler only supports search results for Youtube");
        }
        if(audioPlaylist.getTracks().size() > MAX_SEARCH_LIST) {
            loadResult.tryEmitValue(new YoutubeSearchResult(YoutubeSearchResult.YoutubeSearchResultState.LOADED, audioPlaylist.getTracks().subList(0, MAX_SEARCH_LIST)));
        }
        loadResult.tryEmitValue(new YoutubeSearchResult(YoutubeSearchResult.YoutubeSearchResultState.LOADED, audioPlaylist.getTracks()));
    }

    @Override
    public void noMatches() {
        loadResult.tryEmitValue(new YoutubeSearchResult(YoutubeSearchResult.YoutubeSearchResultState.FAILED_NO_MATCH,null));
    }

    @Override
    public void loadFailed(FriendlyException e) {
        loadResult.tryEmitValue(new YoutubeSearchResult(YoutubeSearchResult.YoutubeSearchResultState.FAILED_LOADING, null));
    }

    public Mono<YoutubeSearchResult> loadResult() {
        return loadResult.asMono();
    }
}
