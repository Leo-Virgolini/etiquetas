package fx;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import pdf.model.CustomFont;
import service.GeneratePDFService;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class VentanaController implements Initializable {

    @FXML
    private TextField tipoEnvioText;
    @FXML
    private TextField trackingText;
    @FXML
    private TextField clienteText;
    @FXML
    private TextField domicilioText;
    @FXML
    private TextField localidadText;
    @FXML
    private TextField cpText;
    @FXML
    private TextArea observacionesText;
    @FXML
    private TextField telefonoText;
    @FXML
    private TextField tipoEnvioFontSize;
    @FXML
    private TextField trackingFontSize;
    @FXML
    private TextField clienteFontSize;
    @FXML
    private TextField domicilioFontSize;
    @FXML
    private TextField localidadFontSize;
    @FXML
    private TextField telefonoFontSize;
    @FXML
    private ColorPicker tipoEnvioColorPicker;
    @FXML
    private ColorPicker trackingColorPicker;
    @FXML
    private ColorPicker clienteColorPicker;
    @FXML
    private ColorPicker domicilioColorPicker;
    @FXML
    private ColorPicker localidadColorPicker;
    @FXML
    private ColorPicker telefonoColorPicker;
    @FXML
    private ComboBox<String> tipoEnvioFontComboBox;
    @FXML
    private ComboBox<String> trackingFontComboBox;
    @FXML
    private ComboBox<String> clienteFontComboBox;
    @FXML
    private ComboBox<String> domicilioFontComboBox;
    @FXML
    private ComboBox<String> localidadFontComboBox;
    @FXML
    private ComboBox<String> telefonoFontComboBox;
    @FXML
    private ComboBox<String> hojaComboBox;
    @FXML
    private CheckBox tipoEnvioCheckBox;
    @FXML
    private CheckBox trackingCheckBox;
    @FXML
    private CheckBox clienteCheckBox;
    @FXML
    private CheckBox domicilioCheckBox;
    @FXML
    private CheckBox localidadCheckBox;
    @FXML
    private CheckBox telefonoCheckBox;
    @FXML
    private TextArea logTextArea;
    @FXML
    private Button generarButton;
    @FXML
    private ProgressIndicator progressIndicator;

    private File archivoDestino;
    private AudioClip errorSound;
    private AudioClip successSound;

    public void initialize(URL url, ResourceBundle rb) {
        inicializarComponentes();
        Main.stage.setOnCloseRequest(event -> savePreferences());
    }

    @FXML
    public void generarEtiqueta() {
        logTextArea.clear();
        if (validarTextInputs()) {
            archivoDestino = new File(System.getProperty("user.dir") + System.getProperty("file.separator") + (clienteText.getText() != null ? clienteText.getText().trim() : "") + " - " + DateTimeFormatter.ofPattern("dd-MM-yy").format(LocalDate.now()) + ".pdf");
            final CustomFont tipoEnvioFont = new CustomFont(Float.parseFloat(tipoEnvioFontSize.getText()), tipoEnvioColorPicker.getValue(), tipoEnvioFontComboBox.getValue());
            final CustomFont trackingFont = new CustomFont(Float.parseFloat(trackingFontSize.getText()), trackingColorPicker.getValue(), trackingFontComboBox.getValue());
            final CustomFont clienteFont = new CustomFont(Float.parseFloat(clienteFontSize.getText()), clienteColorPicker.getValue(), clienteFontComboBox.getValue());
            final CustomFont domicilioFont = new CustomFont(Float.parseFloat(domicilioFontSize.getText()), domicilioColorPicker.getValue(), domicilioFontComboBox.getValue());
            final CustomFont localidadFont = new CustomFont(Float.parseFloat(localidadFontSize.getText()), localidadColorPicker.getValue(), localidadFontComboBox.getValue());
            final CustomFont telefonoFont = new CustomFont(Float.parseFloat(telefonoFontSize.getText()), telefonoColorPicker.getValue(), telefonoFontComboBox.getValue());

            GeneratePDFService service = new GeneratePDFService(archivoDestino,
                    tipoEnvioText.getText(), trackingText.getText(), clienteText.getText(), domicilioText.getText(), localidadText.getText(), cpText.getText(), observacionesText.getText(), telefonoText.getText(),
                    tipoEnvioFont, trackingFont, clienteFont, domicilioFont, localidadFont, telefonoFont,
                    hojaComboBox.getSelectionModel().getSelectedItem(),
                    tipoEnvioCheckBox.isSelected(), trackingCheckBox.isSelected(), clienteCheckBox.isSelected(), domicilioCheckBox.isSelected(), localidadCheckBox.isSelected(), telefonoCheckBox.isSelected());
            service.setOnRunning(e -> {
                generarButton.setDisable(true);
                progressIndicator.setVisible(true);
                logTextArea.setStyle("-fx-text-fill: darkblue;");
                logTextArea.appendText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")) + ": Generando PDF...\n");
            });
            service.setOnSucceeded(e -> {
                successSound.play();
                logTextArea.setStyle("-fx-text-fill: darkgreen;");
                logTextArea.appendText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")) + ": \"" + archivoDestino.getAbsolutePath() + "\" generado.\n");
                generarButton.setDisable(false);
                progressIndicator.setVisible(false);
                limpiarCampos();
                openPdfFile(archivoDestino.getAbsolutePath());
            });
            service.setOnFailed(e -> {
//                service.getException().printStackTrace();
                errorSound.play();
                logTextArea.setStyle("-fx-text-fill: firebrick;");
                logTextArea.appendText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")) + ": Error: " + service.getException().getLocalizedMessage() + "\n");
                generarButton.setDisable(false);
                progressIndicator.setVisible(false);
            });
            service.start();
        } else {
            logTextArea.setStyle("-fx-text-fill: firebrick;");
            logTextArea.appendText("Completa todos los campos correctamente.\n");
            tipoEnvioText.requestFocus();
        }
    }

    @FXML
    public void onClickTipoEnvioColumn(Event event) {
        deshabilitarColumna(tipoEnvioText, tipoEnvioCheckBox, tipoEnvioFontSize, tipoEnvioColorPicker, tipoEnvioFontComboBox);
    }

    @FXML
    public void onClickTrackingColumn(Event event) {
        deshabilitarColumna(trackingText, trackingCheckBox, trackingFontSize, trackingColorPicker, trackingFontComboBox);
    }

    @FXML
    public void onClickClienteColumn(Event event) {
        deshabilitarColumna(clienteText, clienteCheckBox, clienteFontSize, clienteColorPicker, clienteFontComboBox);
    }

    @FXML
    public void onClickDomicilioColumn(Event event) {
        deshabilitarColumna(domicilioText, domicilioCheckBox, domicilioFontSize, domicilioColorPicker, domicilioFontComboBox);
    }

    @FXML
    public void onClickLocalidadColumn(Event event) {
        deshabilitarColumna(localidadText, localidadCheckBox, localidadFontSize, localidadColorPicker, localidadFontComboBox);
        if (localidadCheckBox.isSelected()) {
            cpText.setDisable(false);
            observacionesText.setDisable(false);
        } else {
            cpText.setDisable(true);
            observacionesText.setDisable(true);
        }
    }

    @FXML
    public void onClickTelefonoColumn(Event event) {
        deshabilitarColumna(telefonoText, telefonoCheckBox, telefonoFontSize, telefonoColorPicker, telefonoFontComboBox);
    }

    @FXML
    public void onTipoEnvioColorChange(Event event) {
        tipoEnvioCheckBox.setTextFill(Paint.valueOf((tipoEnvioColorPicker.getValue().toString())));
    }

    @FXML
    public void onTrackingColorChange(Event event) {
        trackingCheckBox.setTextFill(Paint.valueOf((trackingColorPicker.getValue().toString())));
    }

    @FXML
    public void onClienteColorChange(Event event) {
        clienteCheckBox.setTextFill(Paint.valueOf((clienteColorPicker.getValue().toString())));
    }

    @FXML
    public void onDomicilioColorChange(Event event) {
        domicilioCheckBox.setTextFill(Paint.valueOf((domicilioColorPicker.getValue().toString())));
    }

    @FXML
    public void onLocalidadColorChange(Event event) {
        localidadCheckBox.setTextFill(Paint.valueOf((localidadColorPicker.getValue().toString())));
    }

    @FXML
    public void onTelefonoColorChange(Event event) {
        telefonoCheckBox.setTextFill(Paint.valueOf((telefonoColorPicker.getValue().toString())));
    }

    @FXML
    public void onTipoEnvioFontChange(Event event) {
        tipoEnvioCheckBox.setFont(Font.font(tipoEnvioFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void onTrackingFontChange(Event event) {
        trackingCheckBox.setFont(Font.font(trackingFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void onClienteFontChange(Event event) {
        clienteCheckBox.setFont(Font.font(clienteFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void onDomicilioFontChange(Event event) {
        domicilioCheckBox.setFont(Font.font(domicilioFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void onLocalidadFontChange(Event event) {
        localidadCheckBox.setFont(Font.font(localidadFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void onTelefonoFontChange(Event event) {
        telefonoCheckBox.setFont(Font.font(telefonoFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    @FXML
    public void enterPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            this.generarEtiqueta();
        }
    }

    @FXML
    public void limpiarCampos() {
        tipoEnvioText.clear();
        trackingText.clear();
        clienteText.clear();
        domicilioText.clear();
        localidadText.clear();
        cpText.clear();
        observacionesText.clear();
        telefonoText.clear();
        tipoEnvioText.requestFocus();
    }

    private void deshabilitarColumna(TextField textField, CheckBox checkBox, TextField fontSize, ColorPicker colorPicker, ComboBox<String> fontComboBox) {
        if (checkBox.isSelected()) {
            textField.setDisable(false);
            fontSize.setDisable(false);
            colorPicker.setDisable(false);
            fontComboBox.setDisable(false);
        } else {
            textField.setDisable(true);
            fontSize.setDisable(true);
            colorPicker.setDisable(true);
            fontComboBox.setDisable(true);
        }
    }

    private void loadPreferences() {
        final Preferences prefs = Preferences.userRoot().node("etiquetas");
        tipoEnvioFontSize.setText(prefs.get("tipoEnvioFontSize", "30"));
        trackingFontSize.setText(prefs.get("trackingFontSize", "30"));
        clienteFontSize.setText(prefs.get("clienteFontSize", "30"));
        domicilioFontSize.setText(prefs.get("domicilioFontSize", "30"));
        localidadFontSize.setText(prefs.get("localidadFontSize", "30"));
        telefonoFontSize.setText(prefs.get("telefonoFontSize", "30"));

        final String[] tipoEnvioColor = prefs.get("tipoEnvioColorPicker", "0,0,0").split(",");
        tipoEnvioColorPicker.setValue(new Color(Double.parseDouble(tipoEnvioColor[0]), Double.parseDouble(tipoEnvioColor[1]), Double.parseDouble(tipoEnvioColor[2]), 1));
        final String[] trackingColor = prefs.get("trackingColorPicker", "0,0,0").split(",");
        trackingColorPicker.setValue(new Color(Double.parseDouble(trackingColor[0]), Double.parseDouble(trackingColor[1]), Double.parseDouble(trackingColor[2]), 1));
        final String[] clienteColor = prefs.get("clienteColorPicker", "0,0,0").split(",");
        clienteColorPicker.setValue(new Color(Double.parseDouble(clienteColor[0]), Double.parseDouble(clienteColor[1]), Double.parseDouble(clienteColor[2]), 1));
        final String[] domicilioColor = prefs.get("domicilioColorPicker", "0,0,0").split(",");
        domicilioColorPicker.setValue(new Color(Double.parseDouble(domicilioColor[0]), Double.parseDouble(domicilioColor[1]), Double.parseDouble(domicilioColor[2]), 1));
        final String[] localidadColor = prefs.get("localidadColorPicker", "0,0,0").split(",");
        localidadColorPicker.setValue(new Color(Double.parseDouble(localidadColor[0]), Double.parseDouble(localidadColor[1]), Double.parseDouble(localidadColor[2]), 1));
        final String[] telefonoColor = prefs.get("telefonoColorPicker", "0,0,0").split(",");
        telefonoColorPicker.setValue(new Color(Double.parseDouble(telefonoColor[0]), Double.parseDouble(telefonoColor[1]), Double.parseDouble(telefonoColor[2]), 1));

        tipoEnvioFontComboBox.setValue(prefs.get("tipoEnvioFont", "Calibri"));
        trackingFontComboBox.setValue(prefs.get("trackingFont", "Calibri"));
        clienteFontComboBox.setValue(prefs.get("clienteFont", "Calibri"));
        domicilioFontComboBox.setValue(prefs.get("domicilioFont", "Calibri"));
        localidadFontComboBox.setValue(prefs.get("localidadFont", "Calibri"));
        telefonoFontComboBox.setValue(prefs.get("telefonoFont", "Calibri"));

        hojaComboBox.getSelectionModel().select(prefs.get("hojaComboBox", "A4"));

        tipoEnvioCheckBox.setSelected(prefs.getBoolean("tipoEnvioCheckBox", true));
        if (!tipoEnvioCheckBox.isSelected()) {
            tipoEnvioText.setDisable(true);
            tipoEnvioFontSize.setDisable(true);
            tipoEnvioColorPicker.setDisable(true);
            tipoEnvioFontComboBox.setDisable(true);
        }
        trackingCheckBox.setSelected(prefs.getBoolean("trackingCheckBox", true));
        if (!trackingCheckBox.isSelected()) {
            trackingText.setDisable(true);
            trackingFontSize.setDisable(true);
            trackingColorPicker.setDisable(true);
            trackingFontComboBox.setDisable(true);
        }
        clienteCheckBox.setSelected(prefs.getBoolean("clienteCheckBox", true));
        if (!clienteCheckBox.isSelected()) {
            clienteText.setDisable(true);
            clienteFontSize.setDisable(true);
            clienteColorPicker.setDisable(true);
            clienteFontComboBox.setDisable(true);
        }
        domicilioCheckBox.setSelected(prefs.getBoolean("domicilioCheckBox", true));
        if (!domicilioCheckBox.isSelected()) {
            domicilioText.setDisable(true);
            domicilioFontSize.setDisable(true);
            domicilioColorPicker.setDisable(true);
            domicilioFontComboBox.setDisable(true);
        }
        localidadCheckBox.setSelected(prefs.getBoolean("localidadCheckBox", true));
        if (!localidadCheckBox.isSelected()) {
            localidadText.setDisable(true);
            cpText.setDisable(true);
            observacionesText.setDisable(true);
            localidadFontSize.setDisable(true);
            localidadColorPicker.setDisable(true);
            localidadFontComboBox.setDisable(true);
        }
        telefonoCheckBox.setSelected(prefs.getBoolean("telefonoCheckBox", true));
        if (!telefonoCheckBox.isSelected()) {
            telefonoText.setDisable(true);
            telefonoFontSize.setDisable(true);
            telefonoColorPicker.setDisable(true);
            telefonoFontComboBox.setDisable(true);
        }

        tipoEnvioCheckBox.setTextFill(Paint.valueOf((tipoEnvioColorPicker.getValue().toString())));
        trackingCheckBox.setTextFill(Paint.valueOf((trackingColorPicker.getValue().toString())));
        clienteCheckBox.setTextFill(Paint.valueOf((clienteColorPicker.getValue().toString())));
        domicilioCheckBox.setTextFill(Paint.valueOf((domicilioColorPicker.getValue().toString())));
        localidadCheckBox.setTextFill(Paint.valueOf((localidadColorPicker.getValue().toString())));
        telefonoCheckBox.setTextFill(Paint.valueOf((telefonoColorPicker.getValue().toString())));

        tipoEnvioCheckBox.setFont(Font.font(tipoEnvioFontComboBox.getValue(), FontWeight.BOLD, 15));
        trackingCheckBox.setFont(Font.font(trackingFontComboBox.getValue(), FontWeight.BOLD, 15));
        clienteCheckBox.setFont(Font.font(clienteFontComboBox.getValue(), FontWeight.BOLD, 15));
        domicilioCheckBox.setFont(Font.font(domicilioFontComboBox.getValue(), FontWeight.BOLD, 15));
        localidadCheckBox.setFont(Font.font(localidadFontComboBox.getValue(), FontWeight.BOLD, 15));
        telefonoCheckBox.setFont(Font.font(telefonoFontComboBox.getValue(), FontWeight.BOLD, 15));
    }

    private void savePreferences() {
        // Save state to preferences when the application is closed
        final Preferences prefs = Preferences.userRoot().node("etiquetas");
        prefs.put("tipoEnvioFontSize", tipoEnvioFontSize.getText());
        prefs.put("trackingFontSize", trackingFontSize.getText());
        prefs.put("clienteFontSize", clienteFontSize.getText());
        prefs.put("domicilioFontSize", domicilioFontSize.getText());
        prefs.put("localidadFontSize", localidadFontSize.getText());
        prefs.put("telefonoFontSize", telefonoFontSize.getText());

        prefs.put("tipoEnvioColorPicker", tipoEnvioColorPicker.getValue().getRed() + "," + tipoEnvioColorPicker.getValue().getGreen() + "," + tipoEnvioColorPicker.getValue().getBlue());
        prefs.put("trackingColorPicker", trackingColorPicker.getValue().getRed() + "," + trackingColorPicker.getValue().getGreen() + "," + trackingColorPicker.getValue().getBlue());
        prefs.put("clienteColorPicker", clienteColorPicker.getValue().getRed() + "," + clienteColorPicker.getValue().getGreen() + "," + clienteColorPicker.getValue().getBlue());
        prefs.put("domicilioColorPicker", domicilioColorPicker.getValue().getRed() + "," + domicilioColorPicker.getValue().getGreen() + "," + domicilioColorPicker.getValue().getBlue());
        prefs.put("localidadColorPicker", localidadColorPicker.getValue().getRed() + "," + localidadColorPicker.getValue().getGreen() + "," + localidadColorPicker.getValue().getBlue());
        prefs.put("telefonoColorPicker", telefonoColorPicker.getValue().getRed() + "," + telefonoColorPicker.getValue().getGreen() + "," + telefonoColorPicker.getValue().getBlue());

        prefs.put("tipoEnvioFont", tipoEnvioFontComboBox.getValue());
        prefs.put("trackingFont", trackingFontComboBox.getValue());
        prefs.put("clienteFont", clienteFontComboBox.getValue());
        prefs.put("domicilioFont", domicilioFontComboBox.getValue());
        prefs.put("localidadFont", localidadFontComboBox.getValue());
        prefs.put("telefonoFont", telefonoFontComboBox.getValue());

        prefs.put("hojaComboBox", hojaComboBox.getSelectionModel().getSelectedItem());

        prefs.putBoolean("tipoEnvioCheckBox", tipoEnvioCheckBox.isSelected());
        prefs.putBoolean("trackingCheckBox", trackingCheckBox.isSelected());
        prefs.putBoolean("clienteCheckBox", clienteCheckBox.isSelected());
        prefs.putBoolean("domicilioCheckBox", domicilioCheckBox.isSelected());
        prefs.putBoolean("localidadCheckBox", localidadCheckBox.isSelected());
        prefs.putBoolean("telefonoCheckBox", telefonoCheckBox.isSelected());
    }

    private boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            Float.parseFloat(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private boolean validarTextInputs() {
        return !(tipoEnvioText.getText().isBlank() && trackingText.getText().isBlank() && clienteText.getText().isBlank() && domicilioText.getText().isBlank()
                && localidadText.getText().isBlank() && cpText.getText().isBlank() && observacionesText.getText().isBlank() && telefonoText.getText().isBlank())
                && isNumeric(tipoEnvioFontSize.getText()) && isNumeric(trackingFontSize.getText()) && isNumeric(clienteFontSize.getText()) && isNumeric(domicilioFontSize.getText())
                && isNumeric(localidadFontSize.getText()) && isNumeric(telefonoFontSize.getText())
                && hojaComboBox.getSelectionModel().getSelectedIndex() > -1;
    }

    private void inicializarComponentes() {
        // Get the list of available font families
        final List<String> fontFamilies = Font.getFamilies();
        // Populate the ChoiceBox with font families
        tipoEnvioFontComboBox.getItems().addAll(fontFamilies);
        trackingFontComboBox.getItems().addAll(fontFamilies);
        clienteFontComboBox.getItems().addAll(fontFamilies);
        domicilioFontComboBox.getItems().addAll(fontFamilies);
        localidadFontComboBox.getItems().addAll(fontFamilies);
        telefonoFontComboBox.getItems().addAll(fontFamilies);

        hojaComboBox.getItems().addAll(new String[]{"A4", "OFICIO", "EJECUTIVO", "LEGAL", "CARTA", "TABLOIDE"});

        errorSound = new AudioClip(getClass().getResource("/audios/error.mp3").toExternalForm());
        successSound = new AudioClip(getClass().getResource("/audios/success.mp3").toExternalForm());
        errorSound.setVolume(0.1);
        successSound.setVolume(0.1);

        loadPreferences(); // Load previous state from preferences
    }

    public void openPdfFile(String filePath) {
        final File file = new File(filePath);
        final Desktop desktop = Desktop.getDesktop();
        if (file.exists()) {
            try {
                desktop.open(file);
            } catch (IOException e) {
                logTextArea.setStyle("-fx-text-fill: firebrick;");
                logTextArea.appendText("Error al abrir el archivo.");
            }
        } else {
            logTextArea.setStyle("-fx-text-fill: firebrick;");
            logTextArea.appendText("Error: el archivo no existe.");
        }
    }

}