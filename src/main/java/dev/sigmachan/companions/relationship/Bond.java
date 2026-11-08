package dev.sigmachan.companions.relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Friendship-only bond. Legacy romance flags are deliberately not loaded or exposed. */
public final class Bond {
    public static final int FRIEND = 15;
    public static final int CLOSE = 35;
    public static final int BEST_FRIEND = 70;
    private int trust;
    private boolean aiConsent;
    private long lastGiftDay = Long.MIN_VALUE;
    private int giftsToday;
    private final ArrayList<String> memories = new ArrayList<>();
    private String lastTopic = "";
    private int topicTurns;
    private final ArrayList<String> recentReplies = new ArrayList<>();

    public int trust() { return trust; }
    public void setTrust(int value) { trust = Math.clamp(value, 0, 100); }
    public boolean addTrust(int amount) { int before = trust; setTrust(trust + amount); return before != trust; }
    public boolean aiConsent() { return aiConsent; }
    public void setAiConsent(boolean value) { aiConsent = value; }
    public List<String> memories() { return Collections.unmodifiableList(memories); }
    public void remember(String text) {
        String clean = text.replaceAll("[\\p{Cntrl}]", "").trim();
        if (clean.isEmpty()) return;
        clean = clean.substring(0, Math.min(100, clean.length()));
        if (!memories.contains(clean)) memories.add(clean);
        while (memories.size() > 6) memories.remove(0);
    }
    public void restoreMemory(String text) { remember(text); }
    public void clearMemories() { memories.clear(); }
    public String stage() {
        if (trust >= BEST_FRIEND) return "Лучшие друзья";
        if (trust >= CLOSE) return "Близкие друзья";
        if (trust >= FRIEND) return "Друзья";
        return "Знакомство";
    }
    public boolean canGift(long day) { return day != lastGiftDay || giftsToday < 3; }
    public void recordGift(long day) {
        if (!canGift(day)) throw new IllegalStateException("Daily gift limit reached");
        if (day != lastGiftDay) { lastGiftDay = day; giftsToday = 0; }
        giftsToday++;
    }
    public long lastGiftDay() { return lastGiftDay; }
    public int giftsToday() { return giftsToday; }
    public void restoreGiftCounter(long day, int count) { lastGiftDay = day; giftsToday = Math.clamp(count, 0, 3); }
    public String lastTopic() { return lastTopic; }
    public int topicTurns() { return topicTurns; }
    public void setTopic(String topic) {
        if (topic.equals(lastTopic)) topicTurns++; else { lastTopic = topic; topicTurns = 1; }
    }
    public void restoreTopic(String topic, int turns) { lastTopic = topic == null ? "" : topic.substring(0, Math.min(24, topic.length())); topicTurns = Math.clamp(turns, 0, 99); }
    public java.util.List<String> recentReplies() { return Collections.unmodifiableList(recentReplies); }
    public void rememberReply(String reply) {
        recentReplies.add(reply);
        while (recentReplies.size() > 5) recentReplies.remove(0);
    }
}