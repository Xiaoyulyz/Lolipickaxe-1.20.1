package com.anotherstar.lolipickaxe.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

 
public final class LegacyCardNameCodec {
    private static final Pattern ESCAPE = Pattern.compile("#U([0-9a-fA-F]{4})");

    public static String decode(String value) {
        if (value == null || value.isEmpty()) return "";
        Matcher matcher = ESCAPE.matcher(value);
        StringBuffer out = new StringBuffer(value.length());
        while (matcher.find()) {
            char decoded = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(out, Matcher.quoteReplacement(String.valueOf(decoded)));
        }
        matcher.appendTail(out);
        String text = out.toString();
        if (text.regionMatches(true, Math.max(0, text.length() - 4), ".png", 0, 4)) {
            text = text.substring(0, text.length() - 4);
        }
        return text;
    }

    public static boolean containsEncodedText(String value) {
        return value != null && ESCAPE.matcher(value).find();
    }

    private LegacyCardNameCodec() {}
}
