package com.santiagocz.common.text;

public final class TextFormatter {

    // Caracteres después de los cuales empieza una palabra nueva
    private static final String SEPARATORS = " '-";

    private TextFormatter() {
    }

    public static String capitalizeWords(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String collapsed = text.trim().replaceAll("\\s+", " ");

        StringBuilder result = new StringBuilder(collapsed.length());
        boolean startOfWord = true;

        for (char current : collapsed.toCharArray()) {
            result.append(startOfWord
                    ? Character.toUpperCase(current)
                    : Character.toLowerCase(current));
            startOfWord = SEPARATORS.indexOf(current) >= 0;
        }

        return result.toString();
    }
}