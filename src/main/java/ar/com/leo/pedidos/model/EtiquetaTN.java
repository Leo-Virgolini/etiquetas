package ar.com.leo.pedidos.model;

import java.time.OffsetDateTime;

public record EtiquetaTN(long orderId, OffsetDateTime fecha, String nombreApellido,
                         String domicilio, String localidad, String codigoPostal,
                         String telefono, String observaciones) {}
