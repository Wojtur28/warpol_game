package org.example.warpol.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "game")
@Getter
@Setter
public class GameConfig {
    private Board board;
    private Units units;

    @Getter @Setter
    public static class Board {
        private int width;
        private int height;
    }

    @Getter @Setter
    public static class Units {
        private int archer;
        private int cannon;
        private int transport;
    }
}
