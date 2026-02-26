package ar.com.leo.pickit.model;

public class CarrosItem {

    private final String sku;
    private final double cantidad;
    private final String descripcion;
    private final String sector;

    public CarrosItem(String sku, double cantidad, String descripcion, String sector) {
        this.sku = sku;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.sector = sector;
    }

    public String getSku() { return sku; }
    public double getCantidad() { return cantidad; }
    public String getDescripcion() { return descripcion; }
    public String getSector() { return sector; }
}
