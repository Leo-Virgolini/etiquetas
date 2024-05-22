package service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import pdf.model.CustomFont;

import java.io.File;

public class GeneratePDFService extends Service<Void> {

    private final File archivoDestino;
    private final String tipoEnvioText;
    private final String trackingText;
    private final String clienteText;
    private final String domicilioText;
    private final String localidadText;
    private final String cpText;
    private final String observacionesText;
    private final String telefonoText;
    private final CustomFont tipoEnvioFont;
    private final CustomFont trackingFont;
    private final CustomFont clienteFont;
    private final CustomFont domicilioFont;
    private final CustomFont localidadFont;
    private final CustomFont telefonoFont;
    private final String hoja;
    private final boolean tipoEnvioColumn;
    private final boolean trackingColumn;
    private final boolean clienteColumn;
    private final boolean domicilioColumn;
    private final boolean localidadColumn;
    private final boolean telefonoColumn;

    public GeneratePDFService(File archivoDestino, String tipoEnvioText, String trackingText, String clienteText, String domicilioText, String localidadText, String cpText, String observacionesText, String telefonoText, CustomFont tipoEnvioFont,
                              CustomFont trackingFont, CustomFont clienteFont, CustomFont domicilioFont, CustomFont localidadFont, CustomFont telefonoFont, String hoja,
                              boolean tipoEnvioColumn, boolean trackingColumn, boolean clienteColumn, boolean domicilioColumn, boolean localidadColumn, boolean telefonoColumn) {
        this.archivoDestino = archivoDestino;
        this.tipoEnvioText = tipoEnvioText;
        this.trackingText = trackingText;
        this.clienteText = clienteText;
        this.domicilioText = domicilioText;
        this.localidadText = localidadText;
        this.cpText = cpText;
        this.observacionesText = observacionesText;
        this.telefonoText = telefonoText;
        this.tipoEnvioFont = tipoEnvioFont;
        this.trackingFont = trackingFont;
        this.clienteFont = clienteFont;
        this.domicilioFont = domicilioFont;
        this.localidadFont = localidadFont;
        this.telefonoFont = telefonoFont;
        this.hoja = hoja;
        this.tipoEnvioColumn = tipoEnvioColumn;
        this.trackingColumn = trackingColumn;
        this.clienteColumn = clienteColumn;
        this.domicilioColumn = domicilioColumn;
        this.localidadColumn = localidadColumn;
        this.telefonoColumn = telefonoColumn;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                return generarPDF(archivoDestino,
                        tipoEnvioText, trackingText, clienteText, domicilioText, localidadText, cpText, observacionesText, telefonoText,
                        tipoEnvioFont, trackingFont, clienteFont, domicilioFont, localidadFont, telefonoFont,
                        hoja,
                        tipoEnvioColumn, trackingColumn, clienteColumn, domicilioColumn, localidadColumn, telefonoColumn);
            }
        };
    }

    private Void generarPDF(File archivoDestino, String tipoEnvioText, String trackingText, String clienteText, String domicilioText, String localidadText,
                            String cpText, String observacionesText, String telefonoText,
                            CustomFont tipoEnvioFont, CustomFont trackingFont, CustomFont clienteFont, CustomFont domicilioFont, CustomFont localidadFont, CustomFont telefonoFont,
                            String hoja,
                            boolean tipoEnvioColumn, boolean trackingColumn, boolean clienteColumn, boolean domicilioColumn, boolean localidadColumn, boolean telefonoColumn) throws Exception {

        PageSize pageSize;
        switch (hoja) {
            case "EJECUTIVO":
                pageSize = new PageSize(PageSize.EXECUTIVE);
                break;
            case "OFICIO":
                pageSize = new PageSize(612, 934);
                break;
            case "LEGAL":
                pageSize = new PageSize(PageSize.LEGAL);
                break;
            case "CARTA":
                pageSize = new PageSize(PageSize.LETTER);
                break;
            case "TABLOIDE":
                pageSize = new PageSize(PageSize.TABLOID);
                break;
            case "A4":
            default:
                pageSize = new PageSize(PageSize.A4);
                break;
        }

        // Create a PDF document
        try (final PdfDocument pdfDoc = new PdfDocument(new PdfWriter(archivoDestino.getAbsolutePath()));
             final Document doc = new Document(pdfDoc, pageSize)) {
            // Width: 15cm: 425, Height: 20cm: 566.8
            // Set page orientation to landscape
            doc.getPdfDocument().setDefaultPageSize(doc.getPdfDocument().getDefaultPageSize().rotate());

            final FontProvider fontProvider = new BasicFontProvider(true, true);
            doc.setFontProvider(fontProvider);
            doc.setMargins(0, 0, 0, 0);

            if (tipoEnvioColumn) {
                final Paragraph tipoEnvio = new Paragraph(new Text("TIPO DE ENVIO:").setBold().setUnderline());
                tipoEnvio.setFontFamily(tipoEnvioFont.getFamily())
                        .setFontSize(tipoEnvioFont.getSize())
                        .setFontColor(tipoEnvioFont.getRGB());
                tipoEnvio.add(new Text(" " + tipoEnvioText.toUpperCase()).setBold())
                        .setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(tipoEnvio);
            }

            if (trackingColumn) {
                final Paragraph tracking = new Paragraph(new Text("Nº TRACKING:").setBold().setUnderline());
                tracking.setFontFamily(trackingFont.getFamily())
                        .setFontSize(trackingFont.getSize())
                        .setFontColor(trackingFont.getRGB());
                tracking.add(new Text(" " + trackingText).setBold())
                        .setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(tracking);
            }

            if (clienteColumn) {
                final Paragraph cliente = new Paragraph(new Text("CLIENTE:").setBold().setUnderline());
                cliente.setFontFamily(clienteFont.getFamily())
                        .setFontSize(clienteFont.getSize())
                        .setFontColor(clienteFont.getRGB());
                cliente.add(new Text(" " + clienteText).setBold())
                        .setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(cliente);
            }

            if (domicilioColumn) {
                final Paragraph domicilio = new Paragraph(new Text("DOMICILIO:").setBold().setUnderline());
                domicilio.setFontFamily(domicilioFont.getFamily())
                        .setFontSize(domicilioFont.getSize())
                        .setFontColor(domicilioFont.getRGB());
                domicilio.add(new Text(" " + domicilioText).setBold())
                        .setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(domicilio);
            }

            if (localidadColumn) {
                final Paragraph datos = new Paragraph(new Text("LOCALIDAD | CP | OBSERVACIONES:").setBold().setUnderline());
                datos.setFontFamily(localidadFont.getFamily())
                        .setFontSize(localidadFont.getSize())
                        .setFontColor(localidadFont.getRGB());
                if (!localidadText.isBlank()) {
                    Text localidad = new Text(" " + localidadText);
                    datos.add(localidad.setBold());
                }
                if (!cpText.isBlank()) {
                    Text cp = new Text(" | " + cpText);
                    datos.add(cp.setBold());
                }
                if (!observacionesText.isBlank()) {
                    Text observaciones = new Text(" | " + observacionesText);
                    datos.add(observaciones.setBold());
                }
                datos.setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(datos);
            }

            if (telefonoColumn) {
                final Paragraph telefono = new Paragraph(new Text("TELEFONO:").setBold().setUnderline());
                telefono.setFontFamily(telefonoFont.getFamily())
                        .setFontSize(telefonoFont.getSize())
                        .setFontColor(telefonoFont.getRGB());
                telefono.add(new Text(" " + telefonoText).setBold())
                        .setTextAlignment(TextAlignment.CENTER).setMargin(10);
                doc.add(telefono);
            }
        } catch (Exception e) {
            throw e;
        }

        return null;
    }

}