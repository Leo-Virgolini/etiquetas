package ar.com.leo.pedidos.model;

import java.time.OffsetDateTime;

public record PedidoTN(long orderId, OffsetDateTime fecha, String nombreApellido,
                       String sku, double cantidad, String detalle,
                       String tienda, String tipoEnvio) {}
