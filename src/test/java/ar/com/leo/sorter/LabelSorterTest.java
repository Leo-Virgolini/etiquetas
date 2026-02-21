package ar.com.leo.sorter;

import ar.com.leo.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LabelSorterTest {

    private final LabelSorter sorter = new LabelSorter();

    @Test
    void sortsLabelsByZoneThenSku() {
        List<ZplLabel> labels = List.of(
                new ZplLabel("zpl1", "200", "Producto B", null),
                new ZplLabel("zpl2", "100", "Producto A", null),
                new ZplLabel("zpl3", "300", "Producto C", null),
                new ZplLabel("zpl4", "150", "Producto D", null)
        );

        Map<String, String> mapping = Map.of(
                "200", "T1",
                "100", "J1",
                "300", "J1",
                "150", "T1"
        );

        SortResult result = sorter.sort(labels, mapping);

        assertEquals(4, result.groups().size());
        assertEquals("J1", result.groups().get(0).zone());
        assertEquals("100", result.groups().get(0).sku());
        assertEquals("J1", result.groups().get(1).zone());
        assertEquals("300", result.groups().get(1).sku());
        assertEquals("T1", result.groups().get(2).zone());
        assertEquals("150", result.groups().get(2).sku());
        assertEquals("T1", result.groups().get(3).zone());
        assertEquals("200", result.groups().get(3).sku());
    }

    @Test
    void groupsDuplicateSkus() {
        List<ZplLabel> labels = List.of(
                new ZplLabel("zpl1", "100", "Producto A", null),
                new ZplLabel("zpl2", "100", "Producto A", null),
                new ZplLabel("zpl3", "200", "Producto B", null)
        );

        Map<String, String> mapping = Map.of("100", "J1", "200", "J2");
        SortResult result = sorter.sort(labels, mapping);

        assertEquals(2, result.groups().size());
        assertEquals(2, result.groups().get(0).count());
        assertEquals(1, result.groups().get(1).count());
    }

    @Test
    void unmappedSkusGoToUnknown() {
        List<ZplLabel> labels = List.of(
                new ZplLabel("zpl1", "100", "Producto A", null),
                new ZplLabel("zpl2", "999", "Desconocido", null)
        );

        Map<String, String> mapping = Map.of("100", "J1");
        SortResult result = sorter.sort(labels, mapping);

        assertEquals(2, result.groups().size());
        assertEquals("J1", result.groups().get(0).zone());
        assertEquals("???", result.groups().get(1).zone());
    }

    @Test
    void calculatesStatistics() {
        List<ZplLabel> labels = List.of(
                new ZplLabel("zpl1", "100", "A", null),
                new ZplLabel("zpl2", "100", "A", null),
                new ZplLabel("zpl3", "200", "B", null),
                new ZplLabel("zpl4", null, null, null)
        );

        Map<String, String> mapping = Map.of("100", "J1", "200", "T2");
        SortResult result = sorter.sort(labels, mapping);
        LabelStatistics stats = result.statistics();

        assertEquals(4, stats.totalLabels());
        assertEquals(2, stats.countByZone().get("J1"));
        assertEquals(1, stats.countByZone().get("T2"));
        assertEquals(1, stats.countByZone().get("???"));
        assertEquals(2, stats.uniqueSkus());
        assertEquals(1, stats.unmappedLabels());
    }

    @Test
    void sortedFlatListReturnsInOrder() {
        List<ZplLabel> labels = List.of(
                new ZplLabel("T1_zpl", "200", "B", null),
                new ZplLabel("J1_zpl", "100", "A", null)
        );

        Map<String, String> mapping = Map.of("100", "J1", "200", "T1");
        SortResult result = sorter.sort(labels, mapping);

        List<ZplLabel> flat = result.sortedFlatList();
        assertEquals("J1_zpl", flat.get(0).rawZpl());
        assertEquals("T1_zpl", flat.get(1).rawZpl());
    }
}
