package org.drinko.config.discord;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("discord.api")
@Getter
@Setter
public class DiscordConfig {
    private String token;
}
