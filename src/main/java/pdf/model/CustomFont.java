package pdf.model;

import com.itextpdf.kernel.colors.DeviceRgb;
import javafx.scene.paint.Color;

public class CustomFont {

    public CustomFont(Float size, Color color, String family) {
        this.size = size;
        this.color = color;
        this.family = family;
    }

    private Float size;
    private Color color;
    private String family;

    public DeviceRgb getRGB() {
        return new DeviceRgb((int) (color.getRed() * 255), (int) (color.getGreen() * 255), (int) (color.getBlue() * 255));
    }

    public Float getSize() {
        return size;
    }

    public void setSize(Float size) {
        this.size = size;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

}
