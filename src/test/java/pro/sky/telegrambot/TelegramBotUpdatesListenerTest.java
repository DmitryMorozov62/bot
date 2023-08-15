package pro.sky.telegrambot;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.listener.TelegramBotUpdatesListener;
import pro.sky.telegrambot.service.NotificationTaskService;
import pro.sky.telegrambot.service.TelegramBotService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class TelegramBotUpdatesListenerTest {

    private final TelegramBot telegramBot = Mockito.mock(TelegramBot.class);
    private final NotificationTaskService notificationTaskService = Mockito.mock(
            NotificationTaskService.class);

    @InjectMocks
    private TelegramBotUpdatesListener telegramBotUpdatesListener = new TelegramBotUpdatesListener(
            telegramBot,
            new TelegramBotService(telegramBot),
            notificationTaskService);

    @BeforeEach
    public void beforeEach() {
        Mockito.when(telegramBot.execute(any())).thenReturn(
                BotUtils.fromJson(
                        """
                            {
                              "ok": true
                            }
                            """, SendResponse.class)
        );
    }

    @Test
    public void handleStartTest() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "/start");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo( "Здравствуй, " + update.message().chat().firstName() +
                "! Для того, что бы создать напоминание, напечатай сообщение по шаблону:\n" +
                "01.01.2022 20:00 Сделать домашнюю работу");
    }

    @Test
    public void handleInvalidMessage() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "asdfasd");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(
                "Формат сообщения неверный!");
    }

    @Test
    public void handleValidMessage() throws URISyntaxException, IOException {
        String json = Files.readString(
                Paths.get(TelegramBotUpdatesListenerTest.class.getResource("text_update.json").toURI()));
        Update update = getUpdate(json, "31.12.2022 20:00 сделать домашнюю работу");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> sendMessageArgumentCaptor = ArgumentCaptor.forClass(
                SendMessage.class);
        Mockito.verify(telegramBot).execute(sendMessageArgumentCaptor.capture());
        SendMessage actualSendMessage = sendMessageArgumentCaptor.getValue();

        ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor = ArgumentCaptor.forClass(
                LocalDateTime.class);
        ArgumentCaptor<String> stringTimeArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> longArgumentCaptor = ArgumentCaptor.forClass(Long.class);
        Mockito.verify(notificationTaskService).saveMessage(
                longArgumentCaptor.capture(),
                stringTimeArgumentCaptor.capture(),
                localDateTimeArgumentCaptor.capture()
        );
        LocalDateTime actualLocalDateTime = localDateTimeArgumentCaptor.getValue();
        String actualString = stringTimeArgumentCaptor.getValue();
        Long actualLong = longArgumentCaptor.getValue();

        Assertions.assertThat(actualLocalDateTime)
                .isEqualTo(LocalDateTime.of(2022, Month.DECEMBER, 31, 20, 0));
        Assertions.assertThat(actualString).isEqualTo("сделать домашнюю работу");
        Assertions.assertThat(actualLong).isEqualTo(123L);

        Assertions.assertThat(actualSendMessage.getParameters().get("chat_id")).isEqualTo(123L);
        Assertions.assertThat(actualSendMessage.getParameters().get("text")).isEqualTo(
                "напоминание установленно");
    }

    private Update getUpdate(String json, String replaced) {
        return BotUtils.fromJson(json.replace("%text%", replaced), Update.class);
    }
}