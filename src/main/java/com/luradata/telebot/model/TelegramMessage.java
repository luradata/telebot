package com.luradata.telebot.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class TelegramMessage {

    @RequiredArgsConstructor
    public enum ParseMode {
        MARKDOWN("Markdown"),
        MARKDOWNV2("MarkdownV2"),
        HTML("HTML");

        @Getter
        private final String value;
    }

}
