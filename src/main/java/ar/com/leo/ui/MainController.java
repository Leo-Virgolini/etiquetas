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
import javafx.stage.FileChooser;

import javax.print.PrintService;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
    private HBox searchBar;
    @FXML
    private TextField searchField;
    @FXML
    private TableView<LabelTableRow> labelTable;
    @FXML
    private TableColumn<LabelTableRow, String> labelOrderCol;
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
    private Button comboSheetBtn;
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
    private Set<Long> turboShipmentIds = Set.of();
    private FilteredList<OrderTableRow> filteredOrders;

    @FXML
    public void initialize() {
        labelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        labelTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        labelOrderCol.setCellValueFactory(new PropertyValueFactory<>("orderIds"));
        labelOrderCol.setCellFactory(col -> new TableCell<>() {
            private final Label prefixLabel = new Label();
            private final Label suffixLabel = new Label();
            private final HBox box = new HBox(0, prefixLabel, suffixLabel);
            {
                suffixLabel.setStyle("-fx-font-weight: bold;");
                box.setAlignment(Pos.CENTER);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setGraphic(null);
                } else {
                    if (item.length() > 5) {
                        prefixLabel.setText(item.substring(0, item.length() - 5));
                        suffixLabel.setText(item.substring(item.length() - 5));
                    } else {
                        prefixLabel.setText("");
                        suffixLabel.setText(item);
                    }
                    setGraphic(box);
                }
            }
        });
        zoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        zoneCol.setCellFactory(col -> centeredCell());
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        skuCol.setCellFactory(col -> centeredCell());
        descCol.setCellValueFactory(new PropertyValueFactory<>("productDescription"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        countCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });

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
                setAlignment(Pos.CENTER);
                if (empty || status == null || status.isEmpty()) {
                    setText(null);
                    setStyle("");
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
                    String bg = switch (status) {
                        case "ready_to_print" -> "-fx-background-color: #C8E6C9;";
                        case "printed", "ready_for_dropoff", "ready_for_pickup" -> "-fx-background-color: #FFCDD2;";
                        default -> "";
                    };
                    setStyle(bg);
                }
            }
        });
        orderSlaCol.setCellValueFactory(new PropertyValueFactory<>("slaDate"));

        // Celdas multilínea para columnas que pueden tener varios productos
        orderIdCol.setCellFactory(col -> new TableCell<>() {
            private final Label prefixLabel = new Label();
            private final Label suffixLabel = new Label();
            private final HBox box = new HBox(0, prefixLabel, suffixLabel);
            {
                suffixLabel.setStyle("-fx-font-weight: bold;");
                box.setAlignment(Pos.CENTER);
                setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    if (item.length() > 5) {
                        prefixLabel.setText(item.substring(0, item.length() - 5));
                        suffixLabel.setText(item.substring(item.length() - 5));
                    } else {
                        prefixLabel.setText("");
                        suffixLabel.setText(item);
                    }
                    setGraphic(box);
                }
            }
        });
        orderZoneCol.setCellFactory(col -> centeredCell());
        orderSkuCol.setCellFactory(col -> centeredCell());
        orderQtyCol.setCellFactory(col -> centeredCell());
        orderSlaCol.setCellFactory(col -> centeredCell());

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

    private void validateExcelFiles() {
        String excelPath = excelFileField.getText();
        if (excelPath == null || excelPath.isBlank()) {
            throw new IllegalArgumentException("Seleccione el archivo Excel de stock (SKU \u2192 Zona).");
        }
        String comboPath = comboExcelField.getText();
        if (comboPath == null || comboPath.isBlank()) {
            throw new IllegalArgumentException("Seleccione el archivo Excel de composición de combos.");
        }
    }

    private ExcelMapping loadExcelMapping() throws Exception {
        validateExcelFiles();
        return excelReader.readMapping(Path.of(excelFileField.getText()));
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
                // Solo consultar SLAs (fecha de despacho) en paralelo
                if (!shipmentIds.isEmpty()) {
                    slaMap = MercadoLibreAPI.obtenerSlasParalelo(shipmentIds);
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

                // Extraer shipment IDs turbo
                Set<Long> turboIds = new HashSet<>();
                for (var slaEntry : slaMap.entrySet()) {
                    if (slaEntry.getValue().turbo()) {
                        turboIds.add(slaEntry.getKey());
                    }
                }
                final List<OrdenML> finalOrdenes = ordenes;
                final Map<Long, MercadoLibreAPI.SlaInfo> finalSlaMap = slaMap;
                final Map<Long, String> finalSubstatusMap = substatusMap;
                final Set<Long> finalTurboIds = turboIds;

                Platform.runLater(() -> {
                    setLoading(false);
                    fetchedOrders = finalOrdenes;
                    turboShipmentIds = finalTurboIds;
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
        long totalEtiquetas = seleccionadas.stream()
                .map(OrdenML::getShipmentId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .count();
        confirm.setHeaderText("Se descargarán " + totalEtiquetas + " etiqueta(s)");
        boolean hayPendientes = seleccionadas.stream()
                .anyMatch(o -> "ready_to_print".equals(o.getShippingSubstatus()));
        String advertencia = hayPendientes
                ? "Al descargar, el estado de las órdenes pendientes pasará a \"Impresa\" en MercadoLibre.\n\n¿Desea continuar?"
                : "¿Desea continuar?";
        confirm.setContentText(advertencia);
        confirm.setGraphic(new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-señal-de-advertencia-general-100.png"), 48, 48, true, true)));
        ((javafx.stage.Stage) confirm.getDialogPane().getScene().getWindow()).getIcons().add(
                new javafx.scene.image.Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-etiqueta-100.png")));
        Optional<ButtonType> confirmResult = confirm.showAndWait();
        if (confirmResult.isEmpty() || confirmResult.get() != ButtonType.OK) return;

        setLoading(true);

        new Thread(() -> {
            try {
                List<ZplLabel> labels = MercadoLibreAPI.descargarEtiquetasZplParaOrdenes(seleccionadas, turboShipmentIds);
                SortResult result = injectZplHeaders(
                        labelSorter.sort(labels, excelMapping.skuToZone()), excelMapping);

                // Guardar automáticamente en carpeta "Etiquetas"
                String saveError = null;
                try {
                    Path etiquetasDir = Path.of("Etiquetas");
                    Files.createDirectories(etiquetasDir);
                    String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
                    Path outputFile = etiquetasDir.resolve("etiquetas_ordenadas_" + fechaHora + ".txt");
                    fileSaver.save(result.sortedFlatList(), outputFile);
                } catch (Exception ex) {
                    AppLogger.error("Error al guardar automáticamente", ex);
                    saveError = ex.getMessage();
                }

                final String finalSaveError = saveError;
                Platform.runLater(() -> {
                    setLoading(false);
                    currentResult = result;
                    showLabelTable();
                    displayResult(result);
                    if (finalSaveError != null) {
                        AlertHelper.showError("Error al guardar", "No se pudo guardar el archivo automáticamente:\n" + finalSaveError);
                    }
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
    private void onPrintDirect() {
        if (currentResult == null || currentResult.groups().isEmpty()) {
            AlertHelper.showError("Error", "No hay etiquetas para imprimir.");
            return;
        }

        // 1. Seleccionar zonas a imprimir
        Map<String, Long> zoneCounts = new LinkedHashMap<>();
        for (SortedLabelGroup group : currentResult.groups()) {
            zoneCounts.merge(group.zone(), (long) group.labels().size(), Long::sum);
        }

        Dialog<List<String>> zoneDialog = new Dialog<>();
        zoneDialog.setTitle("Seleccionar zonas");
        zoneDialog.setHeaderText("Seleccione las zonas a imprimir:");
        zoneDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox zoneBox = new VBox(8);
        zoneBox.setStyle("-fx-padding: 10;");
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (var entry : zoneCounts.entrySet()) {
            CheckBox cb = new CheckBox(entry.getKey() + "  (" + entry.getValue() + " etiquetas)");
            cb.setSelected(true);
            cb.setUserData(entry.getKey());
            checkBoxes.add(cb);
            zoneBox.getChildren().add(cb);
        }

        Button toggleBtn = new Button("Deseleccionar todas");
        toggleBtn.setOnAction(e -> {
            boolean allSelected = checkBoxes.stream().allMatch(CheckBox::isSelected);
            checkBoxes.forEach(cb -> cb.setSelected(!allSelected));
            toggleBtn.setText(allSelected ? "Seleccionar todas" : "Deseleccionar todas");
        });
        zoneBox.getChildren().add(toggleBtn);

        zoneDialog.getDialogPane().setContent(zoneBox);
        zoneDialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return checkBoxes.stream()
                        .filter(CheckBox::isSelected)
                        .map(cb -> (String) cb.getUserData())
                        .toList();
            }
            return null;
        });

        Optional<List<String>> zonesResult = zoneDialog.showAndWait();
        if (zonesResult.isEmpty() || zonesResult.get().isEmpty()) return;
        Set<String> selectedZones = new LinkedHashSet<>(zonesResult.get());

        // Filtrar etiquetas por zonas seleccionadas
        List<ZplLabel> labelsToPrint = currentResult.groups().stream()
                .filter(g -> selectedZones.contains(g.zone()))
                .flatMap(g -> g.labels().stream())
                .toList();

        if (labelsToPrint.isEmpty()) {
            AlertHelper.showError("Error", "No hay etiquetas en las zonas seleccionadas.");
            return;
        }

        // 2. Seleccionar impresora
        List<PrintService> printers = printerDiscovery.findAll();
        if (printers.isEmpty()) {
            AlertHelper.showError("Error", "No se encontraron impresoras.");
            return;
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                printers.getFirst().getName(),
                printers.stream().map(PrintService::getName).toList());
        dialog.setTitle("Seleccionar impresora");
        dialog.setHeaderText("Seleccione la impresora para enviar " + labelsToPrint.size() + " etiqueta(s):");

        Optional<String> selected = dialog.showAndWait();
        if (selected.isEmpty()) return;

        PrintService selectedPrinter = printers.stream()
                .filter(p -> p.getName().equals(selected.get()))
                .findFirst()
                .orElse(null);

        if (selectedPrinter == null) return;

        try {
            printerService.printViaPrintService(labelsToPrint, selectedPrinter);
            AlertHelper.showInfo("\ud83d\udda8 Impresi\u00f3n", labelsToPrint.size() + " etiquetas enviadas a " + selectedPrinter.getName());
            showComboSheetIfNeeded();
        } catch (Exception e) {
            AlertHelper.showError("Error al imprimir", e.getMessage(), e);
        }
    }

    @FXML
    private void onShowComboSheet() {
        showComboSheetIfNeeded();
    }

    private List<ComboProduct> findMatchingCombos() {
        String comboPath = comboExcelField.getText();
        if (comboPath == null || comboPath.isBlank()) return List.of();
        if (currentResult == null || currentResult.groups().isEmpty()) return List.of();

        try {
            Map<String, ComboProduct> allCombos = comboExcelReader.read(Path.of(comboPath));
            if (allCombos.isEmpty()) return List.of();

            // Recolectar SKUs del lote actual (separar multi-SKU de CARROS)
            Set<String> batchSkus = new HashSet<>();
            for (SortedLabelGroup group : currentResult.groups()) {
                for (String sku : group.sku().split("\n")) {
                    String trimmed = sku.trim();
                    if (!trimmed.isEmpty()) batchSkus.add(trimmed);
                }
            }

            List<ComboProduct> matchingCombos = new ArrayList<>();
            for (var entry : allCombos.entrySet()) {
                if (batchSkus.contains(entry.getKey())) {
                    matchingCombos.add(entry.getValue());
                }
            }
            matchingCombos.sort(Comparator.comparing(ComboProduct::codigoCompuesto));
            return matchingCombos;
        } catch (Exception e) {
            AppLogger.warn("Error al leer Excel de combos: " + e.getMessage());
            return List.of();
        }
    }

    private void showComboSheetIfNeeded() {
        List<ComboProduct> combos = findMatchingCombos();
        if (combos.isEmpty()) {
            AlertHelper.showInfo("Combos", "No se encontraron combos para las etiquetas actuales.");
            return;
        }
        new ComboPrintDialog(getWindow(), combos).show();
    }

    private static <T> TableCell<T, String> centeredCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : item);
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
        searchBar.setDisable(loading);
        downloadLabelsBtn.setDisable(loading);
        comboSheetBtn.setDisable(loading);
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
        comboSheetBtn.setDisable(true);
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
        comboSheetBtn.setDisable(!hayEtiquetas);
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

            // Detectar si el envío es turbo
            boolean esTurbo = false;
            for (OrdenML o : group) {
                Long shipId = o.getShipmentId();
                if (shipId != null && slaMap.containsKey(shipId) && slaMap.get(shipId).turbo()) {
                    esTurbo = true;
                    break;
                }
            }

            // Determinar zona: TURBOS si es turbo, CARROS si hay 2+ SKUs distintos
            Set<String> distinctSkus = new HashSet<>();
            for (OrdenML o : group) {
                for (Venta v : o.getItems()) {
                    String s = v.getSku() != null ? v.getSku() : "";
                    if (!s.isEmpty()) distinctSkus.add(s);
                }
            }
            String zone;
            if (esTurbo) {
                zone = "TURBOS";
            } else if (distinctSkus.size() > 1) {
                zone = "CARROS";
            } else {
                Venta firstItem = firstOrden.getItems().getFirst();
                String firstSku = firstItem.getSku() != null ? firstItem.getSku() : "";
                zone = !firstSku.isEmpty() ? skuToZone.getOrDefault(firstSku, "???") : "???";
            }

            rows.add(new OrderTableRow(true, orderIdStr, zone, skuJoiner.toString(),
                    descJoiner.toString(), qtyJoiner.toString(), status, slaDate, group));
        }

        // Prioridad: J*, T*, COMBOS, CARROS, TURBOS, RETIROS, resto
        rows.sort(Comparator
                .<OrderTableRow, Integer>comparing(r -> {
                    String z = r.getZone().toUpperCase();
                    if (z.startsWith("J")) return 0;
                    if (z.startsWith("TURBOS")) return 4;
                    if (z.startsWith("T")) return 1;
                    if (z.startsWith("COMBOS")) return 2;
                    if (z.startsWith("CARROS")) return 3;
                    if (z.startsWith("RETIROS")) return 5;
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
        Map<String, Integer> countByZone = new LinkedHashMap<>();
        Set<String> uniqueSkus = new HashSet<>();
        for (OrderTableRow r : rows) {
            if ("printed".equals(r.getStatus())) printedCount++;
            else readyCount++;
            totalProductos += r.getProductCount();
            countByZone.merge(r.getZone(), 1, Integer::sum);
            for (String s : r.getSku().split("\n")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) uniqueSkus.add(trimmed);
            }
        }
        final int printed = printedCount;
        final int readyToPrint = readyCount;
        final int ordCount = totalOrdenes;
        final int prodCount = totalProductos;
        final int skuCount = uniqueSkus.size();

        Runnable updateStats = () -> {
            long selected = orderTable.getItems().stream().filter(OrderTableRow::isSelected).count();
            StringJoiner sj = new StringJoiner("  \u2502  ");
            sj.add("Ordenes: " + ordCount);
            sj.add("Productos: " + prodCount);
            sj.add("SKUs: " + skuCount);
            sj.add("Seleccionados: " + selected);
            if (readyToPrint > 0) sj.add("Pendientes: " + readyToPrint);
            if (printed > 0) sj.add("Impresas: " + printed);
            for (Map.Entry<String, Integer> entry : countByZone.entrySet()) {
                if (entry.getValue() > 0) sj.add(entry.getKey() + ": " + entry.getValue());
            }
            statsLabel.setText(sj.toString());
        };

        if (rows.isEmpty()) {
            statsLabel.setText("No hay ordenes para mostrar");
        } else {
            updateStats.run();
        }
        statsBar.setVisible(true);
        statsBar.setManaged(true);
        searchBar.setVisible(true);
        searchBar.setManaged(true);

        // Actualizar stats cuando cambia la selección
        for (OrderTableRow r : rows) {
            r.selectedProperty().addListener((obs, oldVal, newVal) -> updateStats.run());
        }
    }

    private static final Pattern UNIT_PATTERN = Pattern.compile(
            "(\\^FO(\\d+),(\\d+)\\^A0N,70,70\\^FB160,1,0,C\\^FD)(\\d+)(\\^FS)");
    private static final Pattern FO_PATTERN = Pattern.compile("\\^FO(\\d+),(\\d+)");
    private static final Pattern FONT_PATTERN = Pattern.compile("\\^A0N,(\\d+),(\\d+)");
    private static final Pattern FB_PATTERN = Pattern.compile("\\^FB(\\d+),(\\d+)");

    private SortResult injectZplHeaders(SortResult result, ExcelMapping excelMapping) {
        Map<String, String> skuToZone = excelMapping.skuToZone();
        Map<String, String> skuToExtCode = excelMapping.skuToExternalCode();
        List<SortedLabelGroup> newGroups = new ArrayList<>();
        for (SortedLabelGroup group : result.groups()) {
            String zone = group.zone();
            String sku = group.sku();
            String zoneText;
            if ("CARROS".equals(zone)) {
                zoneText = "ZONA: CARROS";
            } else {
                String extCode = skuToExtCode.getOrDefault(sku, "");
                zoneText = "ZONA: " + zone + " | COD.EXT.: " + (extCode.isEmpty() ? "-" : extCode);
            }
            List<ZplLabel> newLabels = new ArrayList<>();
            for (ZplLabel label : group.labels()) {
                String raw = label.rawZpl();
                // Insertar ZONA y COD.EXT. debajo del último campo SKU (o del último campo de producto para packs)
                int lastSkuIdx = raw.lastIndexOf("SKU:");
                int anchorFoIdx = -1;
                int anchorFsIdx = -1;

                if (lastSkuIdx >= 0) {
                    // Etiqueta con SKU: anclar al campo del SKU
                    anchorFsIdx = raw.indexOf("^FS", lastSkuIdx);
                    anchorFoIdx = raw.lastIndexOf("^FO", lastSkuIdx);
                } else {
                    // Etiqueta pack/carro sin SKU: anclar debajo de "Unidad"/"Unidades"
                    int unidadIdx = raw.indexOf("Unidad");
                    if (unidadIdx >= 0) {
                        anchorFsIdx = raw.indexOf("^FS", unidadIdx);
                        anchorFoIdx = raw.lastIndexOf("^FO", unidadIdx);
                    }
                }

                if (anchorFoIdx >= 0 && anchorFsIdx >= 0) {
                    String segment = raw.substring(anchorFoIdx, anchorFsIdx);
                    Matcher foMatcher = FO_PATTERN.matcher(segment);
                    Matcher fontMatcher = FONT_PATTERN.matcher(segment);
                    Matcher fbMatcher = FB_PATTERN.matcher(segment);
                    if (foMatcher.find()) {
                        int x = Integer.parseInt(foMatcher.group(1));
                        int y = Integer.parseInt(foMatcher.group(2));
                        int fontH = fontMatcher.find() ? Integer.parseInt(fontMatcher.group(1)) : 28;
                        int fbLines = fbMatcher.find() ? Integer.parseInt(fbMatcher.group(2)) : 1;
                        int newY = y + (fontH * fbLines) + 4;
                        int fontSize = 25;
                        // Pseudo-bold: imprimir el texto 2 veces con 1 dot de offset
                        String field1 = "^FO" + x + "," + newY + "^A0N," + fontSize + "," + fontSize + "^FD" + zoneText + "^FS";
                        String field2 = "^FO" + (x + 1) + "," + newY + "^A0N," + fontSize + "," + fontSize + "^FD" + zoneText + "^FS";
                        raw = raw.substring(0, anchorFsIdx + 3) + "\n" + field1 + "\n" + field2 + raw.substring(anchorFsIdx + 3);
                    }
                }
                // Resaltar número de unidad (video inverso) si > 1 y zona no es CARROS ni RETIROS
                raw = highlightUnitIfNeeded(raw, zone);
                newLabels.add(new ZplLabel(raw, label.sku(), label.productDescription(), label.details(), label.quantity(), label.turbo(), label.orderIds()));
            }
            newGroups.add(new SortedLabelGroup(zone, group.sku(), group.productDescription(), group.details(), group.orderIds(), newLabels));
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
                    group.orderIds(),
                    group.zone(),
                    group.sku(),
                    group.productDescription(),
                    group.details(),
                    extractQuantityFromLabels(group.labels())));
        }
        labelTable.setItems(rows);

        LabelStatistics stats = result.statistics();
        int totalProductos = result.groups().stream()
                .mapToInt(g -> extractQuantityFromLabels(g.labels()))
                .sum();
        StringJoiner sj = new StringJoiner("  \u2502  ");
        sj.add("Etiquetas: " + stats.totalLabels());
        sj.add("Productos: " + totalProductos);
        sj.add("SKUs: " + stats.uniqueSkus());
        if (stats.unmappedLabels() > 0) {
            sj.add("\u26a0 Sin zona: " + stats.unmappedLabels());
        }
        for (Map.Entry<String, Integer> entry : stats.countByZone().entrySet()) {
            if (entry.getValue() > 0) {
                sj.add(entry.getKey() + ": " + entry.getValue());
            }
        }

        statsLabel.setText(sj.toString());
        statsBar.setVisible(true);
        statsBar.setManaged(true);
        searchBar.setVisible(false);
        searchBar.setManaged(false);
    }

    private javafx.stage.Window getWindow() {
        return labelTable.getScene().getWindow();
    }
}
