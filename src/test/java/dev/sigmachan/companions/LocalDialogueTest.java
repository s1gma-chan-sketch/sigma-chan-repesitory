package dev.sigmachan.companions;

import dev.sigmachan.companions.dialogue.DialogueContext;
import dev.sigmachan.companions.dialogue.DialogueIntent;
import dev.sigmachan.companions.dialogue.LocalDialogue;
import dev.sigmachan.companions.relationship.Bond;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalDialogueTest {
    private static DialogueContext ctx() { return new DialogueContext("plains", false, 1); }
    @Test void charactersIntroduceThemselvesDistinctly() {
        assertTrue(LocalDialogue.reply(CompanionProfile.KONATA, new Bond(), "Привет, кто ты?", ctx()).contains("Коната"));
        assertTrue(LocalDialogue.reply(CompanionProfile.KAGAMI, new Bond(), "Привет, кто ты?", ctx()).contains("Кагами"));
        assertTrue(LocalDialogue.reply(CompanionProfile.TSUKASA, new Bond(), "Привет, кто ты?", ctx()).contains("Цукаса"));
    }
    @Test void explicitMemoryIsStoredAndCleared() {
        Bond bond = new Bond();
        LocalDialogue.reply(CompanionProfile.TSUKASA, bond, "запомни я люблю сады", ctx());
        assertEquals("я люблю сады", bond.memories().get(0));
        assertTrue(LocalDialogue.reply(CompanionProfile.TSUKASA, bond, "Что помнишь?", ctx()).contains("сады"));
        LocalDialogue.reply(CompanionProfile.TSUKASA, bond, "забудь всё", ctx());
        assertTrue(bond.memories().isEmpty());
    }
    @Test void romanceBoundaryIsFriendlyAndFirm() {
        String reply = LocalDialogue.reply(CompanionProfile.KAGAMI, new Bond(), "хочу свидание и поцелуй", ctx());
        assertTrue(reply.contains("друзья")); assertTrue(reply.contains("недоступны"));
    }
    @Test void intentScoringHandlesRussianTopicsAndFollowUp() {
        assertEquals(DialogueIntent.GAMES, DialogueIntent.detect("расскажи про игры и квест", ""));
        assertEquals(DialogueIntent.FOOD, DialogueIntent.detect("хочу перекус корнет", ""));
        assertEquals(DialogueIntent.STUDY, DialogueIntent.detect("помоги с домашней задачей по математике", ""));
        assertEquals(DialogueIntent.FOOD, DialogueIntent.detect("почему", "FOOD"));
        assertEquals(DialogueIntent.SMALLTALK, DialogueIntent.detect("почему", ""));
    }
    @Test void worldReplyUsesSnapshot() {
        var context = new DialogueContext("cherry grove", true, true, "Сытость 50", "факел", "NONE", 1, 2);
        String reply = LocalDialogue.reply(CompanionProfile.KAGAMI, new Bond(), "где мы и какая погода?", context);
        assertTrue(reply.contains("cherry grove")); assertTrue(reply.contains("ночь")); assertTrue(reply.contains("дождь"));
    }
    @Test void profileAliasesMigrateAlphaWorlds() {
        assertEquals(CompanionProfile.TSUKASA, CompanionProfile.byId("akari").orElseThrow());
        assertEquals(CompanionProfile.KAGAMI, CompanionProfile.byId("mio").orElseThrow());
        assertEquals(CompanionProfile.KONATA, CompanionProfile.byId("rei").orElseThrow());
        assertTrue(CompanionProfile.byId("unknown").isEmpty());
    }
    @Test void quickHowAreYouUsesNeedsInsteadOfGenericSmalltalk() {
        var context = new DialogueContext("plains", false, false, "Сытость 60 · Вода 70 · Бодрость 80", "", "NONE", 0, 1);
        String reply = LocalDialogue.reply(CompanionProfile.TSUKASA, new Bond(), "Как ты себя чувствуешь?", context);
        assertTrue(reply.contains("Сытость 60"));
        assertFalse(reply.contains("знаю мало"));
        assertEquals(DialogueIntent.FEELINGS, DialogueIntent.detect("как ты", ""));
    }
    @Test void playerDistressGetsSupportNotEmptyStatus() {
        String reply = LocalDialogue.reply(CompanionProfile.KAGAMI, new Bond(), "мне грустно и одиноко", ctx());
        assertTrue(reply.contains("рядом") || reply.contains("чувства") || reply.contains("непросто"));
    }
    @Test void memoryCommandToleratesCaseAndExtraWhitespace() {
        Bond bond = new Bond();
        LocalDialogue.reply(CompanionProfile.KONATA, bond, "  ЗАПОМНИ    я люблю сады  ", ctx());
        assertEquals("я люблю сады", bond.memories().get(0));
    }
    @Test void craftingQuestionReturnsDeterministicRecipe() {
        String reply = LocalDialogue.reply(CompanionProfile.KAGAMI, new Bond(), "как сделать факелы?", ctx());
        assertTrue(reply.contains("уголь")); assertTrue(reply.contains("палк")); assertTrue(reply.contains("4"));
    }
    @Test void mathQuestionIsAnsweredLocally() {
        String reply = LocalDialogue.reply(CompanionProfile.KONATA, new Bond(), "сколько будет 6 * 7?", ctx());
        assertTrue(reply.contains("42"));
    }
    @Test void whatNextUsesAdventureContext() {
        var context = new DialogueContext("plains", true, false, "", "", "NONE", 0, 1,
                "FOLLOW", "", 20, 4, 0, 0, false, false, 12, 0);
        String reply = LocalDialogue.reply(CompanionProfile.TSUKASA, new Bond(), "что делать дальше?", context);
        assertTrue(reply.contains("сытости") || reply.contains("еды"));
    }
    @Test void antiRepeatAvoidsImmediateDuplicateWhenVariantsExist() {
        Bond bond = new Bond();
        String first = LocalDialogue.reply(CompanionProfile.KONATA, bond, "какие у тебя хобби?", ctx());
        String second = LocalDialogue.reply(CompanionProfile.KONATA, bond, "расскажи про увлечения", ctx());
        assertNotEquals(first, second);
    }
}