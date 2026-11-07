package dev.sigmachan.companions.client;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.network.ActionC2S;
import dev.sigmachan.companions.network.CompanionStateS2C;
import dev.sigmachan.companions.relationship.Bond;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
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
    private void sendChat() {
        String text = input.getText().trim();
        if (!text.isEmpty()) {
            addReply("Ты: " + text);
            ClientPlayNetworking.send(new ActionC2S(state.entityId(), "talk", text));
            input.setText("");
        }
    }
    @Override protected void init() {
        int left = left(), top = top();
        String[] tabs = {"Дружба", "Романтика", "Приключение", "Настройки"};
        for (int i = 0; i < tabs.length; i++) {
            int index = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), button -> { tab = index; clearChildren(); actionButtons.clear(); init(); }).dimensions(left + i * 95, top + 34, 92, 18).build());
        }
        input = new TextFieldWidget(textRenderer, left + 10, top + 204, 290, 18, Text.literal("Сообщение"));
        input.setMaxLength(240);
        input.setPlaceholder(Text.literal("Расскажи о своём дне…"));
        addDrawableChild(input);
        addDrawableChild(ButtonWidget.builder(Text.literal("Сказать"), button -> sendChat()).dimensions(left + 305, top + 203, 65, 20).build());
        setInitialFocus(input);
        refreshButtons();
    }
    private void refreshButtons() {
        for (ButtonWidget button : actionButtons) remove(button);
        actionButtons.clear();
        List<ButtonWidget> buttons = new ArrayList<>();
        switch (tab) {
            case 0 -> {
                buttons.add(button("Комплимент", "compliment", 0));
                buttons.add(button("Подарить из руки", "gift", 0));
                buttons.add(button("Обнять", "hug", Bond.FRIEND));
                buttons.add(button("Погладить по голове", "pet", Bond.FRIEND));
                buttons.add(button("Попросить поддержки", "comfort", Bond.CLOSE));
            }
            case 1 -> {
                buttons.add(button(state.consent() ? "Симпатия: да ✓" : "Симпатия: нет", "consent", 0));
                buttons.add(button("Пригласить на свидание", "date", Bond.ROMANCE));
                buttons.add(button("Поцеловать", "kiss", Bond.ROMANCE));
                buttons.add(button("Создать семью · кольцо", "marry", Bond.MARRIAGE));
                buttons.add(button("Расстаться", "separate", 0));
            }
            case 2 -> {
                buttons.add(button("Следовать", "follow", Bond.FRIEND));
                buttons.add(button("Ждать", "stay", Bond.FRIEND));
                buttons.add(button("Гулять", "wander", Bond.FRIEND));
                buttons.add(button("Отметить общий дом", "home", Bond.CLOSE));
            }
            default -> buttons.add(button(state.aiConsent() ? "Внешний ИИ: разрешён" : "Внешний ИИ: запрещён", "ai_consent", 0));
        }
        for (int i = 0; i < buttons.size(); i++) {
            ButtonWidget button = buttons.get(i);
            button.setPosition(left() + 10 + (i % 2) * 182, top() + 57 + (i / 2) * 24);
            actionButtons.add(button); addDrawableChild(button);
        }
    }
    private ButtonWidget button(String label, String action, int trust) {
        boolean allowed = state.trust() >= trust && (state.owner() || action.equals("compliment") || action.equals("gift") || action.equals("hug") || action.equals("pet") || action.equals("comfort") || action.equals("ai_consent"));
        if (action.equals("kiss")) allowed &= state.consent() && state.dating();
        if (action.equals("date") || action.equals("marry")) allowed &= state.consent();
        ButtonWidget widget = ButtonWidget.builder(Text.literal(label), b -> send(action)).dimensions(0, 0, 176, 20).build();
        widget.active = allowed;
        return widget;
    }
    private int left() { return (width - 380) / 2; }
    private int top() { return (height - 232) / 2; }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.isFocused() && (keyCode == 257 || keyCode == 335)) { sendChat(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, width, height, 0xA0101320, 0xC0101320);
        int left = left(), top = top(), y = top + 132;
        CompanionProfile profile = CompanionProfile.byId(state.profile()).orElse(CompanionProfile.AKARI);
        context.fill(left, top, left + 380, top + 232, 0xF0181923);
        context.drawBorder(left, top, 380, 232, profile.color());
        context.drawTextWithShadow(textRenderer, profile.displayName() + " · " + profile.age() + " лет · " + profile.occupation(), left + 10, top + 9, profile.color());
        context.drawTextWithShadow(textRenderer, state.stage() + "  |  Доверие " + state.trust() + "/100", left + 10, top + 22, 0xFFCBCDD9);
        context.fill(left + 240, top + 22, left + 367, top + 25, 0xFF383B48);
        context.fill(left + 240, top + 22, left + 240 + (int)(127 * state.trust() / 100.0), top + 25, profile.color());
        if (tab == 0) {
            var lines = textRenderer.wrapLines(Text.literal(conversation.isEmpty() ? profile.introduction() : conversation.get(conversation.size() - 1)), 356);
            for (int i = 0; i < Math.min(4, lines.size()); i++) context.drawTextWithShadow(textRenderer, lines.get(i), left + 10, y + i * 10, 0xFFE1E4ED);
        } else if (tab == 1) {
            context.drawTextWithShadow(textRenderer, "Только по взаимному согласию. Без откровенных сцен.", left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Свидание: 55 · Семья: 80 + кольцо обещания", left + 10, y + 12, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, state.dating() ? "Вы встречаетесь. Берегите доверие друг друга." : "Сначала станьте близкими друзьями.", left + 10, y + 24, 0xFFBAC6D9);
        } else if (tab == 2) {
            context.drawTextWithShadow(textRenderer, "Дом: " + state.home() + " · режим: " + state.mode(), left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Защита друга: 35+ · Shift+ПКМ: следовать / ждать", left + 10, y + 12, 0xFFBAC6D9);
        } else {
            context.drawTextWithShadow(textRenderer, state.aiAvailable() ? "Внешний ИИ доступен. Разрешение отдельно для каждого друга." : "Локальные реплики. Внешний ИИ настраивает владелец сервера.", left + 10, y, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "Текст уходит провайдеру только с твоего разрешения.", left + 10, y + 12, 0xFFBAC6D9);
            context.drawTextWithShadow(textRenderer, "API может быть платным. Ключи не передаются клиентам.", left + 10, y + 24, 0xFFBAC6D9);
        }
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }
}