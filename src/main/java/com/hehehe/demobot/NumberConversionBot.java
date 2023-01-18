package com.hehehe.demobot;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class NumberConversionBot extends TelegramLongPollingBot {

    public static final String BINARY_TO_DECIMAL = "/binary_to_decimal";
    public static final String DECIMAL_TO_BINARY = "/decimal_to_binary";
    public static final String MENU = "/menu";
    public static final String START = "/start";
    public static final String BACK = "/back";
    public static final String ERROR_HAPPENED_CHECK_YOUR_INPUT = "Error happened, check your input";

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    private final ConcurrentHashMap<Long, String> usersState = new ConcurrentHashMap<>();

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            final Message message = update.getMessage();

            if (message.isCommand()) {
                processCommand(message);
                return;
            }

            if (usersState.get(message.getChatId()) != null) {
                processUsersStateMessage(message);
                return;
            }
        }

        if (update.hasCallbackQuery()) {
            processCallbackQuery(update);
        }
    }

    private void processCommand(Message message) throws TelegramApiException {
        switch (message.getText()) {
            case START -> {
                execute(buildWelcomeMessage(message));
            }
            case BINARY_TO_DECIMAL -> {
                usersState.put(message.getChatId(), BINARY_TO_DECIMAL);
                initializeConverterCommand(message);
            }
            case DECIMAL_TO_BINARY -> {
                usersState.put(message.getChatId(), DECIMAL_TO_BINARY);
                initializeConverterCommand(message);
            }
        }
    }

    private void processCallbackQuery(Update update) throws TelegramApiException {
        final Message message = update.getCallbackQuery().getMessage();

        switch (update.getCallbackQuery().getData()) {
            case BINARY_TO_DECIMAL -> {
                usersState.put(message.getChatId(), BINARY_TO_DECIMAL);
                initializeConverterCommand(message);
            }
            case DECIMAL_TO_BINARY -> {
                usersState.put(message.getChatId(), DECIMAL_TO_BINARY);
                initializeConverterCommand(message);
            }
            case MENU -> {
                execute(buildMenuMessage(message));
            }
            case BACK -> {
                returnToMainMenuReplyMarkup(message);
            }
        }
    }

    private void initializeConverterCommand(Message message) throws TelegramApiException {
        execute(SendMessage.builder().chatId(message.getChatId()).text("Write a number").build());
    }


    private void processUsersStateMessage(Message message) throws TelegramApiException {
        switch (usersState.get(message.getChatId())) {
            case BINARY_TO_DECIMAL -> {
                convertBinaryToDecimal(message);
            }
            case DECIMAL_TO_BINARY -> {
                convertDecimalToBinary(message);
            }
        }
    }

    private void convertBinaryToDecimal(Message message) throws TelegramApiException {
        try {
            final long converted = Long.parseLong(message.getText(), 2);
            execute(buildConvertedNumberMessage(message, String.valueOf(converted)));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            execute(SendMessage.builder().chatId(message.getChatId()).text(ERROR_HAPPENED_CHECK_YOUR_INPUT).build());
        }
    }

    private void convertDecimalToBinary(Message message) throws TelegramApiException {
        try {
            final String converted = Long.toBinaryString(Long.parseLong(message.getText()));
            execute(buildConvertedNumberMessage(message, converted));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            execute(SendMessage.builder().chatId(message.getChatId()).text(ERROR_HAPPENED_CHECK_YOUR_INPUT).build());
        }

    }

    private SendMessage buildConvertedNumberMessage(Message message, String converted) {
        final InlineKeyboardMarkup replyKeyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(InlineKeyboardButton.builder().text("Back").callbackData(BACK).build())))
                .build();
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text(String.format("Converted number: %s", converted))
                .replyMarkup(replyKeyboard)
                .build();
    }

    private SendMessage buildMenuMessage(Message message) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text("Menu")
                .replyMarkup(buildMainMenuKeyboardMarkup())
                .build();
    }

    private SendMessage buildWelcomeMessage(Message message) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text(String.format("Hello, %s! Let's get started converting numbers. HEHE", message.getFrom().getFirstName()))
                .replyMarkup(buildMainMenuKeyboardMarkup())
                .build();
    }

    private InlineKeyboardMarkup buildMainMenuKeyboardMarkup() {
        final List<InlineKeyboardButton> keyboardButtons = List.of(
                InlineKeyboardButton.builder().text("Binary to Decimal").callbackData(BINARY_TO_DECIMAL).build(),
                InlineKeyboardButton.builder().text("Decimal to Binary").callbackData(DECIMAL_TO_BINARY).build()
        );
        return InlineKeyboardMarkup.builder().keyboard(List.of(keyboardButtons)).build();
    }

    private void returnToMainMenuReplyMarkup(Message message) throws TelegramApiException {
        execute(EditMessageReplyMarkup.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .replyMarkup(buildMainMenuKeyboardMarkup())
                .build());
    }

}
