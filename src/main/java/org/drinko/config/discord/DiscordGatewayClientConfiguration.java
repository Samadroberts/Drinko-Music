package org.drinko.config.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientPresence;
import discord4j.rest.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordGatewayClientConfiguration {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    @Bean
    public GatewayDiscordClient gatewayDiscordClient(@Autowired DiscordConfig configuration) {
        return DiscordClientBuilder.create(configuration.getToken()).build()
                .gateway()
                .setInitialPresence(ignore -> ClientPresence.online())
                .login()
                .block();
    }

    @Bean
    public RestClient discordRestClient(@Autowired GatewayDiscordClient client) {
        return client.getRestClient();
    }
}
