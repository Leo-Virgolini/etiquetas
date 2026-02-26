package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SubRubro {

    @JsonProperty("id")
    private String id;

    @JsonProperty("nombre")
    private String nombre;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
