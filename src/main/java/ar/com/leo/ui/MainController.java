package ar.com.leo.ui;

import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.model.*;
import ar.com.leo.parser.ExcelMappingReader;
import ar.com.leo.parser.ZplParser;
import ar.com.leo.printer.PrinterDiscovery;
import ar.com.leo.printer.ZplFileSaver;
import ar.com.leo.printer.ZplPrinterService;
import ar.com.leo.sorter.LabelSorter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.print.PrintService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.prefs.Preferences;

public class MainController {

    @FXML
    private TabPane tabPane;
    @FXML
    private TextField zplFileField;
    @FXML
    private TextField excelFileField;
    @FXML
    private Label meliStatusLabel;
    @FXML
    private CheckBox incluirImpresasCheck;
    @FXML
    private Label statsLabel;
    @FXML
    private HBox statsBar;
    @FXML
    private TableView<LabelTableRow> labelTable;
    @FXML
    private TableColumn<LabelTableRow, String> zoneCol;
    @FXML
    private TableColumn<LabelTableRow, String> skuCol;
    @FXML
    private TableColumn<LabelTableRow, String> descCol;
    @FXML
    private TableColumn<LabelTableRow, String> detailsCol;
    @FXML
    private TableColumn<LabelTableRow, Integer> countCol;

    private final ZplParser zplParser = new ZplParser();
    private final ExcelMappingReader excelReader = new ExcelMappingReader();
    private final LabelSorter labelSorter = new LabelSorter();
    private final ZplFileSaver fileSaver = new ZplFileSaver();
    private final ZplPrinterService printerService = new ZplPrinterService();
    private final PrinterDiscovery printerDiscovery = new PrinterDiscovery();
    private final Preferences prefs = Preferences.userRoot().node("etiquetas");

    private static final String PREF_EXCEL_PATH = "excelFilePath";
    private static final String PREF_ZPL_DIR = "zplLastDir";

    private boolean meliInitialized = false;
    private SortResult currentResult;

    @FXML
    public void initialize() {
        labelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        zoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("productDescription"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        String savedExcelPath = prefs.get(PREF_EXCEL_PATH, "");
        if (!savedExcelPath.isBlank() && new File(savedExcelPath).exists()) {
            excelFileField.setText(savedExcelPath);
        }

        try {
            meliInitialized = MercadoLibreAPI.inicializar();
            if (meliInitialized) {
                meliStatusLabel.setText("\ud83d\udfe2 Estado: Conectado");
            }
        } catch (Exception e) {
            meliStatusLabel.setText("\u26aa Estado: No conectado");
        }
    }

    @FXML
    private void onSelectZplFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo ZPL");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos ZPL", "*.txt", "*.zpl"));
        String lastDir = prefs.get(PREF_ZPL_DIR, "");
        if (!lastDir.isBlank()) {
            File dir = new File(lastDir);
            if (dir.isDirectory()) {
                fc.setInitialDirectory(dir);
            }
        }
        File file = fc.showOpenDialog(getWindow());
        if (file != null) {
            zplFileField.setText(file.getAbsolutePath());
            prefs.put(PREF_ZPL_DIR, file.getParent());
        }
    }

    @FXML
    private void onSelectExcelFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar archivo Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File file = fc.showOpenDialog(getWindow());
        if (file != null) {
            excelFileField.setText(file.getAbsolutePath());
            prefs.put(PREF_EXCEL_PATH, file.getAbsolutePath());
        }
    }

    private Map<String, String> loadExcelMapping() throws Exception {
        String excelPath = excelFileField.getText();
        if (excelPath == null || excelPath.isBlank()) {
            throw new IllegalArgumentException("Seleccione un archivo Excel con mapeo SKU \u2192 Zona.");
        }
        return excelReader.readMapping(Path.of(excelPath));
    }

    @FXML
    private void onProcessLocal() {
        String zplPath = zplFileField.getText();
        if (zplPath == null || zplPath.isBlank()) {
            AlertHelper.showError("Error", "Seleccione un archivo ZPL.");
            return;
        }

        try {
            Map<String, String> mapping = loadExcelMapping();
            List<ZplLabel> labels = zplParser.parseFile(Path.of(zplPath));
            currentResult = labelSorter.sort(labels, mapping);
            displayResult(currentResult);
        } catch (Exception e) {
            AlertHelper.showError("Error al procesar", e.getMessage(), e);
        }
    }

    @FXML
    private void onFetchMeliLabels() {
        if (!meliInitialized) {
            AlertHelper.showError("Error", "Primero inicie sesi\u00f3n en MercadoLibre.");
            return;
        }

        try {
            Map<String, String> mapping = loadExcelMapping();
            boolean incluirImpresas = incluirImpresasCheck.isSelected();

            new Thread(() -> {
                try {
                    String userId = MercadoLibreAPI.getUserId();
                    List<ZplLabel> labels = MercadoLibreAPI.descargarEtiquetasZpl(userId, incluirImpresas);
                    SortResult result = labelSorter.sort(labels, mapping);

                    Platform.runLater(() -> {
                        currentResult = result;
                        displayResult(result);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> AlertHelper.showError("Error API ML", e.getMessage(), e));
                }
            }).start();
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage(), e);
        }
    }

    @FXML
    private void onSaveFile() {
        if (currentResult == null || currentResult.groups().isEmpty()) {
            AlertHelper.showError("Error", "No hay etiquetas para guardar.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar etiquetas ordenadas");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo ZPL", "*.txt"));
        fc.setInitialFileName("etiquetas_ordenadas.txt");
        File file = fc.showSaveDialog(getWindow());

        if (file != null) {
            try {
                fileSaver.save(currentResult.sortedFlatList(), file.toPath());
                AlertHelper.showInfo("\ud83d\udcbe Guardado", "Archivo guardado en: " + file.getAbsolutePath());
            } catch (Exception e) {
                AlertHelper.showError("Error al guardar", e.getMessage(), e);
            }
        }
    }

    @FXML
    private void onPrintDirect() {
        if (currentResult == null || currentResult.groups().isEmpty()) {
            AlertHelper.showError("Error", "No hay etiquetas para imprimir.");
            return;
        }

        List<PrintService> printers = printerDiscovery.findAll();
        if (printers.isEmpty()) {
            AlertHelper.showError("Error", "No se encontraron impresoras.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                printers.getFirst().getName(),
                printers.stream().map(PrintService::getName).toList());
        dialog.setTitle("Seleccionar impresora");
        dialog.setHeaderText("Seleccione la impresora para enviar las etiquetas:");

        Optional<String> selected = dialog.showAndWait();
        if (selected.isEmpty()) return;

        PrintService selectedPrinter = printers.stream()
                .filter(p -> p.getName().equals(selected.get()))
                .findFirst()
                .orElse(null);

        if (selectedPrinter == null) return;

        try {
            printerService.printViaPrintService(currentResult.sortedFlatList(), selectedPrinter);
            AlertHelper.showInfo("\ud83d\udda8 Impresi\u00f3n", "Etiquetas enviadas a " + selectedPrinter.getName());
        } catch (Exception e) {
            AlertHelper.showError("Error al imprimir", e.getMessage(), e);
        }
    }

    private void displayResult(SortResult result) {
        ObservableList<LabelTableRow> rows = FXCollections.observableArrayList();
        for (SortedLabelGroup group : result.groups()) {
            rows.add(new LabelTableRow(
                    group.zone(),
                    group.sku(),
                    group.productDescription(),
                    group.details(),
                    group.count()));
        }
        labelTable.setItems(rows);

        LabelStatistics stats = result.statistics();
        StringJoiner sj = new StringJoiner("  \u2502  ");
        sj.add("Total: " + stats.totalLabels());
        for (Map.Entry<String, Integer> entry : stats.countByZone().entrySet()) {
            if (entry.getValue() > 0) {
                sj.add(entry.getKey() + ": " + entry.getValue());
            }
        }
        sj.add("SKUs: " + stats.uniqueSkus());
        if (stats.unmappedLabels() > 0) {
            sj.add("\u26a0 Sin zona: " + stats.unmappedLabels());
        }

        statsLabel.setText(sj.toString());
        statsBar.setVisible(true);
        statsBar.setManaged(true);
    }

    private javafx.stage.Window getWindow() {
        return labelTable.getScene().getWindow();
    }
}
