package com.example.childhomeworktracker;

import com.example.childhomeworktracker.telegram.ChildhomeworkTrackerBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class ChildhomeworkTrackerBotApplication implements CommandLineRunner {

    @Autowired
    private ChildhomeworkTrackerBot childhomeworkTrackerBot;

    public static void main(String[] args) {
        SpringApplication.run(ChildhomeworkTrackerBotApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(childhomeworkTrackerBot);
            System.out.println("✅ Bot registered successfully and running!");
        } catch (TelegramApiException e) {
            // Игнорируем стандартную ошибку 404 при удалении webhook
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                System.out.println("⚠️ No webhook to remove — switching to long polling mode...");
            } else {
                e.printStackTrace();
            }
        }
    }
}
