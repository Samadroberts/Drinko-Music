package org.drinko.util;


import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;

public class DrinkoEmbedSpecUtils {
    public static EmbedCreateSpec.Builder createDrinkoEmbedSpec() {
        return EmbedCreateSpec.builder()
                .color(Color.BROWN);
    }

    public static EmbedCreateSpec getSongLoadedEmbed(String trackTitle) {
        return createDrinkoEmbedSpec().description("**" + trackTitle + "**" + " will now play.").build();
    }

    public static EmbedCreateSpec getFailedToLoadSongEmbed(String trackTitle) {
        return createDrinkoEmbedSpec().description("**" + trackTitle + "**" + " failed to load please try again.").build();
    }

    public static EmbedCreateSpec getSongQueueEmbed(String trackTitle) {
        return createDrinkoEmbedSpec().description("**" + trackTitle + "**" + " has been added to the queue.").build();
    }
}
