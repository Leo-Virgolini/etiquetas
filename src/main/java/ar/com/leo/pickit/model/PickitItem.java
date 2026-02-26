package ar.com.leo.pickit.model;

public class PickitItem {

    private String codigo;
    private double cantidad;
    private String descripcion;
    private String proveedor;
    private String unidad;
    private double stockDisponible;
    private String subRubro;

    public PickitItem(String codigo, double cantidad, String descripcion, String proveedor,
                      String unidad, double stockDisponible, String subRubro) {
        this.codigo = codigo;
        this.cantidad = cantidad;
        this.descripcion = descripcion;
        this.proveedor = proveedor;
        this.unidad = unidad;
        this.stockDisponible = stockDisponible;
        this.subRubro = subRubro;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public double getCantidad() { return cantidad; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }
    public double getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(double stockDisponible) { this.stockDisponible = stockDisponible; }
    public String getSubRubro() { return subRubro; }
    public void setSubRubro(String subRubro) { this.subRubro = subRubro; }

    @Override
    public String toString() {
        return "PickitItem{codigo='" + codigo + "', cantidad=" + cantidad +
                ", descripcion='" + descripcion + "', proveedor='" + proveedor +
                "', unidad='" + unidad + "', stockDisponible=" + stockDisponible +
                ", subRubro='" + subRubro + "'}";
    }
}
