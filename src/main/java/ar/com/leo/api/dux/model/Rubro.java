package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Rubro {

    @JsonProperty("id")
    private String id;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("sub_rubro")
    private SubRubro subRubro;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public SubRubro getSubRubro() { return subRubro; }
    public void setSubRubro(SubRubro subRubro) { this.subRubro = subRubro; }
}
