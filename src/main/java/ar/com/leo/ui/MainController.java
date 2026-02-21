package ar.com.leo.ui;

import ar.com.leo.AppLogger;
import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.api.ml.model.OrdenML;
import ar.com.leo.api.ml.model.Venta;
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
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import javax.print.PrintService;
import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private ComboBox<String> estadoFilterCombo;
    @FXML
    private ComboBox<String> despachoFilterCombo;
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
    @FXML
    private Button fetchOrdersBtn;
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
    private final LabelSorter labelSorter = new LabelSorter();
    private final ZplFileSaver fileSaver = new ZplFileSaver();
    private final ZplPrinterService printerService = new ZplPrinterService();
    private final PrinterDiscovery printerDiscovery = new PrinterDiscovery();
    private final Preferences prefs = Preferences.userRoot().node("etiquetas");

    private static final String PREF_EXCEL_PATH = "excelFilePath";
    private static final String PREF_ZPL_DIR = "zplLastDir";

    private boolean meliInitialized = false;
    private SortResult currentResult;
    private List<OrdenML> fetchedOrders;

    @FXML
    public void initialize() {
        labelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        zoneCol.setCellValueFactory(new PropertyValueFactory<>("zone"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        descCol.setCellValueFactory(new PropertyValueFactory<>("productDescription"));
        detailsCol.setCellValueFactory(new PropertyValueFactory<>("details"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
                        case "ready_to_ship" -> "\ud83d\udce6 Lista";
                        case "shipped" -> "\ud83d\ude9a Enviada";
                        case "delivered" -> "\u2714 Entregada";
                        default -> "\u2753 " + status;
                    };
                    setText(label);
                }
            }
        });
        orderSlaCol.setCellValueFactory(new PropertyValueFactory<>("slaDate"));

        estadoFilterCombo.setItems(FXCollections.observableArrayList("Todas", "Solo impresas", "Solo pendientes"));
        estadoFilterCombo.setValue("Solo pendientes");
        despachoFilterCombo.setItems(FXCollections.observableArrayList("Solo para hoy", "Hoy + futuro"));
        despachoFilterCombo.setValue("Solo para hoy");

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

        Map<String, String> mapping;
        try {
            mapping = loadExcelMapping();
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

                // Lanzar consultas de SLA y substatus en paralelo
                Map<Long, MercadoLibreAPI.SlaInfo> slaMap = Collections.emptyMap();
                Map<Long, String> substatusMap = Collections.emptyMap();
                AppLogger.info("Controller - " + ordenes.size() + " órdenes, " + shipmentIds.size() + " shipments. soloSlaHoy=" + soloSlaHoy);

                if (!shipmentIds.isEmpty()) {
                    AppLogger.info("Controller - Consultando SLA y estados de " + shipmentIds.size() + " envíos...");
                    var slaFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                            () -> MercadoLibreAPI.obtenerSlasParalelo(shipmentIds));
                    var substatusFuture = java.util.concurrent.CompletableFuture.supplyAsync(
                            () -> MercadoLibreAPI.obtenerShipmentSubstatuses(shipmentIds));
                    slaMap = slaFuture.join();
                    substatusMap = substatusFuture.join();
                    AppLogger.info("Controller - SLAs obtenidos: " + slaMap.size() + ", estados: " + substatusMap.size());
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
                if (soloImpresas || soloPendientes) {
                    List<OrdenML> filtradasEstado = new ArrayList<>();
                    for (OrdenML orden : ordenes) {
                        Long shipId = orden.getShipmentId();
                        String substatus = shipId != null ? substatusMap.getOrDefault(shipId, "") : "";
                        if (soloImpresas && "printed".equals(substatus)) {
                            filtradasEstado.add(orden);
                        } else if (soloPendientes && !"printed".equals(substatus)) {
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
                    displayOrders(finalOrdenes, mapping, finalSlaMap, finalSubstatusMap);
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

        Map<String, String> mapping;
        try {
            mapping = loadExcelMapping();
        } catch (Exception e) {
            AlertHelper.showError("Error", e.getMessage(), e);
            return;
        }

        // Filtrar solo las órdenes seleccionadas
        List<OrdenML> seleccionadas = new ArrayList<>();
        for (OrderTableRow row : orderTable.getItems()) {
            if (row.isSelected()) {
                seleccionadas.add(row.getOrden());
            }
        }

        if (seleccionadas.isEmpty()) {
            AlertHelper.showError("Error", "No hay \u00f3rdenes seleccionadas.");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                List<ZplLabel> labels = MercadoLibreAPI.descargarEtiquetasZplParaOrdenes(seleccionadas);
                SortResult result = labelSorter.sort(labels, mapping);

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

    private void setLoading(boolean loading) {
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        tabPane.setDisable(loading);
        downloadLabelsBtn.setDisable(loading);
        saveFileBtn.setDisable(loading);
        printDirectBtn.setDisable(loading);
    }

    private void showOrderTable() {
        orderTable.setVisible(true);
        orderTable.setManaged(true);
        labelTable.setVisible(false);
        labelTable.setManaged(false);
        boolean hayOrdenes = !orderTable.getItems().isEmpty();
        downloadLabelsBtn.setDisable(!hayOrdenes);
        saveFileBtn.setDisable(true);
        printDirectBtn.setDisable(true);
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
    }

    private void displayOrders(List<OrdenML> ordenes, Map<String, String> skuToZone,
                               Map<Long, MercadoLibreAPI.SlaInfo> slaMap,
                               Map<Long, String> substatusMap) {
        DateTimeFormatter slaFormatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        ObservableList<OrderTableRow> rows = FXCollections.observableArrayList();

        for (OrdenML orden : ordenes) {
            StringJoiner skuJoiner = new StringJoiner(", ");
            StringJoiner descJoiner = new StringJoiner(", ");
            StringJoiner qtyJoiner = new StringJoiner(", ");
            String firstSku = "";
            for (Venta v : orden.getItems()) {
                String itemSku = v.getSku() != null ? v.getSku() : "";
                if (firstSku.isEmpty() && !itemSku.isEmpty()) firstSku = itemSku;
                skuJoiner.add(itemSku.isEmpty() ? "?" : itemSku);
                descJoiner.add(v.getTitulo() != null && !v.getTitulo().isEmpty() ? v.getTitulo() : itemSku);
                qtyJoiner.add(String.valueOf((int) v.getCantidad()));
            }
            String sku = skuJoiner.toString();
            String desc = descJoiner.toString();
            String qty = qtyJoiner.toString();

            String zone = (!firstSku.isEmpty()) ? skuToZone.getOrDefault(firstSku, "???") : "???";

            String slaDate = "";
            Long shipId = orden.getShipmentId();
            if (shipId != null && slaMap.containsKey(shipId)) {
                MercadoLibreAPI.SlaInfo sla = slaMap.get(shipId);
                if (sla.expectedDate() != null) {
                    slaDate = sla.expectedDate().format(slaFormatter);
                }
            }

            String status = "";
            if (shipId != null && substatusMap.containsKey(shipId)) {
                status = substatusMap.get(shipId);
            }
            rows.add(new OrderTableRow(true, zone, sku, desc, qty, status, slaDate, orden));
        }

        List<String> zoneOrder = List.of("J1", "J2", "J3", "T1", "T2", "T3", "CARROS", "RETIROS");
        rows.sort(Comparator
                .<OrderTableRow, Integer>comparing(r -> {
                    String base = r.getZone().toUpperCase();
                    int dash = base.indexOf('-');
                    if (dash > 0) base = base.substring(0, dash);
                    int idx = zoneOrder.indexOf(base);
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                })
                .thenComparing(r -> {
                    String base = r.getZone().toUpperCase();
                    int dash = base.indexOf('-');
                    return dash > 0 ? base.substring(dash + 1) : "";
                })
                .thenComparing(OrderTableRow::getSku)
                .thenComparing(OrderTableRow::getSlaDate));

        orderTable.setItems(rows);

        // Placeholder dinámico si no hay resultados
        if (rows.isEmpty()) {
            orderTable.setPlaceholder(new Label("No se encontraron ordenes con los filtros seleccionados"));
        }

        // Estadísticas
        int printedCount = 0;
        int readyCount = 0;
        for (OrderTableRow r : rows) {
            if ("printed".equals(r.getStatus())) printedCount++;
            else readyCount++;
        }
        final int printed = printedCount;
        final int readyToPrint = readyCount;
        if (rows.isEmpty()) {
            statsLabel.setText("No hay ordenes para mostrar");
        } else {
            StringJoiner sj = new StringJoiner("  \u2502  ");
            sj.add("Ordenes: " + rows.size());
            sj.add("Seleccionadas: " + rows.size());
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
                String base = "Ordenes: " + orderTable.getItems().size()
                        + "  \u2502  Seleccionadas: " + selected;
                if (readyToPrint > 0) base += "  \u2502  Pendientes: " + readyToPrint;
                if (printed > 0) base += "  \u2502  Impresas: " + printed;
                statsLabel.setText(base);
            });
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
