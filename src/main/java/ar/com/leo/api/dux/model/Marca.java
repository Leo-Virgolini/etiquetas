package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Marca {

    @JsonProperty("codigo_marca")
    private String codigoMarca;

    @JsonProperty("marca")
    private String marca;

    public String getCodigoMarca() { return codigoMarca; }
    public void setCodigoMarca(String codigoMarca) { this.codigoMarca = codigoMarca; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
}
