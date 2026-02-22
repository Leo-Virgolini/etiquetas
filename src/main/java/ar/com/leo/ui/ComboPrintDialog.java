package ar.com.leo.ui;

import ar.com.leo.model.ComboComponent;
import ar.com.leo.model.ComboProduct;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

public class ComboPrintDialog {

    private static final String[] COMBO_COLORS = {
            "#4CAF50", "#2196F3", "#FF9800", "#9C27B0", "#00BCD4", "#E91E63", "#795548", "#607D8B"
    };

    private final Stage stage;
    private final VBox printableContent;

    public ComboPrintDialog(Window owner, List<ComboProduct> combos) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Composición de Combos");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-etiqueta-100.png")));

        printableContent = buildContent(combos);

        ScrollPane scrollPane = new ScrollPane(printableContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(10));

        Button printBtn = new Button("\uD83D\uDDA8 Imprimir");
        printBtn.setStyle("-fx-font-size: 14px; -fx-padding: 8 20;");
        printBtn.setOnAction(e -> print());

        Button closeBtn = new Button("Cerrar");
        closeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 8 20;");
        closeBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(15, printBtn, closeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 15, 10, 15));

        BorderPane root = new BorderPane();
        root.setCenter(scrollPane);
        root.setBottom(buttons);

        Scene scene = new Scene(root, 700, 800);
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
    }

    private VBox buildContent(List<ComboProduct> combos) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("Composición de Combos");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        content.getChildren().add(title);

        for (int i = 0; i < combos.size(); i++) {
            ComboProduct combo = combos.get(i);
            String color = COMBO_COLORS[i % COMBO_COLORS.length];
            content.getChildren().add(buildComboSection(combo, color));
        }

        return content;
    }

    private VBox buildComboSection(ComboProduct combo, String color) {
        VBox section = new VBox(5);
        section.setPadding(new Insets(0, 0, 10, 0));

        // Encabezado del combo
        Label header = new Label(combo.codigoCompuesto() + "  -  " + combo.productoCompuesto());
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; "
                + "-fx-background-color: " + color + "; -fx-padding: 6 12; -fx-background-radius: 4;");
        header.setMaxWidth(Double.MAX_VALUE);
        section.getChildren().add(header);

        // Tabla de componentes
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(4);
        grid.setPadding(new Insets(8, 12, 8, 12));
        grid.setStyle("-fx-background-color: #fafafa; -fx-border-color: #e0e0e0; -fx-border-width: 0 1 1 1;");

        // Headers de la tabla
        Label hCod = new Label("Código");
        hCod.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label hProd = new Label("Producto");
        hProd.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Label hCant = new Label("Cant.");
        hCant.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        grid.add(hCod, 0, 0);
        grid.add(hProd, 1, 0);
        grid.add(hCant, 2, 0);

        // Column constraints
        ColumnConstraints col0 = new ColumnConstraints();
        col0.setPrefWidth(120);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPrefWidth(50);
        grid.getColumnConstraints().addAll(col0, col1, col2);

        int row = 1;
        for (ComboComponent comp : combo.componentes()) {
            Label codLabel = new Label(comp.codigoComponente());
            codLabel.setStyle("-fx-font-size: 12px;");
            Label prodLabel = new Label(comp.productoComponente());
            prodLabel.setStyle("-fx-font-size: 12px;");
            Label cantLabel = new Label(String.valueOf(comp.cantidad()));
            cantLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

            grid.add(codLabel, 0, row);
            grid.add(prodLabel, 1, row);
            grid.add(cantLabel, 2, row);
            row++;
        }

        section.getChildren().add(grid);
        return section;
    }

    private void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            AlertHelper.showError("Error", "No se pudo crear el trabajo de impresión.");
            return;
        }

        boolean proceed = job.showPrintDialog(stage);
        if (!proceed) {
            job.endJob();
            return;
        }

        PageLayout pageLayout = job.getPrinter().createPageLayout(
                Paper.A4, PageOrientation.PORTRAIT,
                javafx.print.Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(pageLayout);

        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();
        double contentWidth = printableContent.prefWidth(-1);
        double contentHeight = printableContent.prefHeight(printableWidth);

        // Escalar para que quepa en el ancho de la página
        double scale = Math.min(1.0, printableWidth / contentWidth);

        // Aplicar layout antes de imprimir
        printableContent.applyCss();
        printableContent.layout();
        printableContent.setPrefWidth(printableWidth / scale);

        // Re-layout con el nuevo ancho
        printableContent.applyCss();
        printableContent.layout();

        contentHeight = printableContent.prefHeight(printableWidth / scale);
        double scaledHeight = contentHeight * scale;

        // Si cabe en una página, imprimir directo
        if (scaledHeight <= printableHeight) {
            Scale scaleTransform = new Scale(scale, scale);
            printableContent.getTransforms().add(scaleTransform);
            boolean success = job.printPage(pageLayout, printableContent);
            printableContent.getTransforms().remove(scaleTransform);
            if (success) {
                job.endJob();
            }
        } else {
            // Multi-página: recortar por secciones
            Scale scaleTransform = new Scale(scale, scale);
            printableContent.getTransforms().add(scaleTransform);

            double pageHeight = printableHeight / scale;
            double totalHeight = contentHeight;
            double yOffset = 0;

            while (yOffset < totalHeight) {
                printableContent.setTranslateY(-yOffset * scale);
                // Clip para que solo se vea la porción de esta página
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(
                        0, yOffset * scale, printableWidth, printableHeight);
                printableContent.setClip(clip);

                boolean success = job.printPage(pageLayout, printableContent);
                if (!success) break;
                yOffset += pageHeight;
            }

            printableContent.setClip(null);
            printableContent.setTranslateY(0);
            printableContent.getTransforms().remove(scaleTransform);
            job.endJob();
        }

        // Restaurar tamaño original
        printableContent.setPrefWidth(Region.USE_COMPUTED_SIZE);
        printableContent.applyCss();
        printableContent.layout();
    }
}
