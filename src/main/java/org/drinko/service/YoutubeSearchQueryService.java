package org.drinko.service;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class YoutubeSearchQueryService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final ConcurrentMap<Snowflake, Map<String, SearchQueryEntry>> searchQueryEntryMap = new ConcurrentHashMap<>();

    public void addSearchResults(final Snowflake guildId, List<AudioTrack> audioTracks) {
        if(!searchQueryEntryMap.containsKey(guildId)) {
            searchQueryEntryMap.put(guildId, new ConcurrentHashMap<>());
        }
        Map<String, SearchQueryEntry> guildAudioTracks = searchQueryEntryMap.get(guildId);
        for (AudioTrack audioTrack : audioTracks) {
            if(guildAudioTracks.containsKey(audioTrack.getIdentifier())) {
                guildAudioTracks.get(audioTrack.getIdentifier()).updateLastQueried();
            }
            guildAudioTracks.computeIfAbsent(audioTrack.getIdentifier(), (ignore) -> new SearchQueryEntry(audioTrack));
        }
    }

    public AudioTrack getAudioTrackClone(Snowflake guildId, String identifier) {
        Map<String, SearchQueryEntry> guildAudioTracks = searchQueryEntryMap.get(guildId);
        if(guildAudioTracks == null || guildAudioTracks.isEmpty()) {
            return null;
        }
        if(guildAudioTracks.containsKey(identifier)) {
            return guildAudioTracks.get(identifier).getAudioTrack().makeClone();
        }
        return null;
    }

    public void removeAllStaleEntries(Snowflake guildId, int olderThan, ChronoUnit unit) {
        final Instant now = Instant.now();
        int removeCount = 0;
        Map<String, SearchQueryEntry> audioTrackMap = this.searchQueryEntryMap.get(guildId);
        if(audioTrackMap == null || audioTrackMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, SearchQueryEntry> stringSearchQueryEntryEntry : audioTrackMap.entrySet()) {
            if(Duration.between(stringSearchQueryEntryEntry.getValue().getLastQueried(), now).get(unit) >= olderThan) {
                this.searchQueryEntryMap.remove(stringSearchQueryEntryEntry.getKey());
            }
        }
        if(removeCount > 0) {
            LOGGER.info("Removed " + removeCount + " stale entries");
        }
    }

    @Getter
    private class SearchQueryEntry {
        private Instant lastQueried;
        private final AudioTrack audioTrack;
        public SearchQueryEntry(final AudioTrack audioTrack) {
            this.audioTrack = audioTrack;
            this.lastQueried = Instant.now();
        }

        public void updateLastQueried() {
            this.lastQueried = Instant.now();
        }
    }
}
