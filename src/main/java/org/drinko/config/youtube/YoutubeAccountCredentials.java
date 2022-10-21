package org.drinko.config.youtube;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("org.drinko.youtube")
@Getter
@Setter
public class YoutubeAccountCredentials {
    private String username;
    private String password;
}
