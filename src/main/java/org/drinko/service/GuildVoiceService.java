package org.drinko.service;

import discord4j.common.util.Snowflake;
import lombok.RequiredArgsConstructor;
import org.drinko.config.youtube.YoutubeAccountCredentials;
import org.drinko.models.audio.GuildVoiceSupport;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GuildVoiceService {
    private final YoutubeAccountCredentials credentials;
    private Map<Snowflake, GuildVoiceSupport> voiceGuildMap = new ConcurrentHashMap<>();

    public GuildVoiceSupport getGuildVoiceSupport(Snowflake guildId) {
        if (voiceGuildMap.containsKey(guildId)) {
            return voiceGuildMap.get(guildId);
        }
        GuildVoiceSupport guildVoiceSupport = voiceGuildMap.computeIfAbsent(guildId, (key) -> new GuildVoiceSupport(guildId, credentials));
        return guildVoiceSupport;
    }

    public void removeGuildVoiceSupport(Snowflake guildId) {
        voiceGuildMap.remove(guildId);
    }
}
