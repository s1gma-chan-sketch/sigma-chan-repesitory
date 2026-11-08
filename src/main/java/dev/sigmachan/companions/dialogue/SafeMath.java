package dev.sigmachan.companions.dialogue;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tiny integer arithmetic parser. No scripting/eval, bounded input, division by zero is rejected. */
public final class SafeMath {
    private static final Pattern QUERY = Pattern.compile("(?iu)(?:сколько(?:\\s+будет)?|посчитай|вычисли)\\s*[:=]?\\s*([0-9+\\-*/()\\s]{1,80})");
    private final String input;
    private int pos;
    private SafeMath(String input) { this.input = input; }

    public static OptionalLong fromQuery(String text) {
        Matcher m = QUERY.matcher(text);
        if (!m.find()) return OptionalLong.empty();
        String expr = m.group(1).trim();
        if (expr.isEmpty() || !expr.matches("[0-9+\\-*/()\\s]+")) return OptionalLong.empty();
        try {
            SafeMath parser = new SafeMath(expr);
            long value = parser.expression();
            parser.skip();
            if (parser.pos != parser.input.length()) return OptionalLong.empty();
            return OptionalLong.of(value);
        } catch (RuntimeException e) { return OptionalLong.empty(); }
    }
    private void skip() { while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) pos++; }
    private long expression() {
        long value = term();
        while (true) {
            skip();
            if (consume('+')) value = Math.addExact(value, term());
            else if (consume('-')) value = Math.subtractExact(value, term());
            else return value;
        }
    }
    private long term() {
        long value = factor();
        while (true) {
            skip();
            if (consume('*')) value = Math.multiplyExact(value, factor());
            else if (consume('/')) { long divisor = factor(); if (divisor == 0) throw new ArithmeticException(); value /= divisor; }
            else return value;
        }
    }
    private long factor() {
        skip();
        if (consume('(')) { long value = expression(); skip(); if (!consume(')')) throw new IllegalArgumentException(); return value; }
        if (consume('-')) return Math.negateExact(factor());
        long value = 0; int digits = 0;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            value = Math.addExact(Math.multiplyExact(value, 10), input.charAt(pos++) - '0');
            if (++digits > 12) throw new IllegalArgumentException();
        }
        if (digits == 0) throw new IllegalArgumentException();
        return value;
    }
    private boolean consume(char c) { skip(); if (pos < input.length() && input.charAt(pos) == c) { pos++; return true; } return false; }
}
