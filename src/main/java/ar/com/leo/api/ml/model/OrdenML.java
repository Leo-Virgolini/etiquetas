package ar.com.leo.api.ml.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenML {

    private final long orderId;
    private final Long packId;
    private final Long shipmentId;
    private final OffsetDateTime fecha;
    private String shippingSubstatus;
    private final List<Venta> items = new ArrayList<>();

    public OrdenML(long orderId, Long packId, Long shipmentId, OffsetDateTime fecha, String shippingSubstatus) {
        this.orderId = orderId;
        this.packId = packId;
        this.shipmentId = shipmentId;
        this.fecha = fecha;
        this.shippingSubstatus = shippingSubstatus;
    }

    public long getOrderId() { return orderId; }
    public Long getPackId() { return packId; }
    public Long getShipmentId() { return shipmentId; }
    public OffsetDateTime getFecha() { return fecha; }
    public String getShippingSubstatus() { return shippingSubstatus; }
    public void setShippingSubstatus(String shippingSubstatus) { this.shippingSubstatus = shippingSubstatus; }
    public List<Venta> getItems() { return items; }

    public long getVentaId() {
        return packId != null ? packId : orderId;
    }

    public String getNumeroVenta() {
        String id = String.valueOf(getVentaId());
        return id.length() > 5 ? id.substring(id.length() - 5) : id;
    }

    public OffsetDateTime getFechaCreacion() { return fecha; }
}
