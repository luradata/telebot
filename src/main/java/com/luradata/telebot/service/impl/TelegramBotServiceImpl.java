package com.luradata.telebot.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.luradata.telebot.config.properties.TelegramBotProperties;
import com.luradata.telebot.model.TelegramMessage.ParseMode;
import com.luradata.telebot.service.TelegramBotService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TelegramBotServiceImpl implements TelegramBotService {
    private final TelegramClient telegramClient;
    private static final String COMMAND_PREFIX = "/";

    public TelegramBotServiceImpl(@Qualifier("telegramBotProperties") TelegramBotProperties telegramBotProperties) {
        this.telegramClient = new OkHttpTelegramClient(telegramBotProperties.getToken());
    }

    @Override
    public void sendMessage(String chatId, String content, ParseMode parseMode) {
        SendMessage message = new SendMessage(chatId, content);
        message.setParseMode(parseMode.getValue());
        try {
            telegramClient.execute(message);
            log.info("Message sent successfully to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Error sending message to chatId: {}", chatId, e);
        }
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                String chatId = message.getChatId().toString();
                User user = message.getFrom();

                if (message.hasText()) {
                    String messageText = message.getText();
                    log.info("Received message from user: {} ({}), chatId: {}, message: {}",
                            user.getUserName(), user.getId(), chatId, messageText);

                    if (messageText.startsWith(COMMAND_PREFIX)) {
                        handleCommand(chatId, messageText, user);
                    } else {
                        handleTextMessage(chatId, messageText, user);
                    }
                } else if (message.hasPhoto()) {
                    handlePhotoMessage(chatId, message, user);
                } else if (message.hasDocument()) {
                    handleDocumentMessage(chatId, message, user);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", update, e);
        }
    }

    private void handleCommand(String chatId, String command, User user) {
        String commandName = command.substring(1).split("\\s+")[0].toLowerCase();
        String[] args = command.split("\\s+");

        switch (commandName) {
            case "start":
                sendMessage(chatId,
                        String.format("Welcome %s! I'm your Telegram bot assistant. How can I help you today?",
                                user.getFirstName()),
                        ParseMode.MARKDOWN);
                break;
            case "help":
                sendMessage(chatId,
                        "Available commands:\n" +
                                "/start - Start the bot\n" +
                                "/help - Show this help message\n" +
                                "/echo <message> - Echo back your message",
                        ParseMode.MARKDOWN);
                break;
            case "echo":
                if (args.length > 1) {
                    String echoMessage = command.substring(6); // Remove "/echo "
                    sendMessage(chatId, echoMessage, ParseMode.MARKDOWN);
                } else {
                    sendMessage(chatId, "Please provide a message to echo", ParseMode.MARKDOWN);
                }
                break;
            default:
                sendMessage(chatId, "Unknown command. Type /help for available commands.", ParseMode.MARKDOWN);
        }
    }

    private void handleTextMessage(String chatId, String messageText, User user) {
        // Default response for text messages
        String response = String.format("Hi %s! I received your message: %s",
                user.getFirstName(), messageText);
        sendMessage(chatId, response, ParseMode.MARKDOWN);
    }

    private void handlePhotoMessage(String chatId, Message message, User user) {
        String response = String.format("Hi %s! I received your photo. Thanks for sharing!",
                user.getFirstName());
        sendMessage(chatId, response, ParseMode.MARKDOWN);
    }

    private void handleDocumentMessage(String chatId, Message message, User user) {
        String response = String.format("Hi %s! I received your document: %s",
                user.getFirstName(), message.getDocument().getFileName());
        sendMessage(chatId, response, ParseMode.MARKDOWN);
    }
}
