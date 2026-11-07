package dev.sigmachan.companions.relationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure game rules, independent of Minecraft. All mutation happens on the server. */
public final class Bond {
    public static final int FRIEND = 15;
    public static final int CLOSE = 35;
    public static final int ROMANCE = 55;
    public static final int MARRIAGE = 80;
    private int trust;
    private boolean romanceConsent;
    private boolean dating;
    private boolean married;
    private boolean aiConsent;
    private long lastGiftDay = Long.MIN_VALUE;
    private int giftsToday;
    private final ArrayList<String> memories = new ArrayList<>();

    public int trust() { return trust; }
    public void setTrust(int value) { trust = Math.clamp(value, 0, 100); }
    public boolean addTrust(int amount) {
        int before = trust;
        setTrust(trust + amount);
        return before != trust;
    }
    public boolean romanceConsent() { return romanceConsent; }
    public void setRomanceConsent(boolean value) {
        romanceConsent = value;
        if (!value) dating = false;
    }
    public boolean dating() { return dating; }
    public boolean married() { return married; }
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
    public String stage() {
        if (married) return "Семья";
        if (dating) return "Романтика";
        if (trust >= ROMANCE) return "Близкие друзья";
        if (trust >= FRIEND) return "Друзья";
        return "Знакомство";
    }
    public boolean canGift(long day) {
        return day != lastGiftDay || giftsToday < 3;
    }
    public void recordGift(long day) {
        if (!canGift(day)) throw IllegalStateException("Daily gift limit reached");
        if (day != lastGiftDay) { lastGiftDay = day; giftsToday = 0; }
        giftsToday++;
    }
    public long lastGiftDay() { return lastGiftDay; }
    public int giftsToday() { return giftsToday; }
    public void restoreGiftCounter(long day, int count) {
        lastGiftDay = day;
        giftsToday = Math.clamp(count, 0, 3);
    }
    public boolean startDate() {
        if (!romanceConsent || trust < ROMANCE || dating || married) return false;
        dating = true;
        return true;
    }
    public boolean marry() {
        if (!romanceConsent || !dating || trust < MARRIAGE || married) return false;
        married = true;
        return true;
    }
    public void separate() {
        dating = false;
        married = false;
        romanceConsent = false;
        setTrust(Math.min(trust, 40));
    }
}