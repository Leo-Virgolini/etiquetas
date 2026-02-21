package ar.com.leo.printer;

import ar.com.leo.model.ZplLabel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ZplFileSaver {

    private static final String SEPARATOR = "^XA^MCY^XZ\n";

    public void save(List<ZplLabel> sortedLabels, Path outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedLabels.size(); i++) {
            if (i > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(sortedLabels.get(i).rawZpl());
            if (!sortedLabels.get(i).rawZpl().endsWith("\n")) {
                sb.append("\n");
            }
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }
}
