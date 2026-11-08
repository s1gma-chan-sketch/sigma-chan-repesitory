package dev.sigmachan.companions.entity;

import net.minecraft.nbt.NbtCompound;

/** Abstract, non-excretory needs. One tick = one real second on the server. */
public final class CompanionNeeds {
    private double satiety = 70, hydration = 80, energy = 90;
    private int pendingNutrition, digestionSeconds;
    private boolean digested;

    public int satiety() { return (int) Math.round(satiety); }
    public int hydration() { return (int) Math.round(hydration); }
    public int energy() { return (int) Math.round(energy); }
    public int digestionSeconds() { return digestionSeconds; }
    public boolean busyDigesting() { return digestionSeconds > 0; }

    public boolean offerSnack(int nutrition, int delaySeconds) {
        if (busyDigesting()) return false;
        pendingNutrition = Math.clamp(nutrition, 8, 36);
        digestionSeconds = Math.clamp(delaySeconds, 5, 600);
        satiety = clamp(satiety + 4);
        return true;
    }
    public void drink() { hydration = clamp(hydration + 35); }
    public void finishRest() { energy = clamp(energy + 20); }

    public void tick(boolean moving) {
        satiety = clamp(satiety - 0.035);
        hydration = clamp(hydration - 0.06);
        energy = clamp(energy - (moving ? 0.08 : 0.025));
        if (digestionSeconds > 0 && --digestionSeconds == 0) {
            satiety = clamp(satiety + pendingNutrition);
            hydration = clamp(hydration - 6);
            energy = clamp(energy - 3);
            pendingNutrition = 0;
            digested = true;
        }
    }
    public boolean consumeDigestedNotice() { boolean value = digested; digested = false; return value; }
    public String hint() {
        if (hydration < 45) return "Я бы выпила воды. Рядом есть источник?";
        if (energy < 35) return "Кажется, пора немного отдохнуть.";
        if (satiety < 35) return "Не откажусь от перекуса, если у тебя есть еда.";
        return null;
    }
    public String summary() {
        return "Сытость " + satiety() + " · Вода " + hydration() + " · Бодрость " + energy()
                + (busyDigesting() ? " · Перекус усваивается: " + digestionSeconds + " с" : "");
    }
    public void write(NbtCompound nbt) {
        nbt.putDouble("Satiety", satiety); nbt.putDouble("Hydration", hydration); nbt.putDouble("Energy", energy);
        nbt.putInt("PendingNutrition", pendingNutrition); nbt.putInt("DigestionSeconds", digestionSeconds);
    }
    public void read(NbtCompound nbt) {
        satiety = nbt.contains("Satiety") ? clamp(nbt.getDouble("Satiety")) : 70;
        hydration = nbt.contains("Hydration") ? clamp(nbt.getDouble("Hydration")) : 80;
        energy = nbt.contains("Energy") ? clamp(nbt.getDouble("Energy")) : 90;
        pendingNutrition = Math.clamp(nbt.getInt("PendingNutrition"), 0, 36);
        digestionSeconds = Math.clamp(nbt.getInt("DigestionSeconds"), 0, 600);
        if (digestionSeconds == 0) pendingNutrition = 0;
        digested = false;
    }
    private static double clamp(double value) { return Math.clamp(value, 0, 100); }
}