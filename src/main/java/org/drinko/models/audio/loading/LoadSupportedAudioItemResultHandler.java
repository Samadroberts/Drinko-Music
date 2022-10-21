package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

public class LoadSupportedAudioItemResultHandler implements AudioLoadResultHandler {
    private final Sinks.One<LoadSupportedAudioItemResult> loadResult = Sinks.one();

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        loadResult.tryEmitValue(new LoadSupportedAudioItemResult(LoadResult.SINGLE_TRACK_LOADED, audioTrack));
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        loadResult.tryEmitValue(new LoadSupportedAudioItemResult(LoadResult.PLAYLIST_LOADED, audioPlaylist));
    }

    @Override
    public void noMatches() {
        loadResult.tryEmitValue(new LoadSupportedAudioItemResult(LoadResult.NOT_FOUND, null));
    }

    @Override
    public void loadFailed(FriendlyException e) {
        loadResult.tryEmitValue(new LoadSupportedAudioItemResult(LoadResult.LOAD_FAILED, null));
    }

    public Mono<LoadSupportedAudioItemResult> getLoadResult() {
        return loadResult.asMono();
    }

    @RequiredArgsConstructor
    @Getter
    public enum LoadResult {
        SINGLE_TRACK_LOADED(AudioTrack.class),
        PLAYLIST_LOADED(AudioPlaylist.class),
        NOT_FOUND(null),
        LOAD_FAILED(null);

        private final Class<? extends AudioItem> supportedClass;
    }
}
