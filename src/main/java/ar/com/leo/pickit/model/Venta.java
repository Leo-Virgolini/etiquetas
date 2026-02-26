package ar.com.leo.pickit.model;

public class Venta {

    private String sku;
    private double cantidad;
    private String origen;

    public Venta(String sku, double cantidad, String origen) {
        this.sku = sku;
        this.cantidad = cantidad;
        this.origen = origen;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    @Override
    public String toString() {
        return "Venta{sku='" + sku + "', cantidad=" + cantidad + ", origen='" + origen + "'}";
    }
}
