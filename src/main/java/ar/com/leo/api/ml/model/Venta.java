package ar.com.leo.api.ml.model;

public class Venta {

    private String sku;
    private double cantidad;
    private String origen;
    private final String titulo;

    public Venta(String sku, double cantidad, String origen) {
        this(sku, cantidad, origen, "");
    }

    public Venta(String sku, double cantidad, String origen, String titulo) {
        this.sku = sku;
        this.cantidad = cantidad;
        this.origen = origen;
        this.titulo = titulo;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getTitulo() { return titulo; }

    @Override
    public String toString() {
        return sku + " x" + (int) cantidad + " (" + origen + ")";
    }
}
