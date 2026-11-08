package dev.sigmachan.companions.client;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.network.ActionC2S;
import dev.sigmachan.companions.network.CompanionStateS2C;
import dev.sigmachan.companions.relationship.Bond;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class CompanionScreen extends Screen {
    private CompanionStateS2C state;
    private TextFieldWidget input;
    private final List<String> conversation = new ArrayList<>();
    private final List<ButtonWidget> actionButtons = new ArrayList<>();
    private int tab;
    public CompanionScreen(CompanionStateS2C state) { super(Text.literal("Sigma Companions")); this.state = state; }
    public int entityId() { return state.entityId(); }
    public void update(CompanionStateS2C value) {
        state = value;
        if (!value.notice().isEmpty()) addReply(value.notice());
        refreshButtons();
    }
    public void addReply(String reply) {
        conversation.add(reply);
        while (conversation.size() > 12) conversation.remove(0);
    }
    private void send(String action) { ClientPlayNetworking.send(new ActionC2S(state.entityId(), action, "")); }
    private void sendTopic(String topic) {
        addReply("Ты: " + topic);
        ClientPlayNetworking.send(new ActionC2S(state.entityId(), "talk", topic));
    }
    private void sendChat() {
        String text = input.getText().trim();
        if (!text.isEmpty()) { addReply("Ты: " + text); ClientPlayNetworking.send(new ActionC2S(state.entityId(), "talk", text)); input.setText(""); }
    }
    @Override protected void init() {
        int left = left(), top = top();
        String[] tabs = {"Дружба", "Перекус", "Приключение", "ИИ"};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), button -> { tab = index; clearChildren(); actionButtons.clear(); init(); }).dimensions(left + 10 + i * 96, top + 50, 92, 18).build());
        }
        input = new TextFieldWidget(textRenderer, left + 10, top + 236, 300, 18, Text.literal("Сообщение"));
        input.setMaxLength(240); input.setPlaceholder(Text.literal("Спроси о играх, еде, учёбе или настроении…"));
        addDrawableChild(input);
        addDrawableChild(ButtonWidget.builder(Text.literal("Сказать"), button -> sendChat()).dimensions(left + 315, top + 235, 75, 20).build());
        setInitialFocus(input);
        refreshButtons();
    }
    private void refreshButtons() {
        for (ButtonWidget button : actionButtons) remove(button);
        actionButtons.clear();
        List<ButtonWidget> buttons = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                buttons.add(button("Помахать", "wave", 0));
                buttons.add(button("Дать пять", "high_five", 0));
                buttons.add(button("Обнять по-дружески", "hug", Bond.FRIEND));
                buttons.add(button("Погладить по голове", "pet", Bond.FRIEND));
                buttons.add(button("Попросить поддержки", "comfort", Bond.CLOSE));
                buttons.add(button("Подарить из руки", "gift", 0));
            }
            case 1 -> {
                buttons.add(button("Угостить едой из руки", "snack", 0));
                buttons.add(button("Напить водой рядом", "drink", 0));
                buttons.add(button("Отдохнуть 10 секунд", "rest", 0));
            }
            case 2 -> {
                buttons.add(button("Следовать", "follow", Bond.FRIEND));
                buttons.add(button("Ждать", "stay", Bond.FRIEND));
                buttons.add(button("Гулять", "wander", Bond.FRIEND));
                buttons.add(button("Отметить дом", "home", Bond.CLOSE));
            }
            default -> buttons.add(button(state.aiConsent() ? "Внешний ИИ: разрешён" : "Внешний ИИ: запрещён", "ai_consent", 0));
        }
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget button = buttons.get(i);
            button.setPosition(left() + 10 + (i % 2) * 192, top() + 76 + (i / 2) * 24);
            actionButtons.add(button); addDrawableChild(button);
        }
        if (tab == 0) {
            String[] topics = {"Игры", "Еда", "Учёба", "Как ты?"};
            String[] messages = {"Расскажи про игры", "Что любишь есть?", "Поговорим об учёбе?", "Как ты себя чувствуешь?"};
            for (int i = 0; i < topics.length; i++) {
                int index = i;
                ButtonWidget quick = ButtonWidget.builder(Text.literal(topics[i]), b -> sendTopic(messages[index])).dimensions(left() + 10 + i * 96, top() + 152, 92, 18).build();
                actionButtons.add(quick); addDrawableChild(quick);
            }
        }
    }
    private ButtonWidget button(String label, String action, int trust) {
        boolean ownerOnly = action.equals("follow") || action.equals("stay") || action.equals("wander") || action.equals("home");
        boolean allowed = state.trust() >= trust && (!ownerOnly || state.owner());
        ButtonWidget widget = ButtonWidget.builder(Text.literal(label), b -> send(action)).dimensions(0, 0, 186, 20).build();
        widget.active = allowed;
        return widget;
    }
    private int left() { return (width - 400) / 2; }
    private int top() { return (height - 272) / 2; }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.isFocused() && (keyCode == 257 || keyCode == 335)) { sendChat(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xA0101320, 0xC0101320);
        int left = left(), top = top();
        CompanionProfile profile = CompanionProfile.byId(state.profile()).orElse(CompanionProfile.KONATA);
        context.fill(left, top, left + 400, top + 272, 0xF0181923);
        context.drawBorder(left, top, 400, 272, profile.color());
        context.drawTextWithShadow(textRenderer, profile.displayName() + " · " + profile.interests(), left + 10, top + 8, profile.color());
        context.drawTextWithShadow(textRenderer, state.stage() + "  |  Доверие " + state.trust() + "/100", left + 10, top + 22, 0xFFCBCDD9);
        context.fill(left + 250, top + 24, left + 388, top + 27, 0xFF383B48);
        context.fill(left + 250, top + 24, left + 250 + (int) (138 * state.trust() / 100.0), top + 27, profile.color());
        context.drawTextWithShadow(textRenderer, "Режим: " + modeName() + " · " + activityName(), left + 10, top + 36, 0xFF9AA4B8);
        int y = top + 178;
        if (tab == 0) {
            var lines = textRenderer.wrapLines(Text.literal(conversation.isEmpty() ? profile.introduction() : conversation.get(conversation.size() - 1)), 376);
            for (int i = 0; i < Math.min(4, lines.size()); i++) context.drawTextWithShadow(textRenderer, lines.get(i), left + 10, y + i * 10, 0xFFE1E4ED);
        } else if (tab == 1) {
            bar(context, "Сытость", state.satiety(), 0xFFE0B26A, left + 10, top + 150);
            bar(context, "Вода", state.hydration(), 0xFF6FB7E8, left + 140, top + 150);
            bar(context, "Бодрость", state.energy(), 0xFF9BD18A, left + 270, top + 150);
            String digestion = state.digestion() > 0 ? "Перекус усваивается: " + state.digestion() + " с" : "Перекус можно предложить любой едой из руки.";
            context.drawTextWithShadow(textRenderer, digestion, left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Вода нужна рядом со спутницей; отдых восстанавливает бодрость.", left + 10, y + 12, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Потребности абстрактные: сытость, вода, бодрость. Без туалета.", left + 10, y + 24, 0xFF8E99AD);
        } else if (tab == 2) {
            context.drawTextWithShadow(textRenderer, "Дом: " + state.home(), left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Защита друга: 35+ · Shift+ПКМ: следовать / ждать", left + 10, y + 12, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Вечером в режиме прогулки спутница возвращается домой.", left + 10, y + 24, 0xFFBAC6D9);
        } else {
            context.drawTextWithShadow(textRenderer, state.aiAvailable() ? "Внешний ИИ доступен. Разрешение отдельно для каждого друга." : "Локальный диалог. Внешний ИИ настраивает владелец сервера.", left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Текст и игровой контекст уходят провайдеру только с разрешения.", left + 10, y + 12, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "API может быть платным. Ключи не передаются клиентам.", left + 10, y + 24, 0xFFBAC6D9);
        }
    }
    private void bar(DrawContext context, String label, int value, int color, int x, int y) {
        context.drawTextWithShadow(textRenderer, label + " " + value, x, y, 0xFFCBCDD9);
        context.fill(x, y + 12, x + 118, y + 16, 0xFF383B48);
        context.fill(x, y + 12, x + (int) (118 * Math.clamp(value, 0, 100) / 100.0), y + 16, color);
    }
    private String modeName() { return switch (state.mode()) { case "FOLLOW" -> "следует"; case "STAY" -> "ждёт"; default -> "гуляет"; }; }
    private String activityName() {
        return switch (state.activity()) { case "WAVE" -> "машет"; case "HUG" -> "обнимает"; case "PET" -> "реакция на поглаживание"; case "COMFORT" -> "поддерживает"; case "HIGH_FIVE" -> "даёт пять"; case "EAT" -> "ест"; case "DRINK" -> "пьёт"; case "REST" -> "отдыхает"; default -> "свободна"; };
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) { super.render(context, mouseX, mouseY, delta); }
}