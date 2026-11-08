package dev.sigmachan.companions.dialogue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Lightweight Russian/English intent scoring. Deterministic, offline, testable; not an LLM. */
public enum DialogueIntent {
    GREETING, IDENTITY, FAVORITES, FOOD, GAMES, STUDY, HELP, FEELINGS, NEEDS, WORLD, MEMORY, FAREWELL, BOUNDARY, SMALLTALK;

    public static String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[^\\p{L}\\p{N}?]+", " ").trim();
    }
    public static boolean isQuestion(String normalized) {
        return normalized.contains("?") || normalized.matches("^(кто|что|где|когда|почему|зачем|как|какой|какая|какие|сколько|расскажи).*");
    }
    public static DialogueIntent detect(String normalized, String lastTopic) {
        if (normalized.matches("^(запомни|забудь)\\b.*")) return MEMORY;
        if (containsAny(normalized, "романт", "свидан", "поцел", "брак", "свадьб", "секс", "интим", "совершеннолет", "люблю тебя", "туалетн", "экскретор", "фекал", "мочеиспуск", "дефекац")) return BOUNDARY;
        if (!lastTopic.isBlank() && (normalized.equals("почему") || normalized.equals("зачем") || normalized.equals("а ты") || normalized.startsWith("а ты ") || normalized.startsWith("расскажи еще") || normalized.equals("еще") || normalized.startsWith("продолжи") || normalized.equals("правда"))) {
            try { return valueOf(lastTopic); } catch (IllegalArgumentException ignored) {}
        }
        Set<String> tokens = new HashSet<>(Arrays.asList(normalized.split(" ")));
        int bestScore = 0; DialogueIntent best = SMALLTALK;
        for (DialogueIntent intent : values()) {
            int score = 0;
            for (String stem : stems(intent)) {
                if (stem.indexOf(' ') >= 0) { if (normalized.contains(stem)) score += 3; }
                else if (tokens.contains(stem)) score += 2;
                else if (stem.length() >= 3 && normalized.contains(stem)) score += 1;
            }
            if (score > bestScore) { bestScore = score; best = intent; }
        }
        return best;
    }
    private static boolean containsAny(String text, String... stems) {
        for (String stem : stems) if (text.contains(stem)) return true;
        return false;
    }
    private static String[] stems(DialogueIntent intent) {
        return switch (intent) {
            case GREETING -> new String[]{"привет", "здравств", "добрый день", "доброе утро", "хай", "hello", "hi"};
            case IDENTITY -> new String[]{"кто ты", "представ", "имя", "возраст", "сколько лет", "кто вы"};
            case FAVORITES -> new String[]{"любим", "нравишься", "увлеч", "хобби", "favorite", "цвет", "интерес"};
            case FOOD -> new String[]{"еда", "перекус", "есть", "голод", "корнет", "сладк", "торт", "печен", "готовк", "чай", "вода", "пить", "кушать", "food"};
            case GAMES -> new String[]{"игр", "квест", "данж", "майнкрафт", "редстоун", "приключ", "пещер", "моб", "ферм", "строит", "блок", "данжен", "minecraft"};
            case STUDY -> new String[]{"учен", "школ", "домашн", "задач", "книг", "экзамен", "урок", "математик", "учеб", "истор", "библиот"};
            case HELP -> new String[]{"управлен", "кнопк", "команд", "как играть", "меню", "следов", "режим", "помощ", "подарок", "дарить", "help"};
            case FEELINGS -> new String[]{"груст", "плохо", "одинок", "страшно", "тревог", "боюсь", "поддерж", "обн", "расстро", "скучно", "устал", "как ты", "как дела", "чувств", "настроени", "самочувств", "how are you"};
            case NEEDS -> new String[]{"сыт", "жажд", "отдых", "поспать", "энерг", "бодр", "устала", "хочу пить", "голодна"};
            case WORLD -> new String[]{"биом", "погод", "ночь", "дождь", "где мы", "время", "день", "закат", "рассвет"};
            case MEMORY -> new String[]{"помнишь", "памят", "воспомин", "что я люблю", "запомни", "забудь"};
            case FAREWELL -> new String[]{"пока", "до встречи", "спокойной ночи", "bye", "прощай"};
            default -> new String[0];
        };
    }
}