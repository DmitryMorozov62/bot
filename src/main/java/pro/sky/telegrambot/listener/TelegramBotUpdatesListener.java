package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;
import pro.sky.telegrambot.service.TelegramBotService;

import javax.annotation.PostConstruct;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private static final Pattern PATTERN = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");

    private static final DateTimeFormatter DATA_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");


    private final TelegramBot telegramBot;

    private final TelegramBotService telegramBotService;

    private final NotificationTaskService notificationTaskService;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, TelegramBotService telegramBotService, NotificationTaskService notificationTaskService) {
        this.telegramBot = telegramBot;
        this.telegramBotService = telegramBotService;
        this.notificationTaskService = notificationTaskService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            // Process your updates here
            Long chatId = update.message().chat().id();
            Message message = update.message();
            String text = message.text();;
            LocalDateTime dateTime;
            if (update.message() != null && text != null) {
                Matcher matcher = PATTERN.matcher(text);
                if (text.equals("/start")) {
                    telegramBotService.sendMessage(chatId, "Здравствуй, " + update.message().chat().firstName() +
                            "! Для того, что бы создать напоминание, напечатай сообщение по шаблону:\n" +
                            "01.01.2022 20:00 Сделать домашнюю работу");
                } else if (matcher.matches() && (dateTime = parse(matcher.group(1))) != null) {
                    notificationTaskService.saveMessage(chatId, matcher.group(3), dateTime);
                    telegramBotService.sendMessage(chatId, "напоминание установленно");
                } else {
                    telegramBotService.sendMessage(chatId, "Формат сообщения не верный");
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Nullable
    private LocalDateTime parse(String dateTime) {
        try {
            return LocalDateTime.parse(dateTime, DATA_TIME_FORMATTER);
        } catch (DateTimeException e) {
            return null;
        }
    }
}
