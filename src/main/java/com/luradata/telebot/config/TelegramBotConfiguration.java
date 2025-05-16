package com.luradata.telebot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.luradata.telebot.config.properties.TelegramBotProperties;


@Configuration
public class TelegramBotConfiguration {

    @Bean("telegramBotProperties")
    @ConfigurationProperties(prefix = "telegram.bot")
    TelegramBotProperties telegramBotProperties() {
        return new TelegramBotProperties();
    }
}
