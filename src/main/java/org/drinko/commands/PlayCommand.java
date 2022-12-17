package org.drinko.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.commands.exceptions.CommandIssuerNotInVoiceChat;
import org.drinko.service.AudioLoadingService;
import org.drinko.service.VoiceConnectionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static org.drinko.util.DrinkoEmbedSpecUtils.getFailedToLoadSongEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongLoadedEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongQueueEmbed;

@Component
@RequiredArgsConstructor
public class PlayCommand implements SlashCommand {
    public static final String COMMAND_NAME = "play";
    private final AudioLoadingService audioLoadingService;
    private final VoiceConnectionService voiceConnectionService;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final String linkOrQuery = getLink(event.getInteraction());


        Mono<VoiceState> voiceStateOrEmpty = event.getInteraction().getMember()
                .map(member -> member.getVoiceState().switchIfEmpty(Mono.error(new CommandIssuerNotInVoiceChat())))
                .orElseGet(() -> Mono.empty());


        Mono<Void> handleSongLoad = voiceStateOrEmpty
                .onErrorResume(CommandIssuerNotInVoiceChat.class, (exception) -> event.createFollowup(exception.getMessage()).then(Mono.empty()))
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceConnectionService.getNewOrExistingConnection(voiceChannel, event.getInteraction().getChannel())).flatMap(voiceConnection ->
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
                                            return audioLoadingService.attemptToQueryYoutube(
                                                            voiceConnection.getGuildId(),
                                                            linkOrQuery,
                                                            event.getInteraction().getChannel()
                                                    ).flatMap(interactionFollowupCreateSpec -> event.createFollowup(interactionFollowupCreateSpec))
                                                    .then();
                                        case FAILED:
                                        default:
                                            return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                                            .addEmbed(getFailedToLoadSongEmbed(linkOrQuery))
                                                            .build()
                                                            .withEphemeral(true))
                                                    .then();
                                    }
                                }));

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
        LINK("link-or-query");
        private final String value;
    }
}
