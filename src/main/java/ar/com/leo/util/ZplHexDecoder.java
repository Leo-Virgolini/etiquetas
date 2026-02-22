package ar.com.leo.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ZplHexDecoder {

    // Matchea secuencias contiguas de _XX (una o más)
    private static final Pattern HEX_SEQ_PATTERN = Pattern.compile("(_[0-9A-Fa-f]{2})+");
    private static final Pattern SINGLE_HEX = Pattern.compile("_([0-9A-Fa-f]{2})");

    private ZplHexDecoder() {
    }

    public static String decode(String zpl) {
        if (zpl == null || !zpl.contains("_")) {
            return zpl;
        }
        Matcher seqMatcher = HEX_SEQ_PATTERN.matcher(zpl);
        StringBuilder sb = new StringBuilder();
        while (seqMatcher.find()) {
            String seq = seqMatcher.group();
            Matcher hexMatcher = SINGLE_HEX.matcher(seq);
            byte[] bytes = new byte[seq.length() / 3]; // cada _XX son 3 chars
            int i = 0;
            while (hexMatcher.find()) {
                bytes[i++] = (byte) Integer.parseInt(hexMatcher.group(1), 16);
            }
            String decoded = new String(bytes, 0, i, StandardCharsets.UTF_8);
            seqMatcher.appendReplacement(sb, Matcher.quoteReplacement(decoded));
        }
        seqMatcher.appendTail(sb);
        return sb.toString();
    }
}
