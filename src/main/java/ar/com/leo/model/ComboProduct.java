package ar.com.leo.model;

import java.util.List;

public record ComboProduct(String codigoCompuesto, String productoCompuesto, List<ComboComponent> componentes) {
}
