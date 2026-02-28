package ar.com.leo.ui;

import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogHelper {

    private static final Font LOG_FONT = Font.font("Segoe UI", 12);
    private static final Pattern FILE_PATH_PATTERN =
            Pattern.compile("[A-Z]:\\\\.*?\\.(?:xlsx?|txt|csv|pdf|zip)");

    private LogHelper() {}

    /**
     * Crea los nodos (Text + Hyperlink) para un mensaje de log.
     * Detecta rutas de archivos Windows y las convierte en links clickeables.
     */
    public static List<Node> createLogNodes(String message, Color color) {
        List<Node> nodes = new ArrayList<>();
        Matcher matcher = FILE_PATH_PATTERN.matcher(message);
        int lastEnd = 0;

        while (matcher.find()) {
            String filePath = matcher.group();
            File file = new File(filePath);
            if (!file.exists()) continue;

            // Texto antes del path
            if (matcher.start() > lastEnd) {
                Text before = new Text(message.substring(lastEnd, matcher.start()));
                before.setFill(color);
                before.setFont(LOG_FONT);
                nodes.add(before);
            }

            // Hyperlink para el archivo
            Hyperlink link = new Hyperlink(file.getName());
            link.setFont(LOG_FONT);
            link.setCursor(Cursor.HAND);
            link.setOnAction(e -> openFile(file));
            nodes.add(link);

            lastEnd = matcher.end();
        }

        // Texto restante (o todo el mensaje si no hubo match)
        String remaining = lastEnd == 0 ? message + "\n" : message.substring(lastEnd) + "\n";
        Text tail = new Text(remaining);
        tail.setFill(color);
        tail.setFont(LOG_FONT);
        nodes.add(tail);

        return nodes;
    }

    /**
     * Agrega un mensaje al TextFlow del log con detección de links.
     */
    public static void appendLog(TextFlow textFlow, ScrollPane scrollPane, String message, Color color) {
        textFlow.getChildren().addAll(createLogNodes(message, color));
        scrollPane.setVvalue(1.0);
    }

    /**
     * Extrae todo el texto del TextFlow (incluyendo Hyperlinks) para copiar.
     */
    public static String extractText(TextFlow textFlow) {
        StringBuilder sb = new StringBuilder();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text t) {
                sb.append(t.getText());
            } else if (node instanceof Hyperlink h) {
                sb.append(h.getText());
            }
        }
        return sb.toString();
    }

    /**
     * Agrega un Hyperlink a un HBox para abrir un archivo generado.
     */
    public static void addFileLink(HBox container, File file) {
        Hyperlink link = new Hyperlink("\uD83D\uDCC4 " + file.getName());
        link.setFont(LOG_FONT);
        link.setCursor(Cursor.HAND);
        link.setStyle("-fx-text-fill: #1565C0;");
        link.setOnAction(e -> openFile(file));
        container.getChildren().add(link);
    }

    private static void openFile(File file) {
        new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception e) {
                // Si falla abrir el archivo, intentar abrir la carpeta contenedora
                try {
                    Desktop.getDesktop().open(file.getParentFile());
                } catch (Exception ignored) {}
            }
        }).start();
    }
}
