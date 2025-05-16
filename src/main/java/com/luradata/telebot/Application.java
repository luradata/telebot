package com.luradata.telebot;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.luradata.telebot.config.properties.TelegramBotProperties;
import com.luradata.telebot.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

	private final TelegramBotProperties telegramBotProperties;
	private final TelegramBotsLongPollingApplication botsApplication;
	private final TelegramBotService telegramBotService;

	public Application(@Qualifier("telegramBotProperties") TelegramBotProperties telegramBotProperties,
			TelegramBotService telegramBotService) {
		this.telegramBotProperties = telegramBotProperties;
		this.botsApplication = new TelegramBotsLongPollingApplication();
		this.telegramBotService = telegramBotService;
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		registerBot();
	}

	private void registerBot() {
		try {
			botsApplication.registerBot(telegramBotProperties.getToken(), telegramBotService);
		} catch (TelegramApiException e) {
			log.error("Error while registering bot: ", e);
		}
	}

}
