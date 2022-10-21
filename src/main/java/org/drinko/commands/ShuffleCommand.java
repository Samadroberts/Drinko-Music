package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ShuffleCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;
    @Override
    public String getName() {
        return "shuffle";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }
        final Boolean shuffleEnabled = getShuffleEnabledToggle(event.getInteraction());
        if(shuffleEnabled == null) {
            return event.reply("enabled is required to be true or false").withEphemeral(true);
        }
        TrackScheduler scheduler = guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler();

        if(scheduler.isShuffled() == shuffleEnabled) {
            return event.reply("Shuffle is already "  + (shuffleEnabled ? "enabled": "disabled"));
        }

        if(shuffleEnabled) {
            scheduler.enableShuffle();
            return event.reply("Shuffle enabled");
        } else {
            scheduler.disableShuffle();
            return event.reply("Shuffle disabled");
        }
    }

    private Boolean getShuffleEnabledToggle(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(commandInteraction -> commandInteraction.getOption(Option.ENABLE.value))
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asBoolean)
                .orElse(null);
    }

    @RequiredArgsConstructor
    @Getter
    private enum Option {
        ENABLE("enable");
        private final String value;
    }
}
