package com.example.childhomeworktracker.telegram;

import com.example.childhomeworktracker.config.BotProperties;
import com.example.childhomeworktracker.service.ParentDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@Component
public class ChildhomeworkTrackerBot extends TelegramLongPollingBot {

    @Autowired private BotProperties botProperties;
    @Autowired private ParentDataService parentDataService;

    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> userPhones = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    @Override public String getBotUsername() { return botProperties.getName().replace("@", ""); }
    @Override public String getBotToken()   { return botProperties.getToken(); }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;
        Message msg = update.getMessage();
        Long chatId = msg.getChatId();

        if (!msg.getChat().isUserChat()) {
            if (msg.hasText() && "/start".equals(msg.getText().trim())) {
                sendText(chatId, "Этот бот работает только в личных сообщениях. Чтобы получить работы — напишите ему в личку.");
            }
            return;
        }

        executor.submit(() -> process(msg));
    }

    private void process(Message msg) {
        Long chatId = msg.getChatId();
        String text = msg.hasText() ? msg.getText().trim() : "";

        if ("В начало".equals(text) || "/start".equals(text)) {
            userStates.put(chatId, "PHONE");
            userPhones.remove(chatId);
            askPhone(chatId);
            return;
        }

        if ("Назад".equals(text)) {
            String phone = userPhones.get(chatId);
            if (phone != null) showKids(chatId, phone);
            else askPhone(chatId);
            return;
        }

        String state = userStates.getOrDefault(chatId, "");

        if (msg.hasContact() && "PHONE".equals(state)) {
            String phone = msg.getContact().getPhoneNumber();
            if (!phone.startsWith("+")) phone = "+" + phone;
            userPhones.put(chatId, phone);
            userStates.put(chatId, "KIDS");
            showKids(chatId, phone);
        } else if ("KIDS".equals(state) && !text.isEmpty()) {
            sendPdf(chatId, text);
        }
    }

    private void askPhone(Long chatId) {
        SendMessage msg = new SendMessage(chatId.toString(),
                "Привет! Поделись номером телефона:");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setOneTimeKeyboard(true);
        kb.setResizeKeyboard(true);
        KeyboardRow row = new KeyboardRow();
        KeyboardButton btn = new KeyboardButton("Поделиться номером");
        btn.setRequestContact(true);
        row.add(btn);
        kb.setKeyboard(List.of(row));
        msg.setReplyMarkup(kb);
        send(msg);
    }

    private void showKids(Long chatId, String phone) {
        List<String> kids = parentDataService.getChildRegistrationsForParent(phone);
        if (kids.isEmpty()) {
            sendText(chatId, "Нет детей с таким номером.");
            askPhone(chatId);
            return;
        }

        SendMessage msg = new SendMessage(chatId.toString(), "Выберите работу ребенка:");
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup();
        kb.setResizeKeyboard(true);
        List<KeyboardRow> rows = new ArrayList<>();

        for (int i = 0; i < kids.size(); i += 2) {
            KeyboardRow row = new KeyboardRow();
            row.add(kids.get(i));
            if (i + 1 < kids.size()) row.add(kids.get(i + 1));
            rows.add(row);
        }

        KeyboardRow nav = new KeyboardRow();
        nav.add("Назад");
        nav.add("В начало");
        rows.add(nav);
        kb.setKeyboard(rows);
        msg.setReplyMarkup(kb);
        send(msg);
    }

    /** Отправка PDF с проверкой на пустой файл */
    private void sendPdf(Long chatId, String reg) {
        String phone = userPhones.get(chatId);
        if (phone == null || !parentDataService.getChildRegistrationsForParent(phone).contains(reg)) {
            sendText(chatId, "Доступ запрещён.");
            return;
        }

        try {
            ClassPathResource res = new ClassPathResource("scans/" + reg + ".pdf");

            if (res.exists() && res.contentLength() > 0) {
                // Файл существует и не пустой → отправляем его
                InputStream stream = res.getInputStream();
                String name = reg + ".pdf";
                String caption = "Скан • " + reg;

                SendDocument doc = new SendDocument();
                doc.setChatId(chatId.toString());
                doc.setDocument(new InputFile(stream, name));
                doc.setCaption(caption);
                execute(doc);
                sendText(chatId, "Готово! Выберите ещё:");
            } else {
                // Файл пустой или отсутствует → сообщаем пользователю
                sendText(chatId, "Скан ещё не готов.");
            }

            showKids(chatId, phone);
        } catch (Exception e) {
            sendText(chatId, "Ошибка: " + e.getMessage());
        }
    }

    private void sendText(Long chatId, String text) {
        SendMessage msg = new SendMessage(chatId.toString(), text);
        send(msg);
    }

    private void send(SendMessage msg) {
        try { execute(msg); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}