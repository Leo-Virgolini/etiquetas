package ar.com.leo.parser;

import ar.com.leo.model.ZplLabel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZplParserTest {

    private final ZplParser parser = new ZplParser();

    @Test
    void parsesSingleLabelWithProductAndDetails() {
        String zpl = "^XA\n^FDTetera Infusora Vidrio^FS\n^FDColor: Transparente | SKU: 1249950^FS\n^XZ";
        List<ZplLabel> labels = parser.parse(zpl);

        assertEquals(1, labels.size());
        assertEquals("1249950", labels.getFirst().sku());
        assertEquals("Tetera Infusora Vidrio", labels.getFirst().productDescription());
        assertEquals("Color: Transparente", labels.getFirst().details());
    }

    @Test
    void parsesMultipleLabels() {
        String zpl = "^XA\n^FDProducto A^FS\n^FDDetalle A | SKU: 111^FS\n^XZ\n"
                + "^XA^MCY^XZ\n"
                + "^XA\n^FDProducto B^FS\n^FDDetalle B | SKU: 222^FS\n^XZ";
        List<ZplLabel> labels = parser.parse(zpl);

        assertEquals(2, labels.size());
        assertEquals("111", labels.get(0).sku());
        assertEquals("222", labels.get(1).sku());
    }

    @Test
    void filtersSeparatorBlocks() {
        String zpl = "^XA^MCY^XZ\n^XA\n^FDNombre^FS\n^FDTest | SKU: 999^FS\n^XZ\n^XA^MCY^XZ";
        List<ZplLabel> labels = parser.parse(zpl);

        assertEquals(1, labels.size());
        assertEquals("999", labels.getFirst().sku());
    }

    @Test
    void handlesLabelWithoutSku() {
        String zpl = "^XA\n^FDSome random content^FS\n^XZ";
        List<ZplLabel> labels = parser.parse(zpl);

        assertEquals(1, labels.size());
        assertNull(labels.getFirst().sku());
    }

    @Test
    void decodesHexInZpl() {
        String zpl = "^XA\n^FDTetera_20Infusora^FS\n^FDColor_3A_20Azul_20_7C_20SKU_3A_201249950^FS\n^XZ";
        List<ZplLabel> labels = parser.parse(zpl);

        assertEquals(1, labels.size());
        assertEquals("1249950", labels.getFirst().sku());
        assertEquals("Tetera Infusora", labels.getFirst().productDescription());
    }

    @Test
    void returnsEmptyForEmptyInput() {
        assertTrue(parser.parse("").isEmpty());
        assertTrue(parser.parse("no zpl content here").isEmpty());
    }
}
