package org.drinko.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.RepeatMode;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.drinko.util.DrinkoEmbedSpecUtils.createDrinkoEmbedSpec;
import static org.drinko.util.ListFormatterUtils.getFormattedTitleString;
import static org.drinko.util.ListFormatterUtils.getPageNumberLine;
import static org.drinko.util.ListFormatterUtils.getPositionString;

@Component
@RequiredArgsConstructor
public class ListCommand implements SlashCommand {

    public final static int MAX_LIST_EMBEDDED_DESCRIPTION_LENGTH = 4096;
    private final static String PLAY_BUTTON_EMOJI = " **▶** ";
    private final static String PAUSE_BUTTON_EMOJI = " **⏸** ";
    private final static String PLAYING_SONG_SEPARATOR = "-----------------------------------------------------------------------------------------------\n";
    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }

        final TrackScheduler trackScheduler = guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler();

        final Map<Integer, AudioTrackInfo> trackMap = trackScheduler.getTrackOrderMap();

        if (trackMap.isEmpty()) {
            return event.reply("Not currently playing anything.").withEphemeral(true);
        }

        long page = getPage(event.getInteraction());
        if (page < 1) {
            return event.reply("Page number must be greater than 0");
        }

        return event.deferReply().then(createFollowup(
                event,
                trackMap,
                trackScheduler.getRepeatMode(),
                page,
                trackScheduler.isShuffled(),
                trackScheduler.isPaused()
        )).then();
    }

    private Mono<Void> createFollowup(
            ChatInputInteractionEvent event,
            Map<Integer, AudioTrackInfo> trackMap,
            RepeatMode repeatMode,
            long page,
            boolean isShuffled,
            boolean isPaused
    ) {
        final InteractionFollowupCreateSpec interactionFollowupCreateSpec = InteractionFollowupCreateSpec.builder()
                .addEmbed(getEmbed(trackMap, page, isShuffled, isPaused))
                .build();

        return event.createFollowup(interactionFollowupCreateSpec).flatMap(message -> {
            if (RepeatMode.NONE.equals(repeatMode)) {
                return Mono.empty();
            }
            if (isShuffled) {
                return message.addReaction(ReactionEmoji.unicode("🔀")).then(message.addReaction(ReactionEmoji.unicode(repeatMode.getEmoji())));
            }
            return message.addReaction(ReactionEmoji.unicode(repeatMode.getEmoji()));
        });
    }

    private EmbedCreateSpec getEmbed(Map<Integer, AudioTrackInfo> trackList, long page, boolean isShuffled, boolean isPaused) {
        AbstractMap.SimpleEntry<Long, String> maxPageAndDescription = getDescription(trackList, page, isShuffled, isPaused);
        return createDrinkoEmbedSpec()
                .title(getPageNumberLine(page, maxPageAndDescription.getKey()))
                .description(maxPageAndDescription.getValue())
                .build();
    }

    private AbstractMap.SimpleEntry<Long, String> getDescription(Map<Integer, AudioTrackInfo> trackMap, long page, boolean isShuffled, boolean isPaused) {
        long charCount = 0;
        long currentPage = 1;
        boolean isCountingPages = false;
        StringBuilder description = new StringBuilder();
        List<Map.Entry<Integer, AudioTrackInfo>> entryList = new ArrayList<>(trackMap.entrySet());


        for (int i = 0; i < entryList.size(); ) {
            String currentLine = "";

            if (entryList.get(i).getKey() - 1 == 0 && page == 1) {
                if (isPaused) {
                    currentLine += PAUSE_BUTTON_EMOJI;
                } else {
                    currentLine += PLAY_BUTTON_EMOJI;
                }
            }
            if(entryList.get(i).getKey() - 1 != 0) {
                currentLine += getPositionString(entryList.get(i).getKey() - 1);
            }


            currentLine += getFormattedTitleString(entryList.get(i).getValue());

            if (i != trackMap.size() - 1) {
                currentLine += "\n";
            }

            if (i == 0 && page == 1 && entryList.size() != 1) {
                currentLine += PLAYING_SONG_SEPARATOR;
            }

            if (charCount + currentLine.length() <= (MAX_LIST_EMBEDDED_DESCRIPTION_LENGTH)) {
                if (!isCountingPages) {
                    description.append(currentLine);
                }
                charCount += currentLine.length();
            } else {
                if (currentPage == page) {
                    isCountingPages = true;
                }
                entryList = new ArrayList<>(entryList.subList(i, entryList.size()));
                i = 0;
                if (!isCountingPages) {
                    description.setLength(0);
                }
                charCount = 0;
                currentPage++;
                //Clear existing page data
                continue;
            }
            i++;
        }
        return new AbstractMap.SimpleEntry<>(currentPage, description.toString());
    }

    private long getPage(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(commandInteraction -> commandInteraction.getOption(Option.PAGE.value))
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .orElse(1L);
    }


    @RequiredArgsConstructor
    @Getter
    private enum Option {
        PAGE("page");
        private final String value;
    }
}
