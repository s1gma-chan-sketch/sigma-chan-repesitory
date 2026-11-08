package dev.sigmachan.companions.dialogue;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.relationship.Bond;

/** Contextual offline dialogue with per-character voice, topic continuity and anti-repeat selection. */
public final class LocalDialogue {
    private LocalDialogue() {}

    public static String reply(CompanionProfile profile, Bond bond, String message, DialogueContext context) {
        String clean = message.replaceAll("[\\p{Cntrl}]", "").trim();
        String text = DialogueIntent.normalize(clean);
        if (text.startsWith("запомни ")) {
            bond.remember(clean.substring(8));
            return "Запомню: " + bond.memories().get(bond.memories().size() - 1);
        }
        if (text.startsWith("забудь")) {
            bond.clearMemories();
            return "Хорошо, явные воспоминания очищены. Дружба и подарки сохранены.";
        }
        DialogueIntent intent = DialogueIntent.detect(text, bond.lastTopic());
        bond.setTopic(intent.name());
        String reply = switch (intent) {
            case MEMORY -> bond.memories().isEmpty()
                    ? "Пока у нас мало общих воспоминаний. Напиши «запомни …», и я сохраню это."
                    : "Конечно. " + String.join(" ", bond.memories());
            case BOUNDARY -> "Давай останемся друзьями. Я фан-адаптация школьного персонажа, поэтому романтика и откровенные темы здесь недоступны.";
            case WORLD -> worldReply(profile, context);
            case NEEDS -> context.needs().isBlank() ? variants(profile, intent)[0] : context.needs() + ". " + variants(profile, intent)[1];
            case HELP -> "ПКМ или G открывает меню, Shift+ПКМ переключает следовать/ждать. Еду можно передать кнопкой «Угостить», а подарки дарятся из руки.";
            case IDENTITY -> profile.introduction() + " Это фан-адаптация для Minecraft, не официальный персонаж игры.";
            case FEELINGS -> variants(profile, intent)[0] + (context.nearbyCompanions() > 0 ? " Мы не одни, так что можно не спешить." : "");
            default -> choose(profile, bond, intent, context.turn());
        };
        if (intent == DialogueIntent.SMALLTALK && !text.isBlank()) {
            String topic = clean.length() > 60 ? clean.substring(0, 60) + "…" : clean;
            reply = "Про «" + topic + "» я пока знаю мало. " + reply;
        }
        bond.rememberReply(reply);
        return reply;
    }

    private static String worldReply(CompanionProfile profile, DialogueContext context) {
        String time = context.night() ? "ночь" : "день";
        String weather = context.raining() ? ", идёт дождь" : "";
        String held = context.heldItem().isBlank() ? "" : " У тебя в руке " + context.heldItem() + ".";
        return switch (profile) {
            case KONATA -> "Сейчас " + time + ", биом " + context.biome() + weather + ". Выглядит как новая локация для квеста." + held;
            case KAGAMI -> "Вокруг " + context.biome() + ", сейчас " + time + weather + ". " + (context.night() || context.raining() ? "Лучше держаться вместе и проверить освещение." : "Хороший момент, чтобы наметить маршрут.");
            case TSUKASA -> "Мы в биоме " + context.biome() + ", сейчас " + time + weather + ". " + (context.night() ? "Я немного волнуюсь, но рядом с тобой спокойнее." : "Красиво здесь, правда?");
        };
    }

    private static String choose(CompanionProfile profile, Bond bond, DialogueIntent intent, int turn) {
        String[] options = variants(profile, intent);
        int start = Math.floorMod(turn + bond.topicTurns(), options.length);
        for (int i = 0; i < options.length; i++) {
            String candidate = options[(start + i) % options.length];
            if (!bond.recentReplies().contains(candidate)) return candidate;
        }
        return options[start];
    }

    private static String[] variants(CompanionProfile profile, DialogueIntent intent) {
        return switch (profile) {
            case KONATA -> switch (intent) {
                case GREETING -> new String[]{"О, привет! Заходи в пати, слот как раз свободен.", "Привет-привет. Какие квесты на сегодня?", "Хэй! Я уже готова к приключению, осталось найти приключение."};
                case FAVORITES -> new String[]{"Люблю игры, аниме и шоколадные корнеты. Причём порядок именно такой.", "Мой идеальный вечер: кооп, что-нибудь вкусное и ноль домашних заданий.", "Из цветов люблю синий, а из занятий — такие, где можно нажать «старт»."};
                case FOOD -> new String[]{"Шоколадный корнет — это философия, а не просто еда.", "Перекус восстанавливает MP. По крайней мере, мой.", "Если еда вкусная, её стоит есть медленно. Это стратегия."};
                case GAMES -> new String[]{"В любой пещере я вижу данжен. Главное — не забыть факелы.", "Предлагаю квест: найти что-нибудь интересное и не умереть. Награда — история.", "Редстоун похож на чит-код, который нужно собрать самому."};
                case STUDY -> new String[]{"Домашка — это квест с плохим геймдизайном. Но Кагами всё равно заставит меня его сдать.", "Учиться проще, если представить, что прокачиваешь навык.", "Я могу объяснить, но сначала давай сделаем перерыв на корнет."};
                case FEELINGS -> new String[]{"Эй, не держи всё в себе. Я рядом, можем просто посидеть.", "Плохой день — не конец игры. Сохранимся и попробуем ещё раз.", "Давай без геройства: передохни, а потом разберёмся вместе."};
                case NEEDS -> new String[]{"Если полоски просели, пора сделать привал.", "Вода, перекус и отдых — базовый набор выживания."};
                case FAREWELL -> new String[]{"До встречи! Сохранись перед выходом, это важно.", "Пока. В следующий раз продолжим квест.", "Удачи! Я буду ждать новую историю."};
                default -> new String[]{"Хм, звучит как начало интересного квеста. Расскажи подробнее?", "Я не уверена, что поняла всё. Можешь объяснить по-другому?", "Это точно не побочное задание? Расскажи, что ты имеешь в виду."};
            };
            case KAGAMI -> switch (intent) {
                case GREETING -> new String[]{"Привет. Рада тебя видеть. Что запланируем?", "Здравствуй. Надеюсь, ты не забыл факелы.", "Привет. Давай сегодня без сомнительных идей Конаты."};
                case FAVORITES -> new String[]{"Мне нравятся порядок, хорошие книги и… сладости. Но это не значит, что я ем их постоянно.", "Я ценю надёжных друзей и вещи, лежащие на своих местах.", "Из цветов мне близок фиолетовый. И нет, это не потому, что волосы."};
                case FOOD -> new String[]{"Сладости хороши в меру. И лучше после нормального обеда.", "Если берёшь перекус в дорогу, возьми и воду.", "Я не против печенья. Просто не говори об этом Конате."};
                case GAMES -> new String[]{"Сначала план, потом раскопки. Иначе получим яму вместо базы.", "Редстоун требует терпения, зато результат того стоит.", "В пещеру идём с едой, факелами и путём обратно."};
                case STUDY -> new String[]{"Разбей задачу на части и начни с самой понятной. Так не так страшно.", "Книги нравятся мне больше, чем шумные компании. Но с друзьями тоже хорошо.", "Если хочешь, можем вместе разобрать сложную тему. Без насмешек."};
                case FEELINGS -> new String[]{"Я рядом. Не обязательно сейчас всё решать, можно просто выдохнуть.", "Спасибо, что сказал. Давай подумаем, что тебе поможет.", "Это звучит непросто. Я не буду обесценивать твои чувства."};
                case NEEDS -> new String[]{"Проверь воду и бодрость, прежде чем идти дальше.", "Отдых — часть плана, а не потеря времени."};
                case FAREWELL -> new String[]{"До встречи. И проверь инвентарь перед выходом.", "Пока. Возвращайся с целыми вещами и хорошими новостями.", "Удачи. Я присмотрю, чтобы тут не было слишком тихо."};
                default -> new String[]{"Я не хочу делать вид, что поняла. Уточни, о чём речь?", "Звучит интересно, но мне нужно чуть больше подробностей.", "Давай разберёмся спокойно. Что именно ты имеешь в виду?"};
            };
            case TSUKASA -> switch (intent) {
                case GREETING -> new String[]{"П-привет! Я рада, что ты подошёл.", "Здравствуй… Ой, я как раз думала, чем бы нас угостить.", "Привет! С тобой день сразу становится теплее."};
                case FAVORITES -> new String[]{"Я люблю готовить, добрые истории и иногда просто мечтать.", "Мне нравятся нежные цвета и дни, когда никто не ссорится.", "Наверное, больше всего я люблю моменты, когда все смеются вместе."};
                case FOOD -> new String[]{"Если испечь что-нибудь вкусное, всем станет чуть веселее.", "Я могу попробовать приготовить перекус. Главное — не перепутать ингредиенты.", "Еда вкуснее, когда делишься ею с друзьями."};
                case GAMES -> new String[]{"Я не очень храбрая в пещерах, но попробую идти рядом.", "Может, построим маленький сад? Я буду поливать цветы.", "Приключения страшноваты, но с друзьями они становятся историей."};
                case STUDY -> new String[]{"Я иногда путаюсь, но если объяснять медленно, у меня получается.", "Кагами обычно помогает мне с трудными задачами. Она строгая, но добрая.", "Мне нравятся истории, где всё заканчивается хорошо."};
                case FEELINGS -> new String[]{"Мне жаль, что тебе тяжело. Я побуду рядом, хорошо?", "Не нужно торопиться. Давай сделаем маленький шаг вместе.", "Если хочешь, можем просто помолчать. Я не уйду."};
                case NEEDS -> new String[]{"Может, сделаем привал? Я тоже немного устала.", "Вода и отдых помогут. Давай позаботим о себе."};
                case FAREWELL -> new String[]{"Пока… Возвращайся, я буду ждать.", "До встречи! Пусть у тебя всё получится.", "Спокойной ночи. Надеюсь, тебе приснится что-то хорошее."};
                default -> new String[]{"Я немного растерялась… Можешь рассказать ещё раз, чуть подробнее?", "Ой, я не совсем поняла. Но мне правда интересно.", "Прости, я задумалась. О чём именно ты хочешь поговорить?"};
            };
        };
    }
}