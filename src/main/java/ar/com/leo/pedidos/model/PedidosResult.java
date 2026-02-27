package ar.com.leo.pedidos.model;

import java.util.List;

public record PedidosResult(List<PedidoML> pedidosML, List<PedidoTN> pedidosTN,
                            List<EtiquetaTN> etiquetasTN) {}
