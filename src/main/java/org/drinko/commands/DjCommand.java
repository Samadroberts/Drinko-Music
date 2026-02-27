package org.drinko.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.commands.exceptions.CommandIssuerNotInVoiceChat;
import org.drinko.models.audio.GuildVoiceSupport;
import org.drinko.service.AudioLoadingService;
import org.drinko.service.GuildVoiceService;
import org.drinko.service.VoiceConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.drinko.util.DrinkoEmbedSpecUtils.getFailedToLoadSongEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongLoadedEmbed;
import static org.drinko.util.DrinkoEmbedSpecUtils.getSongQueueEmbed;

@Component
@RequiredArgsConstructor
public class DjCommand implements SlashCommand {
    private static final Logger logger = LoggerFactory.getLogger(DjCommand.class);
    public static final String COMMAND_NAME = "dj";
    private static final String SYSTEM_PROMPT = """
            You are Drinko, the Playlist Architect.
            CURATION RULES:
            Serve exactly 5 UNIQUE songs.
            Use the 'Theme' for genre and the mood/energy.
            
            If inputs are missing, mix a "House Special" (eclectic vibe).
            
            RESPONSE FORMAT:
            
            1. List the 5 songs clearly, including the Song Title and Artist Name.
            
            2. Provide a brief (1-sentence) description of why each song fits the requested 'Theme'.
            """;
    private static final String USER_PROMPT = """
            Mix a 5-song carafe using the following theme: {theme}
            """;
    private final AudioLoadingService audioLoadingService;
    private final VoiceConnectionService voiceConnectionService;
    private final GuildVoiceService guildVoiceService;
    private final ChatClient chatClient;

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final String context = getLink(event.getInteraction());


        Mono<VoiceState> voiceStateOrEmpty = event.getInteraction().getMember()
                .map(member -> member.getVoiceState().switchIfEmpty(Mono.error(new CommandIssuerNotInVoiceChat())))
                .orElseGet(() -> Mono.empty());


        Mono<Void> handleSongLoad = voiceStateOrEmpty
                .onErrorResume(CommandIssuerNotInVoiceChat.class, (exception) -> event.createFollowup(exception.getMessage()).then(Mono.empty()))
                .flatMap(VoiceState::getChannel)
                .flatMap(voiceChannel -> voiceConnectionService.getNewOrExistingConnection(voiceChannel, event.getInteraction().getChannel()))
                .flatMap(voiceConnection -> {
                    DrinkoPlaylist playlist = chatClient.prompt()
                            .system(SYSTEM_PROMPT)
                            .user((u) -> u.text(USER_PROMPT).param("theme", context == null || context.isEmpty() ? "None" : context))
                            .options(ChatOptions.builder()
                                    .temperature(0.7)
                                    .build())
                            .call()
                            .entity(DrinkoPlaylist.class);
                    if (playlist != null && !playlist.songs.isEmpty()) {
                        logger.info("Processing playlist: {}", playlist.intro());
                        return Flux.fromIterable(playlist.songs)
                                .doOnNext((song) -> {
                                    logger.info("Song Added: Title='{}' | Artist='{}' | Reason='{}'",
                                            song.title(),
                                            song.artist(),
                                            song.reasoning()
                                    );
                                })
                                .flatMap((song -> {

                                    return audioLoadingService.queryYoutube(voiceConnection.getGuildId(), song.title + " " + song.artist)
                                            .flatMap(youtubeSearchResult -> {
                                        switch (youtubeSearchResult.getResult()) {
                                            case LOADED:
                                                AudioTrack track =  youtubeSearchResult.getSearchResults().getFirst();
                                                GuildVoiceSupport voiceSupport = guildVoiceService.getGuildVoiceSupport(voiceConnection.getGuildId());
                                                switch (voiceSupport.getTrackScheduler().queue(track)) {
                                                    case QUEUED:
                                                        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                                                .addEmbed(getSongQueueEmbed(track.getInfo().title))
                                                                .build()).then();
                                                    case PLAYING_NOW:
                                                        return event.createFollowup(InteractionFollowupCreateSpec.builder()
                                                                .addEmbed(getSongLoadedEmbed(track.getInfo().title))
                                                                .build()).then();
                                                    case FAILED:
                                                    default:
                                                        return Mono.empty();
                                                }
                                            case FAILED_NO_MATCH:
                                            case FAILED_LOADING:
                                            default:
                                                return  Mono.empty();
                                        }
                                    });
                                }))
                                .collectList()
                                .then();
                    } else {
                        return Mono.empty();
                    }
                });

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
        LINK("context");
        private final String value;
    }

    private record DrinkoPlaylist(
            String intro,
            List<Song> songs
    ) {
        public record Song(String title, String artist, String reasoning) {}
    }
}
