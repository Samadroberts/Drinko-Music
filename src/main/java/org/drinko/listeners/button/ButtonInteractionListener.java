package org.drinko.listeners.button;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import org.drinko.models.audio.GuildVoiceSupport;
import org.drinko.models.audio.trackscheduler.TrackQueueResult;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.drinko.service.YoutubeSearchQueryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.drinko.util.DrinkoEmbedSpecUtils.getFailedToLoadSongEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongLoadedEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongQueueEmbed;

@Component
public class ButtonInteractionListener {
    private final GuildVoiceService guildVoiceService;

    private final YoutubeSearchQueryService youtubeSearchQueryService;

    public ButtonInteractionListener(GatewayDiscordClient client, GuildVoiceService guildVoiceService, YoutubeSearchQueryService youtubeSearchQueryService) {
        this.guildVoiceService = guildVoiceService;
        this.youtubeSearchQueryService = youtubeSearchQueryService;
        client.on(ButtonInteractionEvent.class, this::handle).subscribe();
    }

    private Mono<Void> handle(ButtonInteractionEvent event) {
        Mono<Snowflake> guildIdMono = event.getInteraction().getGuild()
                .map(Guild::getId);


        Mono<TrackScheduler> schedulerMono = guildIdMono.map((guildId) -> guildVoiceService.getGuildVoiceSupport(guildId))
                .map(GuildVoiceSupport::getTrackScheduler);

        Mono<AudioTrack> toPlay = guildIdMono.mapNotNull((guildId) -> youtubeSearchQueryService.getAudioTrackForCustomIdAndGuildId(guildId, event.getCustomId()));

        return Mono.zip(toPlay, schedulerMono).flatMap((tuple) -> {
            final AudioTrack audioTrack = tuple.getT1();
            if (audioTrack == null) {
                return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                        .addEmbed(getFailedToLoadSongEmbed(audioTrack.getInfo().title))
                        .ephemeral(true)
                        .build()).then();
            }
            final TrackScheduler scheduler = tuple.getT2();
            TrackQueueResult result = scheduler.queue(audioTrack);

            switch (result) {
                case QUEUED:
                    return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(getSongQueueEmbed(audioTrack.getInfo().title))
                            .build()).then();
                case PLAYING_NOW:
                    return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(getSongLoadedEmbed(audioTrack.getInfo().title))
                            .build()).then();
                case FAILED:
                default:
                    return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .addEmbed(getFailedToLoadSongEmbed(audioTrack.getInfo().title))
                            .ephemeral(true)
                            .build()).then();
            }
        });
    }
}
