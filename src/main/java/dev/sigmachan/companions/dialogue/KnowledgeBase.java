package dev.sigmachan.companions.dialogue;

import java.util.List;
import java.util.Optional;

/** Small, deterministic Minecraft 1.21.1 knowledge base. Facts stay local instead of relying on AI recall. */
public final class KnowledgeBase {
    public record Recipe(String title, String ingredients, int output, String note) {}
    private record Entry(String[] keys, Recipe recipe) {}
    private static final List<Entry> RECIPES = List.of(
            new Entry(new String[]{"доск", "досок", "planks"}, new Recipe("Доски", "1 бревно → 4 доски", 4, "Любое бревно подходит.")),
            new Entry(new String[]{"палк", "палок", "stick"}, new Recipe("Палки", "2 доски вертикально → 4 палки", 4, "Нужны для факелов, инструментов и многого другого.")),
            new Entry(new String[]{"верстак", "crafting table"}, new Recipe("Верстак", "4 доски квадратом 2×2 → 1 верстак", 1, "Открывает сетку крафта 3×3.")),
            new Entry(new String[]{"печь", "furnace"}, new Recipe("Печь", "8 булыжника кольцом → 1 печь", 1, "Верхний центр оставляется пустым.")),
            new Entry(new String[]{"сундук", "chest"}, new Recipe("Сундук", "8 досок кольцом → 1 сундук", 1, "Два рядом ставят двойной сундук.")),
            new Entry(new String[]{"факел", "torch"}, new Recipe("Факелы", "1 уголь или древесный уголь над 1 палкой → 4 факела", 4, "Древесный уголь получается переплавкой брёвен.")),
            new Entry(new String[]{"хлеб", "bread"}, new Recipe("Хлеб", "3 пшеницы в ряд → 1 хлеб", 1, "Пшеницу можно вырастить из семян.")),
            new Entry(new String[]{"печень", "cookie"}, new Recipe("Печенье", "2 пшеницы + 1 какао-боб → 8 печений", 8, "Какао растёт на брёвнах джунглей.")),
            new Entry(new String[]{"торт", "cake"}, new Recipe("Торт", "3 ведра молока + 2 сахара + 1 яйцо + 3 пшеницы → 1 торт", 1, "Торт ставится блоком и естся ломтиками.")),
            new Entry(new String[]{"щит", "shield"}, new Recipe("Щит", "6 досок + 1 железный слиток → 1 щит", 1, "Доски ставятся Y-образно, слиток сверху по центру.")),
            new Entry(new String[]{"кровать", "bed"}, new Recipe("Кровать", "3 шерсти одного цвета + 3 доски → 1 кровать", 1, "Шерсть сверху, доски снизу.")),
            new Entry(new String[]{"ведро", "bucket"}, new Recipe("Ведро", "3 железных слитка буквой V → 1 ведро", 1, "Пригодится для воды, лавы и молока.")),
            new Entry(new String[]{"ножниц", "shears"}, new Recipe("Ножницы", "2 железных слитка по диагонали → 1 ножницы", 1, "Стригут овец и собирают листву.")),
            new Entry(new String[]{"огниво", "flint and steel"}, new Recipe("Огниво", "1 железный слиток + 1 кремень → 1 огниво", 1, "Осторожно с огнём в лесу.")),
            new Entry(new String[]{"компас", "compass"}, new Recipe("Компас", "4 железных слитка + 1 редстоун → 1 компас", 1, "Показывает на точку возрождения мира.")),
            new Entry(new String[]{"рельс", "rail"}, new Recipe("Рельсы", "6 железных слитков + 1 палка → 16 рельсов", 16, "Железо по бокам, палка по центру.")),
            new Entry(new String[]{"воронк", "hopper"}, new Recipe("Воронка", "5 железных слитков + 1 сундук → 1 воронка", 1, "Передаёт предметы в контейнер под собой.")),
            new Entry(new String[]{"поршень", "piston"}, new Recipe("Поршень", "3 доски + 4 булыжника + 1 железо + 1 редстоун → 1 поршень", 1, "Доски сверху, редстоун снизу.")),
            new Entry(new String[]{"стол зачарован", "enchanting table", "enchantment table"}, new Recipe("Стол зачарования", "1 книга + 2 алмаза + 4 обсидиана → 1 стол", 1, "Книжные полки вокруг усиливают чары.")),
            new Entry(new String[]{"наковальн", "anvil"}, new Recipe("Наковальня", "3 железных блока + 4 железных слитка → 1 наковальня", 1, "Блоки сверху, слитки в центре и снизу."))
    );
    private KnowledgeBase() {}

    public static Optional<Recipe> find(String message) {
        String text = DialogueIntent.normalize(message);
        for (Entry entry : RECIPES) for (String key : entry.keys()) if (text.contains(key)) return Optional.of(entry.recipe());
        return Optional.empty();
    }
    public static String craftReply(Recipe recipe) {
        return recipe.title() + ": " + recipe.ingredients() + ". " + recipe.note();
    }

    public static String plan(DialogueContext c) {
        if (c.playerHealth() <= 6) return "Здоровья мало: отойди от опасных мобов, съешь еду и дай себе отдышаться. Не лезь в пещеру прямо сейчас.";
        if (c.nearbyHostiles() > 0 && !c.hasWeapon()) return "Рядом враждебные мобы, а оружия в инвентаре я не вижу. Сначала меч или хотя бы топор и щит, потом бой.";
        if (c.playerFood() <= 6) return "У тебя мало сытости. Пополни запас еды: хлеб, мясо или печенье; в меню меня можно угостить едой из руки.";
        if ((c.night() || c.lightLevel() < 8) && c.torches() == 0) return "Темно, а факелов нет. Сделай факелы: уголь над палкой даёт 4 штуки, или поставь кровать и пережди ночь.";
        if (c.night() && c.home().isBlank()) return "Ночь, а дом не отмечен. Если есть кровать — ложись; если нет, сделай укрытие и освещение. Дом можно отметить кнопкой в меню.";
        if (c.foodItems() == 0) return "В инвентаре не вижу еды. Перед шахтой или боем запасись хлебом/мясом; ферма пшеницы — надёжный старт.";
        if (c.torches() < 8 && c.lightLevel() < 10) return "Факелов маловато для вылазки. Набери хотя бы стак побольше: уголь + палки крафтятся по 4 факела.";
        return "Сейчас можно выбрать спокойную цель: обустроить дом, сделать ферму пшеницы или сходить в шахту с едой и факелами. Я могу следовать рядом.";
    }

    public static String topic(DialogueIntent intent, DialogueContext c) {
        return switch (intent) {
            case MINING -> "Для алмазов в 1.21.1 обычно выгодно искать на Y≈-59, но не копай прямо под себя над лавой. Железная кирка нужна для алмазов, золота и редстоуна; возьми факелы, еду и ведро воды.";
            case COMBAT -> "Против скелетов держи щит и сближайся рывками, против криперов бей и отходи. Щит можно поставить в левую руку; еда помогает восстановлению, если сытость высокая.";
            case FARMING -> "Для фермы нужна мотыга, семена и вода рядом: вода увлажняет грядки в радиусе 4 блоков по горизонтали. Свет нужен и ночью, костная мука ускоряет рост.";
            case BUILDING -> "Начни с простого каркаса, потом стены и крыша. Внутри пригодятся кровать, сундуки, верстак и печь; освещение по углам уберёт тёмные места.";
            case SURVIVAL -> plan(c);
            case PLAN -> plan(c);
            default -> "";
        };
    }
}
