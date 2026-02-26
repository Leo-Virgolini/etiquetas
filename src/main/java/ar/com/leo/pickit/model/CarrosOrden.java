package ar.com.leo.pickit.model;

import java.time.OffsetDateTime;
import java.util.List;

public class CarrosOrden {

    private final String numeroVenta;
    private final OffsetDateTime fechaCreacion;
    private final String letraCarro;
    private final List<CarrosItem> items;

    public CarrosOrden(String numeroVenta, OffsetDateTime fechaCreacion, String letraCarro, List<CarrosItem> items) {
        this.numeroVenta = numeroVenta;
        this.fechaCreacion = fechaCreacion;
        this.letraCarro = letraCarro;
        this.items = items;
    }

    public String getNumeroVenta() { return numeroVenta; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public String getLetraCarro() { return letraCarro; }
    public List<CarrosItem> getItems() { return items; }
}
