package com.luradata.telebot.service.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.luradata.telebot.config.properties.TelegramBotProperties;
import com.luradata.telebot.model.OllamaResponse;
import com.luradata.telebot.model.TelegramMessage.ParseMode;
import com.luradata.telebot.service.TelegramBotService;
import com.luradata.telebot.util.HttpCaller;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class TelegramBotServiceImpl implements TelegramBotService {
    private final TelegramClient telegramClient;
    private final String botUsername;
    private final String ollamaModel;
    private static final String COMMAND_PREFIX = "/";
    private static final String OLLAMA_URL = "http://ollama:11434/api/generate";
    
    // Map to track processing status for each user
    private final Map<Long, CompletableFuture<Void>> userProcessingStatus = new ConcurrentHashMap<>();

    public TelegramBotServiceImpl(@Qualifier("telegramBotProperties") TelegramBotProperties telegramBotProperties,
    @Value("${llm.model.name}") String ollamaModelName) {
        this.telegramClient = new OkHttpTelegramClient(telegramBotProperties.getToken());
        this.botUsername = telegramBotProperties.getUsername();
        this.ollamaModel = ollamaModelName;
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
                Long chatId = message.getChatId();
                User user = message.getFrom();

                if (chatId < 0) {
                    // Group chat
                    handleGroupChat(String.valueOf(chatId), message, user);
                } else {
                    // Private chat
                    handlePrivateChat(String.valueOf(chatId), message, user);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", update, e);
        }
    }

    private void handleGroupChat(String chatId, Message message, User user) {
        if (message.hasText()){
            String messageText = message.getText();
            if (isMentioned(messageText)) {
                // Remove @botUsername from messageText
                messageText = messageText.replace("@" + botUsername, "").trim();
                handleTextMessage(chatId, messageText, user);
            }
        }
    }

    private void handlePrivateChat(String chatId, Message message, User user) {
        if (message.hasText()){
            String messageText = message.getText();
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

    private boolean isMentioned(String messageText) {
        String[] parts = messageText.split(" ");
        for (String part : parts) {
            if (part.startsWith("@")) {
                return part.equals("@" + botUsername);
            }
        }
        return false;
    }

    private void handleCommand(String chatId, String command, User user) {
        String commandName = command.substring(1).split("\\s+")[0].toLowerCase();
        String[] args = command.split("\\s+");

        switch (commandName) {
            case "start":
                sendMessage(chatId,
                        String.format("Xin chào %s! Tôi là trợ lý Telegram của bạn. Tôi có thể giúp gì cho bạn hôm nay?",
                                user.getFirstName()),
                        ParseMode.MARKDOWN);
                break;
            case "help":
                sendMessage(chatId,
                        "Các lệnh có sẵn:\n" +
                                "/start - Bắt đầu sử dụng bot\n" +
                                "/help - Hiển thị thông tin trợ giúp\n" +
                                "/echo <tin nhắn> - Lặp lại tin nhắn của bạn",
                        ParseMode.MARKDOWN);
                break;
            case "echo":
                if (args.length > 1) {
                    String echoMessage = command.substring(6); // Remove "/echo "
                    sendMessage(chatId, echoMessage, ParseMode.MARKDOWN);
                } else {
                    sendMessage(chatId, "Vui lòng nhập tin nhắn để lặp lại", ParseMode.MARKDOWN);
                }
                break;
            default:
                sendMessage(chatId, "Lệnh không hợp lệ. Gõ /help để xem các lệnh có sẵn.", ParseMode.MARKDOWN);
        }
    }

    private void handleTextMessage(String chatId, String messageText, User user) {
        Long userId = user.getId();
        
        // Check if user has an ongoing request
        if (userProcessingStatus.containsKey(userId)) {
            sendMessage(chatId, "Bạn đang có một yêu cầu đang được xử lý. Vui lòng đợi cho đến khi yêu cầu trước đó hoàn thành.", ParseMode.MARKDOWN);
            return;
        }

        HttpCaller httpCaller = new HttpCaller();
        
        // Create request body for Ollama API
        Map<String, Object> requestBody = Map.of(
            "model", ollamaModel,
            "prompt", messageText,
            "stream", false
        );

        // Create HTTP request configuration
        HttpCaller.HttpRequestConfig config = HttpCaller.HttpRequestConfig.builder()
            .url(OLLAMA_URL)
            .method(HttpCaller.HttpMethod.POST)
            .headers(Map.of("Content-Type", "application/json"))
            .body(requestBody)
            .build();

        // Create a CompletableFuture to track this request
        CompletableFuture<Void> processingFuture = new CompletableFuture<>();
        userProcessingStatus.put(userId, processingFuture);

        // Make API call and handle response
        httpCaller.callApi(config, OllamaResponse.class)
            .thenAccept(response -> {
                try {
                    if (response != null && response.getResponse() != null) {
                        sendMessage(chatId, response.getResponse(), ParseMode.MARKDOWN);
                    } else {
                        sendMessage(chatId, "Xin lỗi, tôi không thể xử lý yêu cầu của bạn.", ParseMode.MARKDOWN);
                    }
                } finally {
                    // Remove the processing status and complete the future
                    userProcessingStatus.remove(userId);
                    processingFuture.complete(null);
                }
            })
            .exceptionally(ex -> {
                log.error("Error calling Ollama API", ex);
                sendMessage(chatId, "Xin lỗi, đã có lỗi xảy ra khi xử lý yêu cầu của bạn.", ParseMode.MARKDOWN);
                // Remove the processing status and complete the future
                userProcessingStatus.remove(userId);
                processingFuture.completeExceptionally(ex);
                return null;
            });
    }

    private void handlePhotoMessage(String chatId, Message message, User user) {
        String response = String.format("Xin chào %s! Tôi đã nhận được ảnh của bạn. Cảm ơn bạn đã chia sẻ!",
                user.getFirstName());
        sendMessage(chatId, response, ParseMode.MARKDOWN);
    }

    private void handleDocumentMessage(String chatId, Message message, User user) {
        String response = String.format("Xin chào %s! Tôi đã nhận được tài liệu của bạn: %s",
                user.getFirstName(), message.getDocument().getFileName());
        sendMessage(chatId, response, ParseMode.MARKDOWN);
    }
}
