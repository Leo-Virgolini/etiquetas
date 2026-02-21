package ar.com.leo.sorter;

import ar.com.leo.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class LabelSorter {

    private static final String UNKNOWN = "???";

    private static final List<String> ZONE_ORDER = List.of(
            "J1", "J2", "J3", "T1", "T2", "T3", "CARROS", "RETIROS"
    );

    private String zoneBase(String zone) {
        int dash = zone.indexOf('-');
        return dash > 0 ? zone.substring(0, dash).toUpperCase() : zone.toUpperCase();
    }

    private String zoneSuffix(String zone) {
        int dash = zone.indexOf('-');
        return dash > 0 ? zone.substring(dash + 1).toUpperCase() : "";
    }

    private int zoneBaseIndex(String zone) {
        String base = zoneBase(zone);
        for (int i = 0; i < ZONE_ORDER.size(); i++) {
            if (ZONE_ORDER.get(i).equals(base)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    public SortResult sort(List<ZplLabel> labels, Map<String, String> skuToZone) {
        Map<String, List<ZplLabel>> grouped = labels.stream()
                .collect(Collectors.groupingBy(
                        l -> resolveZone(l.sku(), skuToZone) + "|" + (l.sku() != null ? l.sku() : ""),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SortedLabelGroup> groups = grouped.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    String zone = parts[0];
                    String sku = parts.length > 1 ? parts[1] : "";
                    String desc = entry.getValue().stream()
                            .map(ZplLabel::productDescription)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("");
                    String details = entry.getValue().stream()
                            .map(ZplLabel::details)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("");
                    return new SortedLabelGroup(zone, sku, desc, details, entry.getValue());
                })
                .sorted(Comparator
                        .<SortedLabelGroup>comparingInt(g -> zoneBaseIndex(g.zone()))
                        .thenComparing(g -> zoneSuffix(g.zone()))
                        .thenComparing(g -> {
                            try {
                                return Long.parseLong(g.sku());
                            } catch (NumberFormatException e) {
                                return Long.MAX_VALUE;
                            }
                        }))
                .toList();

        LabelStatistics stats = buildStatistics(labels, skuToZone);
        return new SortResult(groups, stats);
    }

    private String resolveZone(String sku, Map<String, String> skuToZone) {
        if (sku == null || sku.isEmpty()) {
            return UNKNOWN;
        }
        return skuToZone.getOrDefault(sku, UNKNOWN);
    }

    private LabelStatistics buildStatistics(List<ZplLabel> labels, Map<String, String> skuToZone) {
        int total = labels.size();
        Map<String, Integer> countByZone = new LinkedHashMap<>();
        Set<String> uniqueSkus = new HashSet<>();
        int unmapped = 0;

        for (ZplLabel label : labels) {
            String zone = resolveZone(label.sku(), skuToZone);
            countByZone.merge(zone, 1, Integer::sum);
            if (label.sku() != null) {
                uniqueSkus.add(label.sku());
            }
            if (zone.equals(UNKNOWN)) {
                unmapped++;
            }
        }

        return new LabelStatistics(total, countByZone, uniqueSkus.size(), unmapped);
    }
}
