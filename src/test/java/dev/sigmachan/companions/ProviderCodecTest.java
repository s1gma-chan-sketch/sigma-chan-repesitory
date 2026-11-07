package dev.sigmachan.companions;

import com.google.gson.JsonParser;
import dev.sigmachan.companions.ai.ProviderCodec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProviderCodecTest {
    @Test void openAIUsesResponsesApiAndDisablesStorage() {
        var body = JsonParser.parseString(ProviderCodec.request("openai", "configured-model", "system", List.of(new ProviderCodec.Message("user", "привет")), "новый вопрос")).getAsJsonObject();
        assertEquals("configured-model", body.get("model").getAsString());
        assertFalse(body.get("store").getAsBoolean());
        assertEquals(2, body.getAsJsonArray("input").size());
        assertFalse(body.has("tools"));
        assertEquals("system", body.get("instructions").getAsString());
    }
    @Test void anthropicSeparatesSystemPrompt() {
        var body = JsonParser.parseString(ProviderCodec.request("anthropic", "configured-model", "system", List.of(), "вопрос")).getAsJsonObject();
        assertEquals("system", body.get("system").getAsString());
        assertEquals(1, body.getAsJsonArray("messages").size());
        assertFalse(body.has("tools"));
    }
    @Test void openAIReadsOnlyTextOutput() {
        String json = "{\"output\":[{\"type\":\"reasoning\",\"content\":[]},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"Привет! \"},{\"type\":\"output_text\",\"text\":\"Как прошёл день?\"}]}]}";
        assertEquals("Привет! Как прошёл день?", ProviderCodec.response("openai", json));
    }
    @Test void anthropicReadsOnlyTextBlocks() {
        assertEquals("Ответ", ProviderCodec.response("anthropic", "{\"content\":[{\"type\":\"thinking\",\"thinking\":\"private\"},{\"type\":\"text\",\"text\":\"Ответ\"}]}"));
    }
    @Test void malformedAndEmptyResponsesFailForLocalFallback() {
        assertThrows(RuntimeException.class, () -> ProviderCodec.response("openai", "{}"));
        assertThrows(RuntimeException.class, () -> ProviderCodec.response("anthropic", "{\"content\":[]}"));
        assertThrows(IllegalArgumentException.class, () -> ProviderCodec.request("unknown", "m", "s", List.of(), "x"));
    }
    @Test void responseLengthIsBounded() {
        String text = "я".repeat(3000);
        String json = "{\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}]}";
        assertEquals(700, ProviderCodec.response("anthropic", json).length());
    }
}