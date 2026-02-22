package ar.com.leo.parser;

import ar.com.leo.model.ComboComponent;
import ar.com.leo.model.ComboProduct;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ComboExcelReader {

    public Map<String, ComboProduct> read(Path excelPath) throws IOException {
        try (InputStream is = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(is)) {
            return readFromWorkbook(workbook);
        }
    }

    private Map<String, ComboProduct> readFromWorkbook(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);

        int codCompuestoCol = -1;
        int prodCompuestoCol = -1;
        int codComponenteCol = -1;
        int prodComponenteCol = -1;
        int cantidadCol = -1;
        int headerRowIdx = -1;

        // Buscar headers en las primeras filas (0, 1 o 2)
        for (int r = 0; r <= Math.min(2, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                if (cell == null) continue;
                String header = getCellStringValue(cell).toLowerCase().trim();
                if (header.contains("código compuesto") || header.contains("codigo compuesto")) {
                    codCompuestoCol = i;
                } else if (header.contains("producto compuesto")) {
                    prodCompuestoCol = i;
                } else if (header.contains("código componente") || header.contains("codigo componente")) {
                    codComponenteCol = i;
                } else if (header.contains("producto componente")) {
                    prodComponenteCol = i;
                } else if (header.equals("cantidad")) {
                    cantidadCol = i;
                }
            }
            if (codCompuestoCol != -1 && codComponenteCol != -1 && cantidadCol != -1) {
                headerRowIdx = r;
                break;
            }
        }

        if (headerRowIdx == -1) {
            throw new IllegalArgumentException(
                    "No se encontraron las columnas requeridas en el Excel de combos. "
                    + "Se necesitan columnas: 'Código Compuesto', 'Código Componente', 'Cantidad'.");
        }

        // Agrupar filas por Código Compuesto
        Map<String, String> comboNames = new LinkedHashMap<>();
        Map<String, List<ComboComponent>> comboComponents = new LinkedHashMap<>();

        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String codCompuesto = getCellValue(row, codCompuestoCol);
            if (codCompuesto.isEmpty()) continue;

            String prodCompuesto = prodCompuestoCol != -1 ? getCellValue(row, prodCompuestoCol) : "";
            String codComponente = getCellValue(row, codComponenteCol);
            String prodComponente = prodComponenteCol != -1 ? getCellValue(row, prodComponenteCol) : "";
            int cantidad = parseCantidad(row, cantidadCol);

            if (codComponente.isEmpty()) continue;

            comboNames.putIfAbsent(codCompuesto, prodCompuesto);
            comboComponents.computeIfAbsent(codCompuesto, k -> new ArrayList<>())
                    .add(new ComboComponent(codComponente, prodComponente, cantidad));
        }

        Map<String, ComboProduct> result = new LinkedHashMap<>();
        for (var entry : comboComponents.entrySet()) {
            String code = entry.getKey();
            result.put(code, new ComboProduct(code, comboNames.get(code), entry.getValue()));
        }
        return result;
    }

    private String getCellValue(Row row, int col) {
        if (col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return getCellStringValue(cell).trim();
    }

    private int parseCantidad(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return 1;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String val = getCellStringValue(cell).trim();
        if (val.isEmpty()) return 1;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 1;
        }
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
