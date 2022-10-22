package org.drinko.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.PartialMember;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.voice.VoiceConnection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.listeners.button.ButtonInteractionListener;
import org.drinko.service.AudioLoadingService;
import org.drinko.service.GuildVoiceService;
import org.drinko.service.VoiceConnectionService;
import org.drinko.service.YoutubeSearchQueryService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.drinko.util.DrinkoEmbedSpecUtils.getFailedToLoadSongEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongLoadedEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongQueueEmbed;

@Component
@RequiredArgsConstructor
public class PlayCommand implements SlashCommand {

    public static final String COMMAND_NAME = "play";
    private final GuildVoiceService service;

    private final AudioLoadingService audioLoadingService;

    private final GatewayDiscordClient gatewayDiscordClient;

    private final VoiceConnectionService voiceConnectionService;

    private final YoutubeSearchQueryService queryService;

    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {

        Mono<VoiceState> voiceStateOrEmpty = event.getInteraction().getMember()
                .map(PartialMember::getVoiceState)
                .orElse(Mono.empty());


//        Mono<Void> noUserInVoice = voiceStateOrEmpty.defaultIfEmpty(null)
//                .flatMap((ignore) -> event.reply("You must join a voice channel first.").withEphemeral(true));


        Mono<VoiceConnection> connection = voiceStateOrEmpty
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceConnectionService.getNewOrExistingConnection(voiceChannel, event.getInteraction().getChannel()));

        final String linkOrQuery = getLink(event.getInteraction());

        Mono<Void> handleSongLoad = connection.flatMap(voiceConnection ->
                audioLoadingService.attemptToLoadLinkOrQuery(voiceConnection, linkOrQuery)
                        .flatMap((result) -> {
                            switch (result.getQueueResult()) {
                                case QUEUED:
                                    return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                            .addEmbed(getSongQueueEmbed(result.getAudioTrackInfo().title))
                                            .build()).then();
                                case PLAYING_NOW:
                                    return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                            .addEmbed(getSongLoadedEmbed(result.getAudioTrackInfo().title))
                                            .build()).then();
                                case QUERY:
                                    return audioLoadingService.attemptToQueryYoutube(voiceConnection.getGuildId(), linkOrQuery, event.getInteraction().getChannel())
                                            .flatMap(interactionFollowupCreateSpec -> event.createFollowup(interactionFollowupCreateSpec))
                                            .then(new ButtonInteractionListener(
                                                    queryService,
                                                    gatewayDiscordClient,
                                                    voiceConnection.getGuildId(),
                                                    guildVoiceService
                                            ).createButtonListener());
                                case FAILED:
                                default:
                                    return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                            .addEmbed(getFailedToLoadSongEmbed(linkOrQuery))
                                            .build()
                                            .withEphemeral(true))
                                            .then();
                            }
                        })).switchIfEmpty(event.createFollowup("You must join a voice channel first.").then());

        return event.deferReply().then(handleSongLoad.then());
    }

    private String getLink(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(commandInteraction -> commandInteraction.getOption(Option.LINK.value))
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);
    }

    @RequiredArgsConstructor
    @Getter
    private enum Option {
        LINK("link");
        private final String value;
    }
}
