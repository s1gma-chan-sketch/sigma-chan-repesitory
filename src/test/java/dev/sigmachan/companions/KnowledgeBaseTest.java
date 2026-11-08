package dev.sigmachan.companions;

import dev.sigmachan.companions.dialogue.DialogueContext;
import dev.sigmachan.companions.dialogue.DialogueIntent;
import dev.sigmachan.companions.dialogue.KnowledgeBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseTest {
    @Test void findsCommonRecipesWithoutNetwork() {
        assertTrue(KnowledgeBase.find("как сделать факелы").orElseThrow().output() == 4);
        assertTrue(KnowledgeBase.find("рецепт хлеба").orElseThrow().ingredients().contains("пшеницы"));
        assertTrue(KnowledgeBase.find("сколько досок из бревна").orElseThrow().output() == 4);
        assertTrue(KnowledgeBase.find("как скрафтить щит").isPresent());
    }
    @Test void planPrioritisesHealthOverExploration() {
        var context = new DialogueContext("cave", true, true, "", "", "NONE", 0, 1,
                "FOLLOW", "", 4, 3, 0, 0, false, false, 2, 3);
        String plan = KnowledgeBase.plan(context);
        assertTrue(plan.contains("Здоровья мало"));
        assertFalse(plan.contains("шахту с едой"));
    }
    @Test void planSuggestsTorchesWhenDarkAndEmpty() {
        var context = new DialogueContext("cave", true, false, "", "", "NONE", 0, 1,
                "FOLLOW", "", 20, 20, 0, 4, true, true, 3, 0);
        assertTrue(KnowledgeBase.plan(context).contains("факел"));
    }
    @Test void miningAdviceMentionsDepthAndPickaxe() {
        String advice = KnowledgeBase.topic(DialogueIntent.MINING, new DialogueContext("plains", false, 1));
        assertTrue(advice.contains("-59")); assertTrue(advice.contains("кирка"));
    }
}
