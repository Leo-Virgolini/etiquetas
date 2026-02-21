package ar.com.leo.api.ml.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdenML {

    private final long orderId;
    private final Long packId;
    private final Long shipmentId;
    private final OffsetDateTime fecha;
    private final List<Venta> items = new ArrayList<>();

    public OrdenML(long orderId, Long packId, Long shipmentId, OffsetDateTime fecha) {
        this.orderId = orderId;
        this.packId = packId;
        this.shipmentId = shipmentId;
        this.fecha = fecha;
    }

    public long getOrderId() { return orderId; }
    public Long getPackId() { return packId; }
    public Long getShipmentId() { return shipmentId; }
    public OffsetDateTime getFecha() { return fecha; }
    public List<Venta> getItems() { return items; }
}
