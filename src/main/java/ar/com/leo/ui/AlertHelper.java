package ar.com.leo.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public final class AlertHelper {

    private static final Image APP_ICON = new Image(
            AlertHelper.class.getResourceAsStream("/ar/com/leo/ui/icons8-etiqueta-100.png"));

    private AlertHelper() {
    }

    private static void setIcon(Alert alert) {
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(APP_ICON);
    }

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setIcon(alert);
        alert.showAndWait();
    }

    public static void showError(String title, String message, Throwable ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        alert.setContentText(sw.toString().substring(0, Math.min(sw.toString().length(), 1000)));
        setIcon(alert);
        alert.showAndWait();
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setIcon(alert);
        alert.showAndWait();
    }

    /**
     * Variante con TextArea scrollable para mensajes largos (no se trunca ni se corta).
     */
    public static void showInfoScrollable(String title, String message) {
        mostrarDialogoScrollable(Alert.AlertType.INFORMATION, title, message, false);
    }

    /**
     * Variante de error con TextArea scrollable y texto en rojo.
     */
    public static void showErrorScrollable(String title, String message) {
        mostrarDialogoScrollable(Alert.AlertType.ERROR, title, message, true);
    }

    private static void mostrarDialogoScrollable(Alert.AlertType type, String title, String message, boolean textoRojo) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message == null ? "" : message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(20);
        textArea.setPrefColumnCount(70);
        if (textoRojo) {
            // Rojo oscuro coherente con el color usado en la columna ERROR del Excel.
            textArea.setStyle("-fx-text-fill: #991b1b; -fx-font-family: 'Consolas', 'Courier New', monospace;");
        }

        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(750);
        alert.setResizable(true);
        setIcon(alert);
        alert.showAndWait();
    }

    public static Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setIcon(alert);
        return alert.showAndWait();
    }
}
