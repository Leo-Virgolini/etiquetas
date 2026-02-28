package ar.com.leo.pickit.service;

import ar.com.leo.AppLogger;
import ar.com.leo.pickit.model.ProductoManual;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextFlow;

import ar.com.leo.ui.LogHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class PickitService extends Service<File> {

    private static final Color COLOR_INFO = Color.web("#555555");
    private static final Color COLOR_SUCCESS = Color.web("#2E7D32");
    private static final Color COLOR_WARN = Color.web("#E65100");
    private static final Color COLOR_ERROR = Color.FIREBRICK;

    private final File stockExcel;
    private final File combosExcel;
    private final List<ProductoManual> productosManuales;
    private final boolean soloHoy;
    private final TextFlow logTextFlow;
    private final ScrollPane logScrollPane;

    public PickitService(File stockExcel, File combosExcel, ObservableList<ProductoManual> productosManuales, boolean soloHoy, TextFlow logTextFlow, ScrollPane logScrollPane) {
        this.stockExcel = stockExcel;
        this.combosExcel = combosExcel;
        this.productosManuales = new ArrayList<>(productosManuales);
        this.soloHoy = soloHoy;
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
                    return PickitGenerator.generarPickit(stockExcel, combosExcel, productosManuales, soloHoy);
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
                            Color color;
                            if (message.contains("[ERROR]")) {
                                color = COLOR_ERROR;
                            } else if (message.contains("[WARN]")) {
                                color = COLOR_WARN;
                            } else if (message.contains("[OK]")) {
                                color = COLOR_SUCCESS;
                            } else {
                                color = COLOR_INFO;
                            }
                            LogHelper.appendLog(logTextFlow, logScrollPane, message, color);
                        }
                        if (!pendingMessages.isEmpty()) {
                            scheduleFlush();
                        }
                    });
                }
            }
        };
    }
}
