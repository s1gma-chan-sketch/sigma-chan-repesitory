package dev.sigmachan.companions.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

/** Request/response format isolated for unit tests; no credentials or network in this class. */
public final class ProviderCodec {
    public record Message(String role, String text) {}
    private ProviderCodec() {}

    public static String request(String provider, String model, String system, List<Message> history, String input) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        JsonArray messages = new JsonArray();
        for (Message message : history) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.addProperty("content", message.text());
            messages.add(item);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", input);
        messages.add(user);
        if (provider.equals("openai")) {
            root.addProperty("instructions", system);
            root.add("input", messages);
            root.addProperty("max_output_tokens", 1024);
            root.addProperty("store", false);
            JsonObject reasoning = new JsonObject();
            reasoning.addProperty("effort", "low");
            root.add("reasoning", reasoning);
        } else if (provider.equals("anthropic")) {
            root.addProperty("system", system);
            root.addProperty("max_tokens", 300);
            root.add("messages", messages);
        } else throw new IllegalArgumentException("Unknown provider");
        return root.toString();
    }

    public static String response(String provider, String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        StringBuilder result = new StringBuilder();
        if (provider.equals("openai")) {
            if (!root.has("output") || !root.get("output").isJsonArray()) throw new IllegalArgumentException("Missing output");
            for (var element : root.getAsJsonArray("output")) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                if (!item.has("content") || !item.get("content").isJsonArray()) continue;
                for (var part : item.getAsJsonArray("content")) {
                    if (part.isJsonObject() && part.getAsJsonObject().has("text") && part.getAsJsonObject().get("text").isJsonPrimitive())
                        result.append(part.getAsJsonObject().get("text").getAsString());
                }
            }
        } else if (provider.equals("anthropic")) {
            if (!root.has("content") || !root.get("content").isJsonArray()) throw new IllegalArgumentException("Missing content");
            for (var element : root.getAsJsonArray("content")) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                if (item.has("type") && item.get("type").getAsString().equals("text") && item.has("text") && item.get("text").isJsonPrimitive())
                    result.append(item.get("text").getAsString());
            }
        } else throw new IllegalArgumentException("Unknown provider");
        String clean = result.toString().replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Empty model response");
        return clean.substring(0, Math.min(700, clean.length()));
    }
}