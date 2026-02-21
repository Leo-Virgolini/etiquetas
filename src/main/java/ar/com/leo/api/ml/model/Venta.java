package ar.com.leo.api.ml.model;

public class Venta {

    private final String sku;
    private final double cantidad;
    private final String origen;
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
    public double getCantidad() { return cantidad; }
    public String getOrigen() { return origen; }
    public String getTitulo() { return titulo; }

    @Override
    public String toString() {
        return sku + " x" + (int) cantidad + " (" + origen + ")";
    }
}
