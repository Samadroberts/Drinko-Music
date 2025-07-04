package org.drinko.config.discord;

import dev.lavalink.youtube.clients.Web;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties("youtube.api")
@Getter
@Setter
public class YouTubePoTokenConfig {
    private String poToken;
    private String visitorData;


    @PostConstruct
    public void init() {
        if (poToken != null && !poToken.isBlank() && visitorData != null && !visitorData.isBlank()) {
            Web.setPoTokenAndVisitorData(poToken, visitorData);
        }
    }
}
