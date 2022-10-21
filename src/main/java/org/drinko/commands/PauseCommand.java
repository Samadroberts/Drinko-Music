package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PauseCommand implements SlashCommand {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final GuildVoiceService voiceService;

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }
        TrackScheduler trackScheduler = voiceService.getGuildVoiceSupport(guildId).getTrackScheduler();

        if (trackScheduler.isNoTrackPlayingAndQueueEmpty()) {
            return event.reply("The player is not currently playing anything. Use the /" + PlayCommand.COMMAND_NAME + " command to add a track.");
        }

        if (trackScheduler.isPaused()) {
            return event.reply("The player is already paused.");
        }
        if (trackScheduler.pause()) {
            return event.reply("The player is now paused. You can unpause it with /" + ResumeCommand.COMMAND_NAME);
        }
        return event.reply("Failed to pause track").withEphemeral(true);
    }
}
