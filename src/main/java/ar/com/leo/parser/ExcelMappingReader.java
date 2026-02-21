package ar.com.leo.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ExcelMappingReader {

    public Map<String, String> readMapping(Path excelPath) throws IOException {
        try (InputStream is = Files.newInputStream(excelPath);
             Workbook workbook = new XSSFWorkbook(is)) {
            return readFromWorkbook(workbook);
        }
    }

    private Map<String, String> readFromWorkbook(Workbook workbook) {
        Map<String, String> mapping = new HashMap<>();
        Sheet sheet = workbook.getSheetAt(0);

        int skuCol = -1;
        int zoneCol = -1;
        int headerRowIdx = -1;

        Row headerRow = sheet.getRow(2); // Fila 3 (índice 0)
        if (headerRow != null) {
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;
                String header = getCellStringValue(cell).toLowerCase().trim();
                if (header.equals("código producto")) {
                    skuCol = i;
                }
                if (header.contains("unidad")) {
                    zoneCol = i;
                }
            }
            if (skuCol != -1 && zoneCol != -1) {
                headerRowIdx = 2;
            }
        }

        if (headerRowIdx == -1) {
            throw new IllegalArgumentException(
                    "No se encontraron las columnas requeridas en el Excel. "
                    + "Se necesita una columna con 'Código'/'SKU' y otra con 'Unidad'.");
        }

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            Cell skuCell = row.getCell(skuCol);
            Cell zoneCell = row.getCell(zoneCol);
            if (skuCell == null || zoneCell == null) continue;

            String sku = getCellStringValue(skuCell).trim();
            String zoneName = getCellStringValue(zoneCell).trim();

            if (!sku.isEmpty() && !zoneName.isEmpty()) {
                mapping.put(sku, zoneName);
            }
        }

        return mapping;
    }

    private String getCellStringValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
