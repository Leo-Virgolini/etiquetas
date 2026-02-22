package ar.com.leo.ui;

import ar.com.leo.AppLogger;
import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.api.ml.model.OrdenML;
import ar.com.leo.api.ml.model.Venta;
import ar.com.leo.model.*;
import ar.com.leo.model.ExcelMapping;
import ar.com.leo.model.ComboProduct;
import ar.com.leo.parser.ComboExcelReader;
import ar.com.leo.parser.ExcelMappingReader;
import ar.com.leo.parser.ZplParser;
import ar.com.leo.printer.PrinterDiscovery;
import ar.com.leo.printer.ZplFileSaver;
import ar.com.leo.printer.ZplPrinterService;
import ar.com.leo.sorter.LabelSorter;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.print.PrintService;
import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {

    @FXML
    private TabPane tabPane;
    @FXML
    private TextField zplFileField;
    @FXML
    private TextField excelFileField;
    @FXML
    private TextField comboExcelField;
    @FXML
    private VBox excelSelectorsBox;
    @FXML
    private Label meliStatusLabel;
    @FXML
    private ComboBox<String> estadoFilterCombo;
    @FXML
    private ComboBox<String> despachoFilterCombo;
    @FXML
    private Label statsLabel;
    @FXML
    private HBox statsBar;
    @FXML
    private TextField searchField;
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
    @FXML
    private Button fetchOrdersBtn;
    @FXML
    private Button backToOrdersBtn;
    @FXML
    private Button downloadLabelsBtn;
    @FXML
    private Button saveFileBtn;
    @FXML
    private Button printDirectBtn;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TableView<OrderTableRow> orderTable;
    @FXML
    private TableColumn<OrderTableRow, Boolean> orderSelectCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderIdCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderZoneCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderSkuCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderDescCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderQtyCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderStatusCol;
    @FXML
    private TableColumn<OrderTableRow, String> orderSlaCol;

    private final ZplParser zplParser = new ZplParser();
    private final ExcelMappingReader excelReader = new ExcelMappingReader();
    private final ComboExcelReader comboExcelReader = new ComboExcelReader();
    private final LabelSorter labelSorter = new LabelSorter();
    private final ZplFileSaver fileSaver = new ZplFileSaver();
    private final ZplPrinterService printerService = new ZplPrinterService();
    private final PrinterDiscovery printerDiscovery = new PrinterDiscovery();
    private final Preferences prefs = Preferences.userRoot().node("etiquetas");

    private static final String PREF_EXCEL_PATH = "excelFilePath";
    private static final String PREF_COMBO_EXCEL_PATH = "comboExcelFilePath";
    private static final String PREF_ZPL_DIR = "zplLastDir";

    private boolean meliInitialized = false;
    private SortResult currentResult;
    private List<OrdenML> fetchedOrders;
    private FilteredList<OrderTableRow> filteredOrders;

    @FXML
    public void initialize() {
        labelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        labelTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        zoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("productDescription"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        orderTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        orderTable.setEditable(true);
        orderSelectCol.setCellValueFactory(cd -> cd.getValue().selectedProperty());
        orderSelectCol.setCellFactory(CheckBoxTableCell.forTableColumn(orderSelectCol));
        CheckBox selectAllCheck = new CheckBox();
        selectAllCheck.setSelected(true);
        selectAllCheck.setOnAction(e -> {
            boolean val = selectAllCheck.isSelected();
            for (OrderTableRow row : orderTable.getItems()) {
                row.setSelected(val);
            }
        });
        orderSelectCol.setGraphic(selectAllCheck);
        orderIdCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        orderZoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        orderSkuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        orderDescCol.setCellValueFactory(new PropertyValueFactory<>("productDescription"));
        orderQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        orderStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        orderStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null || status.isEmpty()) {
                    setText(null);
                } else {
                    String label = switch (status) {
                        case "ready_to_print" -> "\ud83d\udfe1 Pendiente";
                        case "printed" -> "\u2705 Impresa";
                        case "ready_for_dropoff", "ready_for_pickup" -> "\ud83d\udce6 Lista p/ despacho";
                        case "dropped_off" -> "\ud83d\udce5 Despachada";
                        case "picked_up", "in_hub", "in_transit" -> "\ud83d\ude9a En camino";
                        case "shipped" -> "\ud83d\ude9a Enviada";
                        case "delivered" -> "\u2714 Entregada";
                        default -> "\u2753 " + status;
                    };
                    setText(label);
                }
            }
        });
        orderSlaCol.setCellValueFactory(new PropertyValueFactory<>("slaDate"));

        // Celdas multilínea para columnas que pueden tener varios productos
        orderSkuCol.setCellFactory(col -> newWrappingCell());
        orderDescCol.setCellFactory(col -> newWrappingCell());
        orderQtyCol.setCellFactory(col -> newWrappingCell());

        // Ajustar altura de fila según cantidad de productos
        orderTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(OrderTableRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                } else {
                    int lines = item.getProductCount();
                    setPrefHeight(lines > 1 ? lines * 22 + 10 : Control.USE_COMPUTED_SIZE);
                }
            }
        });

        // Centrar headers de ambas tablas
        centerColumnHeaders(orderTable);
        centerColumnHeaders(labelTable);

        // Copiar al portapapeles con Ctrl+C (fila) y click derecho (celda)
        setupTableCopyHandler(orderTable);
        setupTableCopyHandler(labelTable);
        setupCellCopyMenu(orderTable);
        setupCellCopyMenu(labelTable);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredOrders != null) {
                String filter = newVal == null ? "" : newVal.trim();
                filteredOrders.setPredicate(row ->
                        filter.isEmpty() || row.getOrderId().contains(filter));
            }
        });

        estadoFilterCombo.setItems(FXCollections.observableArrayList("Todas", "Solo impresas", "Solo pendientes"));
        estadoFilterCombo.setValue("Solo pendientes");
        setupComboIcons(estadoFilterCombo, Map.of(
                "Todas", "\uD83D\uDCCB",
                "Solo impresas", "✅",
                "Solo pendientes", "\uD83D\uDD51"
        ));
        despachoFilterCombo.setItems(FXCollections.observableArrayList("Solo para hoy", "Hoy + futuro"));
        despachoFilterCombo.setValue("Solo para hoy");
        setupComboIcons(despachoFilterCombo, Map.of(
                "Solo para hoy", "\uD83D\uDCC5",
                "Hoy + futuro", "\uD83D\uDCC6"
        ));

        String savedExcelPath = prefs.get(PREF_EXCEL_PATH, "");
        if (!savedExcelPath.isBlank() && new File(savedExcelPath).exists()) {
            excelFileField.setText(savedExcelPath);
        }

        String savedComboPath = prefs.get(PREF_COMBO_EXCEL_PATH, "");
        if (!savedComboPath.isBlank() && new File(savedComboPath).exists()) {
            comboExcelField.setText(savedComboPath);
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

    @FXML
    private void onSelectComboExcelFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar Excel de composición de combos");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls"));
        File file = fc.showOpenDialog(getWindow());
        if (file != null) {
            comboExcelField.setText(file.getAbsolutePath());
            prefs.put(PREF_COMBO_EXCEL_PATH, file.getAbsolutePath());
        }
    }

    private ExcelMapping loadExcelMapping() throws Exception {
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
            ExcelMapping excelMapping = loadExcelMapping();
            List<ZplLabel> labels = zplParser.parseFile(Path.of(zplPath));
            currentResult = injectZplHeaders(
                    labelSorter.sort(labels, excelMapping.skuToZone()), excelMapping);
            showLabelTable();
            displayResult(currentResult);
        } catch (Exception e) {
            AlertHelper.showError("Error al procesar", e.getMessage(), e);
        }
    }

    @FXML
    private void onFetchMeliOrders() {
        if (!meliInitialized) {
            AlertHelper.showError("Error", "Primero inicie sesi\u00f3n en MercadoLibre.");
            return;
        }

        ExcelMapping excelMapping;
        try {
            excelMapping = loadExcelMapping();
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage(), e);
            return;
        }

        String estadoFiltro = estadoFilterCombo.getValue();
        String despachoFiltro = despachoFilterCombo.getValue();
        boolean incluirImpresas = !"Solo pendientes".equals(estadoFiltro);
        boolean soloPendientes = "Solo pendientes".equals(estadoFiltro);
        boolean soloImpresas = "Solo impresas".equals(estadoFiltro);
        boolean soloSlaHoy = "Solo para hoy".equals(despachoFiltro);

        setLoading(true);

        new Thread(() -> {
            try {
                String userId = MercadoLibreAPI.getUserId();
                MercadoLibreAPI.MLOrderResult result = MercadoLibreAPI.obtenerVentasReadyToPrint(userId, incluirImpresas);
                List<OrdenML> ordenes = result.ordenes();

                // Obtener SLAs en paralelo
                List<Long> shipmentIds = new ArrayList<>();
                for (OrdenML orden : ordenes) {
                    Long shipId = orden.getShipmentId();
                    if (shipId != null && shipId > 0) {
                        shipmentIds.add(shipId);
                    }
                }

                // Substatus ya viene del search (asignado en searchAndCollect)
                Map<Long, MercadoLibreAPI.SlaInfo> slaMap = new HashMap<>();
                Map<Long, String> substatusMap = new HashMap<>();
                for (OrdenML orden : ordenes) {
                    Long shipId = orden.getShipmentId();
                    if (shipId != null && shipId > 0) {
                        substatusMap.put(shipId, orden.getShippingSubstatus());
                    }
                }
                AppLogger.info("Controller - " + ordenes.size() + " órdenes, " + shipmentIds.size() + " shipments. soloSlaHoy=" + soloSlaHoy);

                // Solo consultar SLAs (fecha de despacho) en paralelo
                if (!shipmentIds.isEmpty()) {
                    AppLogger.info("Controller - Consultando SLA de " + shipmentIds.size() + " envíos...");
                    slaMap = MercadoLibreAPI.obtenerSlasParalelo(shipmentIds);
                    AppLogger.info("Controller - SLAs obtenidos: " + slaMap.size() + " envíos");
                }

                if (soloSlaHoy && !shipmentIds.isEmpty()) {
                    OffsetDateTime hoyFin = java.time.LocalDate.now()
                            .atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toOffsetDateTime();

                    List<OrdenML> filtradas = new ArrayList<>();
                    for (OrdenML orden : ordenes) {
                        Long shipId = orden.getShipmentId();
                        if (shipId == null || shipId <= 0) {
                            filtradas.add(orden);
                            continue;
                        }
                        MercadoLibreAPI.SlaInfo sla = slaMap.get(shipId);
                        if (sla == null || sla.expectedDate() == null) {
                            filtradas.add(orden);
                            continue;
                        }
                        OffsetDateTime expected = sla.expectedDate();
                        if (expected.isBefore(hoyFin) || expected.isEqual(hoyFin)) {
                            filtradas.add(orden); // SLA hoy o antes
                        }
                    }
                    AppLogger.info("Controller - Filtro SLA: " + filtradas.size() + " de " + ordenes.size() + " órdenes");
                    ordenes = filtradas;
                }

                // Filtro por estado (solo impresas / solo pendientes)
                // "Solo pendientes" = substatus ready_to_print
                // "Solo impresas" = cualquier substatus que NO sea ready_to_print (printed, ready_for_dropoff, etc.)
                if (soloImpresas || soloPendientes) {
                    List<OrdenML> filtradasEstado = new ArrayList<>();
                    for (OrdenML orden : ordenes) {
                        Long shipId = orden.getShipmentId();
                        String substatus = shipId != null ? substatusMap.getOrDefault(shipId, "") : "";
                        boolean esPendiente = "ready_to_print".equals(substatus);
                        if (soloImpresas && !esPendiente) {
                            filtradasEstado.add(orden);
                        } else if (soloPendientes && esPendiente) {
                            filtradasEstado.add(orden);
                        }
                    }
                    ordenes = filtradasEstado;
                }

                final List<OrdenML> finalOrdenes = ordenes;
                final Map<Long, MercadoLibreAPI.SlaInfo> finalSlaMap = slaMap;
                final Map<Long, String> finalSubstatusMap = substatusMap;
                AppLogger.info("Controller - Mostrando " + finalOrdenes.size() + " órdenes en tabla");

                Platform.runLater(() -> {
                    setLoading(false);
                    fetchedOrders = finalOrdenes;
                    displayOrders(finalOrdenes, excelMapping.skuToZone(), finalSlaMap, finalSubstatusMap);
                    showOrderTable();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    AlertHelper.showError("Error API ML", e.getMessage(), e);
                });
            }
        }).start();
    }

    @FXML
    private void onDownloadSelectedLabels() {
        if (fetchedOrders == null || fetchedOrders.isEmpty()) {
            AlertHelper.showError("Error", "No hay \u00f3rdenes cargadas.");
            return;
        }

        ExcelMapping excelMapping;
        try {
            excelMapping = loadExcelMapping();
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage(), e);
            return;
        }

        // Filtrar solo las órdenes seleccionadas (deduplicar por orderId)
        LinkedHashSet<Long> seenOrderIds = new LinkedHashSet<>();
        List<OrdenML> seleccionadas = new ArrayList<>();
        for (OrderTableRow row : orderTable.getItems()) {
            if (row.isSelected()) {
                for (OrdenML o : row.getOrdenes()) {
                    if (seenOrderIds.add(o.getOrderId())) {
                        seleccionadas.add(o);
                    }
                }
            }
        }

        if (seleccionadas.isEmpty()) {
            AlertHelper.showError("Error", "No hay \u00f3rdenes seleccionadas.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar descarga");
        confirm.setHeaderText("Se descargarán " + seleccionadas.size() + " etiqueta(s)");
        confirm.setContentText("Al descargar, el estado de las órdenes pasará a \"Impresa\" en MercadoLibre.\n\n¿Desea continuar?");
        confirm.setGraphic(new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-señal-de-advertencia-general-100.png"), 48, 48, true, true)));
        ((javafx.stage.Stage) confirm.getDialogPane().getScene().getWindow()).getIcons().add(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-etiqueta-100.png")));
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) return;

        setLoading(true);

        new Thread(() -> {
            try {
                List<ZplLabel> labels = MercadoLibreAPI.descargarEtiquetasZplParaOrdenes(seleccionadas);
                SortResult result = injectZplHeaders(
                        labelSorter.sort(labels, excelMapping.skuToZone()), excelMapping);

                Platform.runLater(() -> {
                    setLoading(false);
                    currentResult = result;
                    showLabelTable();
                    displayResult(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    AlertHelper.showError("Error API ML", e.getMessage(), e);
                });
            }
        }).start();
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
                showComboSheetIfNeeded();
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
            showComboSheetIfNeeded();
        } catch (Exception e) {
            AlertHelper.showError("Error al imprimir", e.getMessage(), e);
        }
    }

    private void showComboSheetIfNeeded() {
        String comboPath = comboExcelField.getText();
        if (comboPath == null || comboPath.isBlank()) return;
        if (currentResult == null || currentResult.groups().isEmpty()) return;

        try {
            Map<String, ComboProduct> allCombos = comboExcelReader.read(Path.of(comboPath));
            if (allCombos.isEmpty()) return;

            // Recolectar SKUs del lote actual
            Set<String> batchSkus = new HashSet<>();
            for (SortedLabelGroup group : currentResult.groups()) {
                batchSkus.add(group.sku());
            }

            // Filtrar combos presentes en el lote
            List<ComboProduct> matchingCombos = new ArrayList<>();
            for (var entry : allCombos.entrySet()) {
                if (batchSkus.contains(entry.getKey())) {
                    matchingCombos.add(entry.getValue());
                }
            }

            if (!matchingCombos.isEmpty()) {
                new ComboPrintDialog(getWindow(), matchingCombos).show();
            }
        } catch (Exception e) {
            AppLogger.warn("Error al leer Excel de combos: " + e.getMessage());
        }
    }

    private static <T> TableCell<T, String> newWrappingCell() {
        return new TableCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(Double.MAX_VALUE);
                setGraphic(label);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    label.setText(null);
                } else {
                    label.setText(item);
                }
            }
        };
    }

    private <T> void centerColumnHeaders(TableView<T> table) {
        for (TableColumn<T, ?> col : table.getColumns()) {
            if (col.getGraphic() != null) continue; // ya tiene graphic (ej. checkbox)
            Label headerLabel = new Label(col.getText());
            headerLabel.setStyle("-fx-font-weight: bold;");
            headerLabel.setMaxWidth(Double.MAX_VALUE);
            headerLabel.setAlignment(Pos.CENTER);
            col.setGraphic(headerLabel);
            col.setText("");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void setupCellCopyMenu(TableView<T> table) {
        MenuItem copiarCelda = new MenuItem("Copiar celda");
        copiarCelda.setOnAction(e -> {
            var pos = table.getFocusModel().getFocusedCell();
            if (pos != null && pos.getRow() >= 0 && pos.getTableColumn() != null) {
                TableColumn<T, ?> col = (TableColumn<T, ?>) pos.getTableColumn();
                Object val = col.getCellObservableValue(pos.getRow()).getValue();
                if (val != null) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(val.toString());
                    Clipboard.getSystemClipboard().setContent(content);
                }
            }
        });
        table.setContextMenu(new ContextMenu(copiarCelda));
    }

    private <T> void setupTableCopyHandler(TableView<T> table) {
        KeyCodeCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        table.setOnKeyPressed(event -> {
            if (ctrlC.match(event)) {
                StringBuilder sb = new StringBuilder();
                for (T item : table.getSelectionModel().getSelectedItems()) {
                    if (item == null) continue;
                    StringJoiner line = new StringJoiner("\t");
                    for (TableColumn<T, ?> col : table.getColumns()) {
                        Object val = col.getCellObservableValue(item).getValue();
                        line.add(val != null ? val.toString().replace("\n", " / ") : "");
                    }
                    sb.append(line).append("\n");
                }
                if (!sb.isEmpty()) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(sb.toString());
                    Clipboard.getSystemClipboard().setContent(content);
                }
            }
        });
    }

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        excelSelectorsBox.setDisable(loading);
        tabPane.setDisable(loading);
        orderTable.setDisable(loading);
        labelTable.setDisable(loading);
        statsBar.setDisable(loading);
        downloadLabelsBtn.setDisable(loading);
        saveFileBtn.setDisable(loading);
        printDirectBtn.setDisable(loading);
        backToOrdersBtn.setDisable(loading);
        searchField.setDisable(loading);
        estadoFilterCombo.setDisable(loading);
        despachoFilterCombo.setDisable(loading);
        fetchOrdersBtn.setDisable(loading);
    }

    private void showOrderTable() {
        orderTable.setVisible(true);
        orderTable.setManaged(true);
        labelTable.setVisible(false);
        labelTable.setManaged(false);
        searchField.clear();
        boolean hayOrdenes = !orderTable.getItems().isEmpty();
        downloadLabelsBtn.setDisable(!hayOrdenes);
        saveFileBtn.setDisable(true);
        printDirectBtn.setDisable(true);
        backToOrdersBtn.setVisible(false);
        backToOrdersBtn.setManaged(false);
    }

    private void showLabelTable() {
        labelTable.setVisible(true);
        labelTable.setManaged(true);
        orderTable.setVisible(false);
        orderTable.setManaged(false);
        downloadLabelsBtn.setDisable(true);
        boolean hayEtiquetas = currentResult != null && !currentResult.groups().isEmpty();
        saveFileBtn.setDisable(!hayEtiquetas);
        printDirectBtn.setDisable(!hayEtiquetas);
        // Mostrar botón volver solo si hay órdenes cargadas
        boolean hayOrdenes = !orderTable.getItems().isEmpty();
        backToOrdersBtn.setVisible(hayOrdenes);
        backToOrdersBtn.setManaged(hayOrdenes);
    }

    @FXML
    private void onBackToOrders() {
        showOrderTable();
    }

    private void displayOrders(List<OrdenML> ordenes, Map<String, String> skuToZone,
                               Map<Long, MercadoLibreAPI.SlaInfo> slaMap,
                               Map<Long, String> substatusMap) {
        DateTimeFormatter slaFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        ObservableList<OrderTableRow> rows = FXCollections.observableArrayList();

        // Agrupar órdenes por pack_id (o por order_id si no tiene pack)
        Map<String, List<OrdenML>> grouped = new LinkedHashMap<>();
        for (OrdenML orden : ordenes) {
            String groupKey = orden.getPackId() != null
                    ? "P" + orden.getPackId()
                    : "O" + orden.getOrderId();
            grouped.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(orden);
        }

        int totalOrdenes = grouped.size();

        for (var entry : grouped.entrySet()) {
            List<OrdenML> group = entry.getValue();
            OrdenML firstOrden = group.getFirst();

            String orderIdStr = firstOrden.getPackId() != null
                    ? String.valueOf(firstOrden.getPackId())
                    : String.valueOf(firstOrden.getOrderId());

            // SLA y status del primer envío del grupo
            String slaDate = "";
            String status = "";
            for (OrdenML o : group) {
                Long shipId = o.getShipmentId();
                if (shipId != null) {
                    if (slaDate.isEmpty() && slaMap.containsKey(shipId)) {
                        MercadoLibreAPI.SlaInfo sla = slaMap.get(shipId);
                        if (sla.expectedDate() != null) {
                            slaDate = sla.expectedDate().format(slaFormatter);
                        }
                    }
                    if (status.isEmpty() && substatusMap.containsKey(shipId)) {
                        status = substatusMap.get(shipId);
                    }
                }
            }

            // Recolectar todos los productos de todas las órdenes del grupo
            StringJoiner skuJoiner = new StringJoiner("\n");
            StringJoiner descJoiner = new StringJoiner("\n");
            StringJoiner qtyJoiner = new StringJoiner("\n");

            for (OrdenML o : group) {
                for (Venta v : o.getItems()) {
                    String itemSku = v.getSku() != null ? v.getSku() : "?";
                    String desc = v.getTitulo() != null && !v.getTitulo().isEmpty() ? v.getTitulo() : itemSku;
                    String qty = String.valueOf((int) v.getCantidad());
                    skuJoiner.add(itemSku);
                    descJoiner.add(desc);
                    qtyJoiner.add(qty);
                }
            }

            // Determinar zona
            int totalItems = group.stream().mapToInt(o -> o.getItems().size()).sum();
            String zone;
            if (totalItems > 1 || group.size() > 1) {
                zone = "CARROS";
            } else {
                Venta firstItem = firstOrden.getItems().getFirst();
                String firstSku = firstItem.getSku() != null ? firstItem.getSku() : "";
                zone = !firstSku.isEmpty() ? skuToZone.getOrDefault(firstSku, "???") : "???";
            }

            rows.add(new OrderTableRow(true, orderIdStr, zone, skuJoiner.toString(),
                    descJoiner.toString(), qtyJoiner.toString(), status, slaDate, group));
        }

        // Prioridad: J* (J1,J2,J3…), T* (T1,T2,T3…), COMBOS, CARROS, resto
        rows.sort(Comparator
                .<OrderTableRow, Integer>comparing(r -> {
                    String z = r.getZone().toUpperCase();
                    if (z.startsWith("J")) return 0;
                    if (z.startsWith("T")) return 1;
                    if (z.startsWith("COMBOS")) return 2;
                    if (z.startsWith("CARROS")) return 3;
                    if (z.startsWith("RETIROS")) return 4;
                    return Integer.MAX_VALUE;
                })
                .thenComparing(r -> r.getZone().toUpperCase())
                .thenComparing(OrderTableRow::getSku));

        filteredOrders = new FilteredList<>(rows, p -> true);
        SortedList<OrderTableRow> sortedOrders = new SortedList<>(filteredOrders);
        sortedOrders.comparatorProperty().bind(orderTable.comparatorProperty());
        orderTable.setItems(sortedOrders);
        searchField.clear();

        if (rows.isEmpty()) {
            orderTable.setPlaceholder(new Label("No se encontraron ordenes con los filtros seleccionados"));
        }

        // Estadísticas
        int printedCount = 0;
        int readyCount = 0;
        int totalProductos = 0;
        for (OrderTableRow r : rows) {
            if ("printed".equals(r.getStatus())) printedCount++;
            else readyCount++;
            totalProductos += r.getProductCount();
        }
        final int printed = printedCount;
        final int readyToPrint = readyCount;
        final int ordCount = totalOrdenes;
        final int prodCount = totalProductos;
        if (rows.isEmpty()) {
            statsLabel.setText("No hay ordenes para mostrar");
        } else {
            StringJoiner sj = new StringJoiner("  \u2502  ");
            sj.add("Ordenes: " + ordCount);
            sj.add("Productos: " + prodCount);
            sj.add("Seleccionados: " + rows.size());
            if (readyToPrint > 0) sj.add("Pendientes: " + readyToPrint);
            if (printed > 0) sj.add("Impresas: " + printed);
            statsLabel.setText(sj.toString());
        }
        statsBar.setVisible(true);
        statsBar.setManaged(true);

        // Actualizar stats cuando cambia la selección
        for (OrderTableRow r : rows) {
            r.selectedProperty().addListener((obs, oldVal, newVal) -> {
                long selected = orderTable.getItems().stream().filter(OrderTableRow::isSelected).count();
                StringJoiner sj = new StringJoiner("  \u2502  ");
                sj.add("Ordenes: " + ordCount);
                sj.add("Productos: " + prodCount);
                sj.add("Seleccionados: " + selected);
                if (readyToPrint > 0) sj.add("Pendientes: " + readyToPrint);
                if (printed > 0) sj.add("Impresas: " + printed);
                statsLabel.setText(sj.toString());
            });
        }
    }

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "(\\^FO(\\d+),(\\d+)\\^A0N,70,70\\^FB160,1,0,C\\^FD)(\\d+)(\\^FS)");
    private static final Pattern FO_PATTERN = Pattern.compile("\\^FO(\\d+),(\\d+)");
    private static final Pattern FONT_PATTERN = Pattern.compile("\\^A0N,(\\d+),(\\d+)");

    private SortResult injectZplHeaders(SortResult result, ExcelMapping excelMapping) {
        Map<String, String> skuToZone = excelMapping.skuToZone();
        Map<String, String> skuToExtCode = excelMapping.skuToExternalCode();
        AppLogger.info("injectZplHeaders - extCodeMap tiene " + skuToExtCode.size() + " entradas");
        List<SortedLabelGroup> newGroups = new ArrayList<>();
        for (SortedLabelGroup group : result.groups()) {
            String zone = group.zone();
            String sku = group.sku();
            String extCode = "";
            if (sku != null && sku.contains("\n")) {
                // Multi-SKU: buscar cada SKU individual
                for (String s : sku.split("\n")) {
                    String ec = skuToExtCode.getOrDefault(s.trim(), "");
                    if (!ec.isEmpty()) {
                        extCode = ec;
                        break;
                    }
                }
            } else {
                extCode = skuToExtCode.getOrDefault(sku, "");
            }
            AppLogger.info("injectZplHeaders - SKU='" + sku + "' -> extCode='" + extCode + "'");
            String zoneText = "ZONA: " + zone + " | COD.EXT.: " + (extCode.isEmpty() ? "-" : extCode);
            List<ZplLabel> newLabels = new ArrayList<>();
            for (ZplLabel label : group.labels()) {
                String raw = label.rawZpl();
                // Insertar ZONA y COD.EXT. en una nueva línea debajo del último campo SKU
                int lastSkuIdx = raw.lastIndexOf("SKU:");
                if (lastSkuIdx >= 0) {
                    int fsIdx = raw.indexOf("^FS", lastSkuIdx);
                    // Buscar el ^FO que posiciona el campo del SKU para obtener coordenadas
                    int foIdx = raw.lastIndexOf("^FO", lastSkuIdx);
                    if (fsIdx >= 0 && foIdx >= 0) {
                        Matcher foMatcher = FO_PATTERN.matcher(raw.substring(foIdx, lastSkuIdx));
                        Matcher fontMatcher = FONT_PATTERN.matcher(raw.substring(foIdx, lastSkuIdx));
                        if (foMatcher.find()) {
                            int x = Integer.parseInt(foMatcher.group(1));
                            int y = Integer.parseInt(foMatcher.group(2));
                            int fontH = fontMatcher.find() ? Integer.parseInt(fontMatcher.group(1)) : 28;
                            int newY = y + fontH + 4;
                            // Pseudo-bold: imprimir el texto 2 veces con 1 dot de offset
                            String field1 = "^FO" + x + "," + newY + "^A0N,25,25^FD" + zoneText + "^FS";
                            String field2 = "^FO" + (x + 1) + "," + newY + "^A0N,25,25^FD" + zoneText + "^FS";
                            raw = raw.substring(0, fsIdx + 3) + "\n" + field1 + "\n" + field2 + raw.substring(fsIdx + 3);
                        }
                    }
                }
                // Resaltar número de unidad (video inverso) si > 1 y zona no es CARROS ni RETIROS
                raw = highlightUnitIfNeeded(raw, zone);
                newLabels.add(new ZplLabel(raw, label.sku(), label.productDescription(), label.details(), label.quantity()));
            }
            newGroups.add(new SortedLabelGroup(zone, group.sku(), group.productDescription(), group.details(), newLabels));
        }
        return new SortResult(newGroups, result.statistics());
    }

    private String highlightUnitIfNeeded(String rawZpl, String zone) {
        String zoneUpper = zone.toUpperCase();
        if (zoneUpper.startsWith("CARROS") || zoneUpper.startsWith("RETIROS")) {
            return rawZpl;
        }
        Matcher m = UNIT_PATTERN.matcher(rawZpl);
        if (m.find()) {
            int unitNum = Integer.parseInt(m.group(4));
            if (unitNum > 1) {
                int x = Integer.parseInt(m.group(2));
                int y = Integer.parseInt(m.group(3));
                // Caja negra rellena detrás del número, tamaño ajustado a la cantidad de dígitos
                int digits = String.valueOf(unitNum).length();
                int boxW = digits * 50 + 28;
                int boxH = 76;
                int boxX = x + (160 - boxW) / 2;
                String box = "^FO" + boxX + "," + (y - 3) + "^GB" + boxW + "," + boxH + "," + boxH + "^FS\n";
                // ^FR debe ir ANTES de ^FD para invertir el campo (blanco sobre negro)
                String prefix = m.group(1); // termina en ^FD
                String correctedPrefix = prefix.substring(0, prefix.length() - 3) + "^FR^FD";
                String replacement = box + correctedPrefix + m.group(4) + m.group(5);
                rawZpl = m.replaceFirst(Matcher.quoteReplacement(replacement));
            }
        }
        return rawZpl;
    }

    private int extractQuantityFromLabels(List<ZplLabel> labels) {
        int total = 0;
        for (ZplLabel label : labels) {
            total += label.quantity();
        }
        return total;
    }

    private void setupComboIcons(ComboBox<String> combo, Map<String, String> icons) {
        javafx.util.Callback<ListView<String>, ListCell<String>> factory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String icon = icons.getOrDefault(item, "");
                    setText(icon.isEmpty() ? item : icon + " " + item);
                }
            }
        };
        combo.setCellFactory(factory);
        combo.setButtonCell(factory.call(null));
    }

    private void displayResult(SortResult result) {
        ObservableList<LabelTableRow> rows = FXCollections.observableArrayList();
        for (SortedLabelGroup group : result.groups()) {
            rows.add(new LabelTableRow(
                    group.zone(),
                    group.sku(),
                    group.productDescription(),
                    group.details(),
                    extractQuantityFromLabels(group.labels())));
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
