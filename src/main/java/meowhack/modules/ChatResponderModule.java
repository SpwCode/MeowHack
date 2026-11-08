package meowhack.modules;

import meowhack.AddonTemplate;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import com.google.gson.*;

public class ChatResponderModule extends Module {
    private final HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .build();

    public ChatResponderModule() {
        super(AddonTemplate.CATEGORY, "LM ChatBot", "Отвечает на сообщения через LM Studio");
    }

    @EventHandler
    private void onChat(ReceiveMessageEvent event) {
        String message = event.getMessage().getString();

        // Игнорируем системные сообщения и команды
        if (message.startsWith("<") && message.contains(">")) {
            String username = message.substring(message.indexOf("<") + 1, message.indexOf(">"));
            String content = message.substring(message.indexOf(">") + 2);

            // Ответим только на сообщения, не от нас
            String cleanUsername = stripMinecraftFormatting(username);

            if (!cleanUsername.equals(mc.player.getName().getString())) {
                info("Получено сообщение от " + username + ": " + content);
                sendToLMStudio(content);
            }
        }
    }

    private String stripMinecraftFormatting(String text) {
        return text.replaceAll("§.", "");
    }

    private void sendToLMStudio(String userMessage) {
        info("ОТПРАВЛЯЮ ЗАПРОС В LM STUDIO...");

        String json = """
            {
              "model": "google/gemma-3-12b",
              "messages": [
                { "role": "system", "content": "" },
                { "role": "user", "content": "%s" }
              ],
              "temperature": 0.7,
              "max_tokens": 100,
              "stream": false
            }
            """.formatted(userMessage);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:1234/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        // Отправка асинхронно
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenAccept(this::handleResponse)
            .exceptionally(ex -> {
                error("Ошибка при запросе: " + ex.getMessage());
                return null;
            });
    }



    private void handleResponse(String response) {
        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0) {
                error("Пустой список ответов.");
                return;
            }

            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null) {
                error("Ответ не содержит поле message.");
                return;
            }

            String reply = message.get("content").getAsString();
            String cleanedResponse = reply.replace("\n", " ").trim();

            mc.execute(() -> {
                //mc.player.sendMessage(Text.of("[LM Studio] " + reply.trim()), false);
                mc.player.networkHandler.sendChatMessage(Config.get().prefix + "say " +  cleanedResponse.trim());
            });
        } catch (Exception e) {
            error("Ошибка при парсинге JSON: " + e.getMessage());
        }
    }

}
