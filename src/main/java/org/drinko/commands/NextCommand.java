package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.SkipResult;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class NextCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return "next";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }

        return event.deferReply().then(skipSongs(event, guildId, getNumberOfSongsToSkip(event.getInteraction())));
    }

    private Mono<Void> skipSongs(ChatInputInteractionEvent event, Snowflake guildId, long numberToSkip) {
        final TrackScheduler scheduler = guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler();
        SkipResult result = scheduler.skip(numberToSkip);
        switch (result.getState()) {
            case SUCCESS:
                return event.createFollowup("Skipped " + result.getNumberSkipped() + " track" + (result.getNumberSkipped() > 1 ? "s" : "")).then();
            case INVALID:
            case EMPTY:
                return event.createFollowup(result.getMessage()).then();
        }
        return Mono.empty();
    }

    private long getNumberOfSongsToSkip(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(commandInteraction -> commandInteraction.getOption(Option.NUMBER.value))
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .orElse(1L);
    }

    @RequiredArgsConstructor
    @Getter
    private enum Option {
        NUMBER("number");
        private final String value;
    }
}
