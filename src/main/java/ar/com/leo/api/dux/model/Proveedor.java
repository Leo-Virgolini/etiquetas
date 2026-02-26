package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Proveedor {

    @JsonProperty("id_proveedor")
    private Integer idProveedor;

    @JsonProperty("proveedor")
    private String proveedor;

    @JsonProperty("tipo_doc")
    private String tipoDoc;

    @JsonProperty("nro_doc")
    private String nroDoc;

    @JsonProperty("provincia")
    private String provincia;

    @JsonProperty("localidad")
    private String localidad;

    @JsonProperty("domicilio")
    private String domicilio;

    @JsonProperty("barrio")
    private String barrio;

    @JsonProperty("cod_postal")
    private String codPostal;

    @JsonProperty("telefono")
    private String telefono;

    @JsonProperty("fax")
    private String fax;

    @JsonProperty("compania_celular")
    private String companiaCelular;

    @JsonProperty("cel")
    private String cel;

    @JsonProperty("persona_contacto")
    private String personaContacto;

    @JsonProperty("email")
    private String email;

    @JsonProperty("pagina_web")
    private String paginaWeb;

    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer idProveedor) { this.idProveedor = idProveedor; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }
    public String getNroDoc() { return nroDoc; }
    public void setNroDoc(String nroDoc) { this.nroDoc = nroDoc; }
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }
    public String getDomicilio() { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }
    public String getBarrio() { return barrio; }
    public void setBarrio(String barrio) { this.barrio = barrio; }
    public String getCodPostal() { return codPostal; }
    public void setCodPostal(String codPostal) { this.codPostal = codPostal; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getCompaniaCelular() { return companiaCelular; }
    public void setCompaniaCelular(String companiaCelular) { this.companiaCelular = companiaCelular; }
    public String getCel() { return cel; }
    public void setCel(String cel) { this.cel = cel; }
    public String getPersonaContacto() { return personaContacto; }
    public void setPersonaContacto(String personaContacto) { this.personaContacto = personaContacto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPaginaWeb() { return paginaWeb; }
    public void setPaginaWeb(String paginaWeb) { this.paginaWeb = paginaWeb; }
}
