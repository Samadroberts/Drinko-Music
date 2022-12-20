package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.RemoveResult;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RemoveCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }

        return event.deferReply().then(removeTrack(event, guildId, getIndexOfSongToRemove(event.getInteraction())));
    }

    private Mono<Void> removeTrack(ChatInputInteractionEvent event, Snowflake guildId, long indexOfSongToRemove) {
        final TrackScheduler scheduler = guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler();
        RemoveResult result = scheduler.removeTrack(indexOfSongToRemove);
        switch (result.getState()) {
            case SUCCESS:
                return event.createFollowup("Removed track at index " + indexOfSongToRemove + " from the queue").then();
            case INVALID:
            case EMPTY:
                return event.createFollowup(result.getMessage()).then();
        }
        return Mono.empty();
    }

    private long getIndexOfSongToRemove(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(commandInteraction -> commandInteraction.getOption(Option.INDEX.value))
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .orElse(1L);
    }

    @RequiredArgsConstructor
    @Getter
    private enum Option {
        INDEX("index");
        private final String value;
    }
}
