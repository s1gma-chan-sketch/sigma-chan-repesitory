package dev.sigmachan.companions;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Optional;

/** Original adult characters. Ages are immutable; there is no child or breeding state. */
public enum CompanionProfile {
    AKARI("akari", "Акари", 24, "ботаник", 0xFFE995AF,
            "Люблю оранжереи, рассветы и чай с мятой.", "minecraft:poppy",
            new String[]{"Каждый росток напоминает: забота важнее спешки.", "Давай посадим что-нибудь рядом с домом?", "Я принесла семена. Только не наступи на грядки!"}),
    MIO("mio", "Мио", 26, "хранительница историй", 0xFF9CACEE,
            "Собираю истории, люблю книги и тихие вечера.", "minecraft:book",
            new String[]{"У каждого путешествия должна быть глава, которую приятно перечитать.", "В библиотеке всегда найдётся место для ещё одной истории.", "Расскажи, что ты сегодня открыл для себя?"}),
    REI("rei", "Рэй", 23, "механик-изобретатель", 0xFF82D4C5,
            "Чиню механизмы и мечтаю построить мастерскую.", "minecraft:redstone",
            new String[]{"Хороший механизм начинается с хорошего чертежа.", "Если что-то искрит — сначала выключи, потом исследуй!", "У меня есть идея. И, кажется, гаечный ключ."});

    private final String id;
    private final String displayName;
    private final int age;
    private final String occupation;
    private final int color;
    private final String introduction;
    private final String favoriteGiftId;
    private final String[] ambient;

    CompanionProfile(String id, String displayName, int age, String occupation, int color,
                     String introduction, String favoriteGiftId, String[] ambient) {
        this.id = id;
        this.displayName = displayName;
        this.age = age;
        this.occupation = occupation;
        this.color = color;
        this.introduction = introduction;
        this.favoriteGiftId = favoriteGiftId;
        this.ambient = ambient;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public int age() { return age; }
    public String occupation() { return occupation; }
    public int color() { return color; }
    public String introduction() { return introduction; }
    public Item favoriteGift() { return Registries.ITEM.get(Identifier.of(favoriteGiftId)); }
    public String ambient(int index) { return ambient[Math.floorMod(index, ambient.length)]; }
    public boolean likes(Item item) {
        String id = Registries.ITEM.getId(item).toString();
        return id.equals(favoriteGiftId) || id.equals("minecraft:cake") || id.equals("minecraft:sweet_berries");
    }

    public static Optional<CompanionProfile> byId(String id) {
        for (CompanionProfile profile : values()) if (profile.id.equals(id.toLowerCase(Locale.ROOT))) return Optional.of(profile);
        return Optional.empty();
    }
}