package org.drinko.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import lombok.RequiredArgsConstructor;
import org.drinko.service.GuildVoiceService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ClearCommand implements SlashCommand {
    private final GuildVoiceService guildVoiceService;

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        Snowflake guildId = event.getInteraction().getGuildId().orElse(null);
        if (guildId == null) {
            return event.reply("ERROR: Response from discord did not include guild_id").withEphemeral(true);
        }
        return event.deferReply()
                .then(event.createFollowup("Queue Cleared").doOnNext((ignore) ->
                        guildVoiceService.getGuildVoiceSupport(guildId).getTrackScheduler().stopCurrentSongAndClearQueue()))
                .then();
    }
}
