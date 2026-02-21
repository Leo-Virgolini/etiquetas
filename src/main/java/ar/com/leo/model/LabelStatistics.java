package ar.com.leo.model;

import java.util.Map;

public record LabelStatistics(int totalLabels, Map<String, Integer> countByZone, int uniqueSkus, int unmappedLabels) {
}
