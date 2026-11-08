package dev.sigmachan.companions;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Optional;

/** Non-sexual fan adaptations of the Lucky Star characters. No age-up or romance mechanics. */
public enum CompanionProfile {
    KONATA("konata", "Коната Идзуми", "игры, аниме и шоколадный корнет", 0xFF79A9EE,
            "Я Коната. Если приключение похоже на квест, я уже ищу кнопку «принять».", "minecraft:cookie",
            new String[]{"Этот пейзаж ничего себе, но интерфейс мог бы быть и поудобнее.", "Предлагаю сначала перекус, потом подвиг. Классический порядок.", "Если найдём пещеру, назовём её данжем. Договорились?"}),
    KAGAMI("kagami", "Кагами Хиираги", "учёба, порядок и сладости", 0xFFBE9AEB,
            "Я Кагами. Только давай без бессмысленного риска и потерянных вещей.", "minecraft:book",
            new String[]{"Я не ворчу. Я просто заранее замечаю проблемы.", "Хороший план экономит больше времени, чем кажется.", "Коната опять предложила что-то сомнительное. Надеюсь, ты хотя бы взял факелы."}),
    TSUKASA("tsukasa", "Цукаса Хиираги", "готовка, добрые истории и мечты", 0xFFE4B5DF,
            "Я Цукаса. Я могу немного волноваться, но с друзьями всё не так страшно.", "minecraft:cake",
            new String[]{"Я подумала: если испечь что-нибудь вкусное, всем станет чуть веселее.", "Сегодняшний день похож на начало хорошей истории.", "Я не очень храбрая, но попробую. Только не убегай далеко, ладно?"});

    private final String id, displayName, interests, introduction, favoriteGiftId;
    private final int color;
    private final String[] ambient;
    CompanionProfile(String id, String displayName, String interests, int color, String introduction, String favoriteGiftId, String[] ambient) {
        this.id = id; this.displayName = displayName; this.interests = interests; this.color = color;
        this.introduction = introduction; this.favoriteGiftId = favoriteGiftId; this.ambient = ambient;
    }
    public String id() { return id; }
    public String displayName() { return displayName; }
    public String shortName() { return displayName.split(" ")[0]; }
    public String interests() { return interests; }
    public int color() { return color; }
    public String introduction() { return introduction; }
    public Item favoriteGift() { return Registries.ITEM.get(Identifier.of(favoriteGiftId)); }
    public String ambient(int index) { return ambient[Math.floorMod(index, ambient.length)]; }
    public float scale() { return this == KAGAMI ? 1.03f : this == TSUKASA ? 0.98f : 0.96f; }
    public boolean likes(Item item) {
        String id = Registries.ITEM.getId(item).toString();
        return id.equals(favoriteGiftId) || id.equals("minecraft:cake") || id.equals("minecraft:sweet_berries") || id.equals("sigma_companions:chocolate_cornet");
    }
    public String reaction(String event) {
        return switch (event) {
            case "wave" -> switch (this) { case KONATA -> "О, привет! Заходи в пати, слот как раз свободен."; case KAGAMI -> "Привет. Рада тебя видеть. Что запланируем?"; case TSUKASA -> "П-привет! Я рада, что ты подошёл."; };
            case "high_five" -> switch (this) { case KONATA -> "Отлично! Комбо засчитано!"; case KAGAMI -> "Вот это уже командная работа. Так держать."; case TSUKASA -> "Получилось! Я старалась не промахнуться."; };
            case "hug" -> switch (this) { case KONATA -> "Дружеское объятие принято. Теперь у пати бонус к настроению."; case KAGAMI -> "Только не при всех… Ладно, спасибо. Мне это нужно было."; case TSUKASA -> "Как уютно. Давай так и посидим минутку."; };
            case "pet" -> switch (this) { case KONATA -> "Эй, я не питомец! Хотя… продолжай, это неплохо."; case KAGAMI -> "Причёска же! Ну ладно, аккуратнее."; case TSUKASA -> "Спасибо. От этого как-то спокойнее."; };
            case "snack" -> switch (this) { case KONATA -> "Перекус — важная часть стратегии. Особенно если это корнет."; case KAGAMI -> "Спасибо. Теперь можно нормально заняться делом."; case TSUKASA -> "Мне нравится есть вместе. Так вкуснее."; };
            default -> ambient(0);
        };
    }
    public static Optional<CompanionProfile> byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        // Migrate alpha worlds without losing friendship, gifts or explicit memories.
        normalized = switch (normalized) { case "akari" -> "tsukasa"; case "mio" -> "kagami"; case "rei" -> "konata"; default -> normalized; };
        for (CompanionProfile profile : values()) if (profile.id.equals(normalized)) return Optional.of(profile);
        return Optional.empty();
    }
}