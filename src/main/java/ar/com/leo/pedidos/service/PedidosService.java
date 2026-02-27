package ar.com.leo.pedidos.service;

import ar.com.leo.AppLogger;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PedidosService extends Service<File> {

    private static final Color COLOR_INFO = Color.web("#555555");
    private static final Color COLOR_SUCCESS = Color.web("#2E7D32");
    private static final Color COLOR_WARN = Color.web("#E65100");
    private static final Color COLOR_ERROR = Color.FIREBRICK;

    private final TextFlow logTextFlow;
    private final ScrollPane logScrollPane;

    public PedidosService(TextFlow logTextFlow, ScrollPane logScrollPane) {
        this.logTextFlow = logTextFlow;
        this.logScrollPane = logScrollPane;
    }

    @Override
    protected Task<File> createTask() {
        return new Task<>() {
            private final ConcurrentLinkedQueue<String> pendingMessages = new ConcurrentLinkedQueue<>();
            private final AtomicBoolean flushing = new AtomicBoolean(false);

            @Override
            protected File call() throws Exception {
                AppLogger.setUiLogger(message -> {
                    pendingMessages.add(message);
                    scheduleFlush();
                });

                try {
                    return PedidosGenerator.generarPedidos();
                } finally {
                    AppLogger.setUiLogger(null);
                }
            }

            private void scheduleFlush() {
                if (flushing.compareAndSet(false, true)) {
                    Platform.runLater(() -> {
                        List<String> batch = new ArrayList<>();
                        String msg;
                        while ((msg = pendingMessages.poll()) != null) {
                            batch.add(msg);
                        }
                        flushing.set(false);
                        for (String message : batch) {
                            Text text = new Text(message + "\n");
                            text.setFont(Font.font("Segoe UI", 12));
                            if (message.contains("[ERROR]")) {
                                text.setFill(COLOR_ERROR);
                            } else if (message.contains("[WARN]")) {
                                text.setFill(COLOR_WARN);
                            } else if (message.contains("[OK]")) {
                                text.setFill(COLOR_SUCCESS);
                            } else {
                                text.setFill(COLOR_INFO);
                            }
                            logTextFlow.getChildren().add(text);
                        }
                        logScrollPane.setVvalue(1.0);
                        if (!pendingMessages.isEmpty()) {
                            scheduleFlush();
                        }
                    });
                }
            }
        };
    }
}
