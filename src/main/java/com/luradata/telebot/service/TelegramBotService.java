package com.luradata.telebot.service;

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;

import com.luradata.telebot.model.TelegramMessage;

public interface TelegramBotService extends LongPollingSingleThreadUpdateConsumer{

    void sendMessage(String chatId, String content, TelegramMessage.ParseMode parseMode);

}
