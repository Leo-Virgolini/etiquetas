package ar.com.leo.pickit.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenML {

    private final long orderId;
    private final Long packId;
    private final Long shipmentId;
    private final OffsetDateTime fechaCreacion;
    private final List<Venta> items;

    public OrdenML(long orderId, Long packId, Long shipmentId, OffsetDateTime fechaCreacion) {
        this.orderId = orderId;
        this.packId = packId;
        this.shipmentId = shipmentId;
        this.fechaCreacion = fechaCreacion;
        this.items = new ArrayList<>();
    }

    public long getOrderId() { return orderId; }
    public Long getPackId() { return packId; }
    public Long getShipmentId() { return shipmentId; }
    public OffsetDateTime getFechaCreacion() { return fechaCreacion; }
    public List<Venta> getItems() { return items; }

    public long getVentaId() {
        return packId != null ? packId : orderId;
    }

    public String getNumeroVenta() {
        String id = String.valueOf(getVentaId());
        return id.length() > 5 ? id.substring(id.length() - 5) : id;
    }
}
