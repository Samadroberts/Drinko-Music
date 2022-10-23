package org.drinko.service;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class YoutubeSearchQueryService {
    private final ConcurrentMap<GuildCustomIdKey, AudioTrack> youtubeIdGuildMap = new ConcurrentHashMap();

    public void addSearchResults(Snowflake guildId, Function<AudioTrack, String> audioTrackId, List<AudioTrack> trackList) {
        for (AudioTrack audioTrack : trackList) {
            final GuildCustomIdKey key = new GuildCustomIdKey(guildId, audioTrackId.apply(audioTrack));
            if (!youtubeIdGuildMap.containsKey(key)) {
                youtubeIdGuildMap.put(key, audioTrack);
            }
        }
    }

    public AudioTrack getAudioTrackForCustomIdAndGuildId(Snowflake guildId, String customId) {
        final GuildCustomIdKey key = new GuildCustomIdKey(guildId, customId);
        if (youtubeIdGuildMap.containsKey(key)) {
            return youtubeIdGuildMap.get(key).makeClone();
        }
        return null;
    }

    public void disconnectedFromGuild(Snowflake guildId) {
        final List<GuildCustomIdKey> keysToDelete = youtubeIdGuildMap.keySet().stream()
                .filter((guildCustomIdKey -> guildCustomIdKey.guildId == guildId))
                .collect(Collectors.toList());

        for (GuildCustomIdKey guildCustomIdKey : keysToDelete) {
            youtubeIdGuildMap.remove(guildCustomIdKey);
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    private final class GuildCustomIdKey {
        private final Snowflake guildId;
        private final String customId;
    }
}
