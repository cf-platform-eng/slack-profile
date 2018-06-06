package io.pivotal.ecosystem.slack;

import feign.Feign;
import feign.Logger;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Value("${slack.token}")
    private String slackToken;

    @Bean
    public String authToken() {
        return slackToken;
    }

    @Bean
    public SlackRepository slackRepository() {
        return Feign.builder()
                .encoder(new GsonEncoder())
                .decoder(new GsonDecoder())
                .logger(new Logger.JavaLogger())
                .logLevel(Logger.Level.FULL)
                .target(SlackRepository.class, "https://slack.com/api");
    }
}
