package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.drinko.models.audio.trackscheduler.TrackScheduler;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ResumeCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;

    public static final String COMMAND_NAME = "resume";

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }
        TrackScheduler trackScheduler = guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler();

        if(trackScheduler.isNoTrackPlayingAndQueueEmpty()) {
            return event.reply("The player is not currently playing anything. Use the /" + PlayCommand.COMMAND_NAME + " command to add a track.");
        }

        if(!trackScheduler.isPaused()) {
            return event.reply("The player is not paused.");
        }

        if (trackScheduler.resume()) {
            return event.reply("The player is no longer paused.");
        }
        return event.reply("Failed to unpause track");
    }
}
