package org.drinko.service;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.voice.VoiceConnection;
import lombok.RequiredArgsConstructor;
import org.drinko.util.DrinkoEmbedSpecUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class VoiceConnectionService {
    private final GuildVoiceService guildVoiceService;
    private final YoutubeSearchQueryService youtubeSearchQueryService;
    private final GatewayDiscordClient gatewayDiscordClient;

    private static final int DISCONNECT_PERIOD_SECONDS = 10;

    public Mono<VoiceConnection> getNewOrExistingConnection(VoiceChannel voiceChannel, Mono<MessageChannel> commandChannel) {
        return voiceChannel.getClient().getVoiceConnectionRegistry().getVoiceConnection(voiceChannel.getGuildId())
                .switchIfEmpty(setupNewConnection(voiceChannel, commandChannel));
    }

    private Mono<VoiceConnection> setupNewConnection(VoiceChannel voiceChannel, Mono<MessageChannel> commandChannel) {
        Mono<VoiceConnection> newConnection = voiceChannel.join(getJoinSpec(voiceChannel));
        return newConnection
                .doOnSuccess(voiceConnection -> {
                    handleDisconnectOnNewConnection(voiceConnection, commandChannel).subscribe();
                });
    }

    private VoiceChannelJoinSpec getJoinSpec(VoiceChannel voiceChannel) {
        return VoiceChannelJoinSpec.builder()
                .provider(guildVoiceService.getGuildVoiceSupport(voiceChannel.getGuildId()).getAudioProvider())
                .build();
    }

    private Mono<Void> handleDisconnectOnNewConnection(VoiceConnection voiceConnection, Mono<MessageChannel> commandChannel) {

        final Mono<Boolean> manuallyDisconnected = voiceConnection.stateEvents()
                .filter(state -> VoiceConnection.State.DISCONNECTED.equals(state))
                .next()
                .map((ignore) -> true);

        Mono<Boolean> isBotAloneInVoiceAfter10Seconds = gatewayDiscordClient.getEventDispatcher().on(VoiceStateUpdateEvent.class)
                .filter(voiceStateUpdateEvent -> voiceStateUpdateEvent.isMoveEvent() || voiceStateUpdateEvent.isLeaveEvent())
                .filterWhen(event -> isVoiceStateEventRelatedToExistingVoiceConnection(event, voiceConnection))
                .flatMap(ignore -> voiceConnection.getChannelId())
                .flatMap(channelId -> gatewayDiscordClient.getChannelById(channelId))
                .cast(VoiceChannel.class)
                .delayElements(Duration.of(DISCONNECT_PERIOD_SECONDS, ChronoUnit.SECONDS))
                .flatMap(voiceChannel -> getNumberOfBotUsersInVoiceChannel(voiceChannel))
                .map(nonBotUsersInVC -> nonBotUsersInVC == 0)
                .filter(isBotAlone -> isBotAlone)
                .next();

        return Mono.firstWithSignal(manuallyDisconnected, isBotAloneInVoiceAfter10Seconds)
                .doOnSuccess((ignore) -> handleDisconnect(voiceConnection.getGuildId()))
                .filterWhen((ignore) -> voiceConnection.isConnected())
                .flatMap((ignore) -> commandChannel.flatMap((messageChannel -> messageChannel.createMessage(getDisconnectMessage()))))
                .flatMap((ignore) -> voiceConnection.disconnect());
    }

    private Mono<Integer> getNumberOfBotUsersInVoiceChannel(VoiceChannel voiceChannel) {
        return voiceChannel.getVoiceStates()
                .flatMap(VoiceState::getUser)
                .reduce(0, (accum, user) -> accum + (user.isBot() ? 0 : 1));
    }

    private MessageCreateSpec getDisconnectMessage() {
        return MessageCreateSpec.builder()
                .addEmbed(
                        DrinkoEmbedSpecUtils.createDrinkoEmbedSpec()
                                .description("All users have left the voice channel.\n Disconnecting and clearing the queue because I was inactive for too long.")
                                .build()
                ).build();
    }

    private Mono<Boolean> isVoiceStateEventRelatedToExistingVoiceConnection(VoiceStateUpdateEvent event, VoiceConnection voiceConnection) {
        return voiceConnection.getChannelId()
                .map(channelId -> channelId.equals(event.getOld().flatMap(VoiceState::getChannelId).orElse(null))
                        || channelId.equals(event.getCurrent().getChannelId().orElse(null))
                );
    }

    public void handleDisconnect(Snowflake guildId) {
        guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler().stopCurrentSongAndClearQueue();
        guildVoiceService.removeGuildVoiceSupport(guildId);
        youtubeSearchQueryService.disconnectedFromGuild(guildId);
    }

}
