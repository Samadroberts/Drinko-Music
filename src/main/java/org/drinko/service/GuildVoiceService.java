package org.drinko.service;

import dev.lavalink.youtube.YoutubeSourceOptions;
import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.GuildVoiceSupport;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GuildVoiceService {
    private Map<Snowflake, GuildVoiceSupport> voiceGuildMap = new ConcurrentHashMap<>();
    private final YoutubeSourceOptions youtubeSourceOptions;

    public GuildVoiceSupport getGuildVoiceSupport(Snowflake guildId) {
        if (voiceGuildMap.containsKey(guildId)) {
            return voiceGuildMap.get(guildId);
        }
        GuildVoiceSupport guildVoiceSupport = voiceGuildMap.computeIfAbsent(guildId, (key) -> new GuildVoiceSupport(guildId, youtubeSourceOptions));
        return guildVoiceSupport;
    }

    public void removeGuildVoiceSupport(Snowflake guildId) {
        voiceGuildMap.remove(guildId);
    }
}
