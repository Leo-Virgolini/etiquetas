package ar.com.leo.pickit.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class ProductoManual {

    private final SimpleStringProperty sku;
    private final SimpleDoubleProperty cantidad;

    public ProductoManual(String sku, double cantidad) {
        this.sku = new SimpleStringProperty(sku);
        this.cantidad = new SimpleDoubleProperty(cantidad);
    }

    public String getSku() { return sku.get(); }
    public void setSku(String sku) { this.sku.set(sku); }
    public SimpleStringProperty skuProperty() { return sku; }

    public double getCantidad() { return cantidad.get(); }
    public void setCantidad(double cantidad) { this.cantidad.set(cantidad); }
    public SimpleDoubleProperty cantidadProperty() { return cantidad; }
}
