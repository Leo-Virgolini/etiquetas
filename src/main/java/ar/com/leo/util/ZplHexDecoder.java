package ar.com.leo.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ZplHexDecoder {

    private static final Pattern HEX_PATTERN = Pattern.compile("_([0-9A-Fa-f]{2})");

    private ZplHexDecoder() {
    }

    public static String decode(String zpl) {
        if (zpl == null || !zpl.contains("_")) {
            return zpl;
        }
        Matcher matcher = HEX_PATTERN.matcher(zpl);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) codePoint)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
