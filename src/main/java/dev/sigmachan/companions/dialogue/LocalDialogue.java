package dev.sigmachan.companions.dialogue;

import dev.sigmachan.companions.CompanionProfile;
import dev.sigmachan.companions.relationship.Bond;

import java.util.Locale;

/** Offline, context-sensitive dialogue. Not represented as a neural model. */
public final class LocalDialogue {
    private LocalDialogue() {}

    public static String reply(CompanionProfile profile, Bond bond, String message, String biome, boolean night, int turn) {
        String text = message.toLowerCase(Locale.ROOT).replace('ё', 'е');
        if (text.startsWith("запомни ")) {
            bond.remember(message.substring(8));
            return "Запомню: " + bond.memories().get(bond.memories().size() - 1);
        }
        if (contains(text, "помнишь", "памят", "что я люблю")) {
            return bond.memories().isEmpty() ? "Пока у нас мало общих воспоминаний. Напиши «запомни …», и я сохраню это."
                    : "Конечно. " + String.join(" ", bond.memories());
        }
        if (contains(text, "груст", "плохо", "устал", "одинок", "страшно"))
            return "Не нужно справляться со всем в одиночку. Я рядом. Давай передохнём, а потом сделаем один маленький шаг.";
        if (contains(text, "спасибо", "благодар"))
            return "Мне приятно это слышать. Забота работает в обе стороны.";
        if (contains(text, "люблю", "романт", "свидан", "поцел")) {
            if (bond.married()) return "Мне дорого наше общее путешествие. Давай вернёмся домой и расскажем друг другу о дне.";
            if (bond.dating()) return "Мне хорошо с тобой. Давай не торопиться и узнавать друг друга лучше.";
            return "Сначала давай укрепим доверие. Романтику можно включить отдельно — она не включается сама собой.";
        }
        if (contains(text, "семь", "дом", "брак"))
            return bond.married() ? "Наш дом — место, куда можно вернуться вместе. Отметь его в меню, и я буду возвращаться туда вечером."
                    : "Дом начинается с заботы. Сначала дружба, взаимное согласие и доверие — а потом общие планы.";
        if (contains(text, "цвет", "сад", "растен") || profile == CompanionProfile.AKARI && contains(text, "работ", "увлеч"))
            return "Я берегу семена для нашего сада. Маки — мои любимые. Подарок можно передать прямо из руки.";
        if (contains(text, "книг", "истор", "библиот") || profile == CompanionProfile.MIO && contains(text, "работ", "увлеч"))
            return "Я собираю истории путешественников. Подари мне книгу, и начнём новую главу.";
        if (contains(text, "механ", "редстоун", "чертеж", "изобрет") || profile == CompanionProfile.REI && contains(text, "работ", "увлеч"))
            return "Мечтаю о мастерской с большим столом. Для первого механизма мне пригодится редстоун.";
        if (contains(text, "привет", "здравств", "hello"))
            return "Привет! Я " + profile.displayName() + ", мне " + profile.age() + ". " + profile.introduction();
        if (contains(text, "кто ты", "возраст", "сколько лет"))
            return "Я " + profile.displayName() + ", мне " + profile.age() + " лет. Я " + profile.occupation() + ". Это оригинальный персонаж Sigma Companions.";
        if (contains(text, "иди", "следуй", "за мной"))
            return "Выбери «Следовать» в меню. После уровня дружбы я отправлюсь с тобой; «Ждать» остановит меня.";
        if (contains(text, "ночь", "погод", "где мы"))
            return "Сейчас " + (night ? "ночь" : "светлое время суток") + ", вокруг " + biome + ". " + (night ? "В темноте лучше держаться вместе." : "Хороший момент для прогулки.");
        if (!bond.memories().isEmpty() && turn % 4 == 0)
            return "Я вспомнила: " + bond.memories().get(turn % bond.memories().size()) + ". Как это связано с твоим приключением?";
        if (bond.trust() < Bond.FRIEND)
            return profile.introduction() + " Расскажи о своём путешествии — или просто побудь рядом.";
        return profile.ambient(turn) + (night ? " Вечером я предпочитаю быть ближе к дому." : " Расскажешь, что интересного ты нашёл?");
    }

    private static boolean contains(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}