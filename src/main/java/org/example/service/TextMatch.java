package org.example.service;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Comparación léxica de nombres (sin dependencias): normalización y similitud entre 0 y 1. */
public final class TextMatch {

    private TextMatch() {}

    /** Minúsculas, sin tildes, sin el sufijo "(s)", solo letras y dígitos separados por un espacio. */
    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).replace("(s)", " ").replaceAll("[^a-z0-9]+", " ").trim();
    }

    /**
     * 1.0 si son iguales tras normalizar; 0.9 si uno contiene al otro como palabra o prefijo de palabra;
     * en otro caso, coeficiente de Dice sobre bigramas de caracteres.
     */
    public static double similarity(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1.0;
        }
        if (containsAsWords(x, y) || containsAsWords(y, x)) {
            return 0.9;
        }
        return dice(x, y);
    }

    private static boolean containsAsWords(String outer, String inner) {
        return (" " + outer).contains(" " + inner);
    }

    private static double dice(String x, String y) {
        Map<String, Integer> bx = bigrams(x);
        Map<String, Integer> by = bigrams(y);
        int total = 0;
        int common = 0;
        for (int v : bx.values()) {
            total += v;
        }
        for (int v : by.values()) {
            total += v;
        }
        for (var e : bx.entrySet()) {
            common += Math.min(e.getValue(), by.getOrDefault(e.getKey(), 0));
        }
        return total == 0 ? 0 : 2.0 * common / total;
    }

    private static Map<String, Integer> bigrams(String s) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i + 1 < s.length(); i++) {
            m.merge(s.substring(i, i + 2), 1, Integer::sum);
        }
        return m;
    }
}
