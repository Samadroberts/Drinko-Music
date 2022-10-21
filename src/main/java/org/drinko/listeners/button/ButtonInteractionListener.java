package org.drinko.listeners.button;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.GuildVoiceSupport;
import org.drinko.models.audio.trackscheduler.TrackQueueResult;
import org.drinko.service.GuildVoiceService;
import org.drinko.service.YoutubeSearchQueryService;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;

import static org.drinko.util.DrinkoEmbedSpecUtils.getFailedToLoadSongEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongLoadedEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongQueueEmbed;


@RequiredArgsConstructor
public class ButtonInteractionListener {
    private final int TIMEOUT_LENGTH = 30;
    private final ChronoUnit TIMEOUT_UNIT = ChronoUnit.MINUTES;
    private final YoutubeSearchQueryService queryService;
    private final GatewayDiscordClient client;

    private final Snowflake guildId;
    private final GuildVoiceService guildVoiceService;

    public Mono<Void> createButtonListener() {
        Mono<Void> tempListener = client.on(ButtonInteractionEvent.class, event -> {
                    AudioTrack queriedTrack = queryService.getAudioTrackClone(guildId, event.getCustomId());
                    if (queriedTrack != null) {

                        GuildVoiceSupport voiceSupport = guildVoiceService.getGuildVoiceSupport(guildId);
                        TrackQueueResult result = voiceSupport.getTrackScheduler().queue(queriedTrack);

                        switch (result) {
                            case QUEUED:
                                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(getSongQueueEmbed(queriedTrack.getInfo().title))
                                        .build()).then();
                            case PLAYING_NOW:
                                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(getSongLoadedEmbed(queriedTrack.getInfo().title))
                                        .build()).then();
                            case FAILED:
                            default:
                                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                        .addEmbed(getFailedToLoadSongEmbed(queriedTrack.getInfo().title))
                                        .ephemeral(true)
                                        .build()).then();
                        }
                    }
                    return Mono.empty();
                }).timeout(Duration.of(TIMEOUT_LENGTH, TIMEOUT_UNIT)) // Timeout after 30 minutes
                // Handle TimeoutException that will be thrown when the above times out
                .onErrorResume(TimeoutException.class, ignore -> {
                    queryService.removeAllStaleEntries(guildId, TIMEOUT_LENGTH, TIMEOUT_UNIT);
                    return Mono.empty();
                })
                .then(); //Transform the flux to a mono
        return tempListener;
    }
}
