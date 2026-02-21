package ar.com.leo.api.ml.model;

public class Venta {

    private final String sku;
    private final double cantidad;
    private final String origen;

    public Venta(String sku, double cantidad, String origen) {
        this.sku = sku;
        this.cantidad = cantidad;
        this.origen = origen;
    }

    public String getSku() { return sku; }
    public double getCantidad() { return cantidad; }
    public String getOrigen() { return origen; }

    @Override
    public String toString() {
        return sku + " x" + (int) cantidad + " (" + origen + ")";
    }
}
