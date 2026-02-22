package ar.com.leo.model;

import java.util.Map;

public record ExcelMapping(Map<String, String> skuToZone, Map<String, String> skuToExternalCode) {
}
