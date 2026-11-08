package dev.sigmachan.companions;

import dev.sigmachan.companions.dialogue.SafeMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeMathTest {
    @Test void parsesRussianQueryWithPrecedenceAndParentheses() {
        assertEquals(56, SafeMath.fromQuery("Сколько будет 17 * 3 + 5?").orElseThrow());
        assertEquals(14, SafeMath.fromQuery("посчитай (2 + 5) * 2").orElseThrow());
        assertEquals(-3, SafeMath.fromQuery("вычисли 2 - 5").orElseThrow());
    }
    @Test void rejectsUnsafeOrMalformedInput() {
        assertTrue(SafeMath.fromQuery("сколько будет 1/0").isEmpty());
        assertTrue(SafeMath.fromQuery("сколько будет 2+").isEmpty());
        assertTrue(SafeMath.fromQuery("сколько будет Runtime.exec()").isEmpty());
        assertTrue(SafeMath.fromQuery("сколько нужно палок").isEmpty());
        assertTrue(SafeMath.fromQuery("сколько будет 99999999999999999999").isEmpty());
    }
}
