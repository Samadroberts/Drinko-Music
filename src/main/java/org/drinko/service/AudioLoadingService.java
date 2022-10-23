package org.drinko.service;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.voice.VoiceConnection;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.GuildVoiceSupport;
import org.drinko.models.audio.loading.LoadSupportedAudioItemResult;
import org.drinko.models.audio.loading.LoadSupportedAudioItemResultHandler;
import org.drinko.models.audio.loading.SchedulingTrackResult;
import org.drinko.models.audio.loading.SupportedAudioPlaylistResultHandler;
import org.drinko.models.audio.loading.SupportedAudioTrackResultHandler;
import org.drinko.models.audio.loading.TrackQueuedState;
import org.drinko.models.audio.youtube.YouTubeSearchResultHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.drinko.util.DrinkoEmbedSpecUtils.createDrinkoEmbedSpec;
import static org.drinko.util.ListFormatterUtils.getFormattedTitleString;
import static org.drinko.util.ListFormatterUtils.getPositionString;

@Service
@RequiredArgsConstructor
public class AudioLoadingService {
    private final GuildVoiceService guildVoiceService;
    private final YoutubeSearchQueryService queryService;

    public Mono<SchedulingTrackResult> attemptToLoadLinkOrQuery(VoiceConnection voiceConnection, String linkOrQuery) {
        final Snowflake guildId = voiceConnection.getGuildId();
        final GuildVoiceSupport voiceSupport = guildVoiceService.getGuildVoiceSupport(voiceConnection.getGuildId());

        final LoadSupportedAudioItemResultHandler supportedAudioTrackResultHandler = new LoadSupportedAudioItemResultHandler();

        voiceSupport.getAudioPlayerManager().loadItemOrdered(guildId, linkOrQuery, supportedAudioTrackResultHandler);

        final Mono<LoadSupportedAudioItemResult> resultMono = supportedAudioTrackResultHandler.getLoadResult();

        return resultMono.map((result) -> {
            switch (result.getResult()) {
                case SINGLE_TRACK_LOADED:
                    return new SupportedAudioTrackResultHandler().handle(
                            guildVoiceService.getGuildVoiceSupport(voiceConnection.getGuildId()).getTrackScheduler(),
                            (AudioTrack) result.getLoadResult()
                    );
                case PLAYLIST_LOADED:
                    return new SupportedAudioPlaylistResultHandler().handle(
                            guildVoiceService.getGuildVoiceSupport(voiceConnection.getGuildId()).getTrackScheduler(),
                            (AudioPlaylist) result.getLoadResult()
                    );
                case NOT_FOUND:
                    return new SchedulingTrackResult(null, TrackQueuedState.QUERY);
                default:
                    return new SchedulingTrackResult(null, TrackQueuedState.FAILED);
            }
        });
    }

    public Mono<InteractionFollowupCreateSpec> attemptToQueryYoutube(Snowflake guildId, String query, Mono<MessageChannel> channel) {
        final GuildVoiceSupport voiceSupport = guildVoiceService.getGuildVoiceSupport(guildId);

        YouTubeSearchResultHandler searchResultHandler = new YouTubeSearchResultHandler();

        voiceSupport.getAudioPlayerManager().loadItemOrdered(guildId, "ytsearch: " + query, searchResultHandler);

        return searchResultHandler.loadResult().map(searchResult -> {
            switch (searchResult.getResult()) {
                case LOADED:
                    queryService.addSearchResults(guildId, (audioTrack -> getCustomButtonId(audioTrack)), searchResult.getSearchResults());
                    return createSearchFollowup(searchResult.getSearchResults());
                case FAILED_NO_MATCH:
                    return InteractionFollowupCreateSpec.builder()
                            .content("No results for `" + query + "`")
                            .build();
                case FAILED_LOADING:
                default:
                    return InteractionFollowupCreateSpec.builder()
                            .content("Failed to query youtube for `" + query + "`")
                            .build();
            }
        });
    }

    private static final String QUERY_INSTRUCTIONS = "Please select a track using the buttons below.\n\n";


    private InteractionFollowupCreateSpec createSearchFollowup(List<AudioTrack> tracksFromQuery) {
        return InteractionFollowupCreateSpec.builder()
                .addEmbed(getSearchQueryEmbed(tracksFromQuery))
                .addComponent(getButtons(tracksFromQuery))
                .build();
    }



    private static String getCustomButtonId(AudioTrack audioTrack) {
        return audioTrack.getIdentifier();
    }

    private ActionRow getButtons(List<AudioTrack> trackFromQuery) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < trackFromQuery.size(); i++) {
            buttons.add(Button.primary(getCustomButtonId(trackFromQuery.get(i)), String.valueOf(i + 1)));
        }
        return ActionRow.of(buttons);
    }

    public static EmbedCreateSpec getSearchQueryEmbed(List<AudioTrack> tracksFromQuery) {
        String description = QUERY_INSTRUCTIONS;
        for (int i = 0; i < tracksFromQuery.size(); i++) {
            description += getPositionString(i + 1);
            description += getFormattedTitleString(tracksFromQuery.get(i).getInfo());
            if (i == tracksFromQuery.size() - 1) {
                break;
            }
            description += "\n";
        }
        return createDrinkoEmbedSpec().description(description).build();
    }
}
