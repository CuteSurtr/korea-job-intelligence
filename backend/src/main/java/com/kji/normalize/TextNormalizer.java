package com.kji.normalize;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{Punct}\\u00b7\\u2022\\u2013\\u2014]+");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]{1,200}>");
    private static final Pattern MULTI_NEWLINE = Pattern.compile("\\n{3,}");

    private TextNormalizer() {
    }

    public static String compatibilityNormalize(String value) {
        if (value == null) {
            return null;
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }

    public static String collapseWhitespace(String value) {
        if (value == null) {
            return null;
        }
        return WHITESPACE.matcher(value).replaceAll(" ").trim();
    }

    public static String normalizeForMatching(String value) {
        if (value == null) {
            return null;
        }
        String normalized = compatibilityNormalize(value).toLowerCase(Locale.ROOT);
        normalized = PUNCTUATION.matcher(normalized).replaceAll(" ");
        return collapseWhitespace(normalized);
    }

    public static String unescapeHtmlEntities(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    public static String stripHtml(String value) {
        if (value == null) {
            return null;
        }
        String unescapedInput = unescapeHtmlEntities(value);
        String withoutTags = HTML_TAG.matcher(unescapedInput)
                .replaceAll(match -> match.group().matches("(?i)</?(p|br|div|li|tr|h[1-6])[^>]*>") ? "\n" : " ");
        String unescaped = unescapeHtmlEntities(withoutTags);
        String collapsed = MULTI_NEWLINE.matcher(unescaped).replaceAll("\n\n");
        return collapsed.lines()
                .map(line -> WHITESPACE.matcher(line).replaceAll(" ").trim())
                .reduce(new StringBuilder(), (builder, line) -> {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    return builder.append(line);
                }, StringBuilder::append)
                .toString()
                .trim();
    }

    public static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
