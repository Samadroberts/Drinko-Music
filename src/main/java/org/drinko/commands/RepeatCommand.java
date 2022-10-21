package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.GuildVoiceSupport;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RepeatCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return "repeat";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        RepeatType type = getTimeUnit(event.getInteraction());
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }
        GuildVoiceSupport voiceSupport = guildVoiceService.getGuildVoiceSupport(guildId);
        switch (type) {
            case ALL:
                voiceSupport.getTrackScheduler().enableRepeatAll();
                return event.reply("The player will now repeat the queue.");
            case NONE:
                voiceSupport.getTrackScheduler().disableRepeat();
                return event.reply("The player is no longer on repeat.");
            case ONE:
                voiceSupport.getTrackScheduler().enableRepeatOne();
                return event.reply("The player will now repeat the current track");
        }
        return Mono.empty();
    }

    private RepeatType getTimeUnit(Interaction interaction) {
        return interaction.getCommandInteraction()
                .flatMap(applicationCommandInteraction -> applicationCommandInteraction.getOption(Option.TYPE.value))
                .flatMap(applicationCommandInteractionOption -> applicationCommandInteractionOption.getValue())
                .map(applicationCommandInteractionOptionValue -> applicationCommandInteractionOptionValue.asString())
                .map((value) -> RepeatType.valueOf(value.toUpperCase()))
                .orElseThrow(() -> new IllegalArgumentException("type parameter is required"));
    }

    @RequiredArgsConstructor
    @Getter
    enum RepeatType {
        ALL("all"),
        NONE("none"),
        ONE("one");

        private final String value;
    }

    @RequiredArgsConstructor
    @Getter
    private enum Option {
        TYPE("type");
        private final String value;
    }
}
