package org.drinko.config.discord;

import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.Web;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties("youtube.api")
@Getter
@Setter
public class YoutubeConfig {
    private String poToken;
    private String visitorData;
    private String remoteCipherUrl;
    private String remoteCipherPassword;
    private String remoteCipherUserAgent;


    @PostConstruct
    public void init() {
        if (poToken != null && !poToken.isBlank() && visitorData != null && !visitorData.isBlank()) {
            Web.setPoTokenAndVisitorData(poToken, visitorData);
        }
    }

    @Bean
    public YoutubeSourceOptions youtubeSourceOptions() {
        YoutubeSourceOptions options = new YoutubeSourceOptions();
        options.setRemoteCipher(remoteCipherUrl, remoteCipherPassword, remoteCipherUserAgent);
                options.setAllowSearch(true);
        options.setAllowDirectVideoIds(true);
        options.setAllowDirectPlaylistIds(true);
        return options;
    }
}
