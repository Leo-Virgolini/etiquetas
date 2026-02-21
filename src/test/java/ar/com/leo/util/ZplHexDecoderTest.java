package ar.com.leo.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZplHexDecoderTest {

    @Test
    void decodesHexSequences() {
        String input = "Tetera_20Infusora";
        String result = ZplHexDecoder.decode(input);
        assertEquals("Tetera Infusora", result);
    }

    @Test
    void decodesMultipleHexSequences() {
        String input = "SKU_3A_201234";
        String result = ZplHexDecoder.decode(input);
        assertEquals("SKU: 1234", result);
    }

    @Test
    void returnsNullForNull() {
        assertNull(ZplHexDecoder.decode(null));
    }

    @Test
    void returnsOriginalWhenNoHex() {
        String input = "No hex here";
        assertEquals("No hex here", ZplHexDecoder.decode(input));
    }

    @Test
    void handlesLowercaseHex() {
        String input = "_2f_2F";
        String result = ZplHexDecoder.decode(input);
        assertEquals("//", result);
    }
}
