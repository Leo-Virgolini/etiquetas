package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Item {

    @JsonProperty("cod_item")
    private String codItem;

    @JsonProperty("item")
    private String item;

    @JsonProperty("codigos_barra")
    private List<String> codigosBarra;

    @JsonProperty("rubro")
    private Rubro rubro;

    @JsonProperty("sub_rubro")
    private SubRubro subRubro;

    @JsonProperty("marca")
    private Marca marca;

    @JsonProperty("proveedor")
    private Proveedor proveedor;

    @JsonProperty("costo")
    private String costo;

    @JsonProperty("porc_iva")
    private String porcIva;

    @JsonProperty("precios")
    private List<Precio> precios;

    @JsonProperty("stock")
    private List<Stock> stock;

    @JsonProperty("id_det_item")
    private String idDetItem;

    @JsonProperty("talle")
    private String talle;

    @JsonProperty("color")
    private String color;

    @JsonProperty("habilitado")
    private String habilitado;

    @JsonProperty("codigo_externo")
    private String codigoExterno;

    @JsonProperty("fecha_creacion")
    private String fechaCreacion;

    @JsonProperty("imagen_url")
    private String imagenUrl;

    public String getCodItem() { return codItem; }
    public void setCodItem(String codItem) { this.codItem = codItem; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public List<String> getCodigosBarra() { return codigosBarra; }
    public void setCodigosBarra(List<String> codigosBarra) { this.codigosBarra = codigosBarra; }
    public Rubro getRubro() { return rubro; }
    public void setRubro(Rubro rubro) { this.rubro = rubro; }
    public SubRubro getSubRubro() { return subRubro; }
    public void setSubRubro(SubRubro subRubro) { this.subRubro = subRubro; }
    public Marca getMarca() { return marca; }
    public void setMarca(Marca marca) { this.marca = marca; }
    public Proveedor getProveedor() { return proveedor; }
    public void setProveedor(Proveedor proveedor) { this.proveedor = proveedor; }
    public String getCosto() { return costo; }
    public void setCosto(String costo) { this.costo = costo; }
    public String getPorcIva() { return porcIva; }
    public void setPorcIva(String porcIva) { this.porcIva = porcIva; }
    public List<Precio> getPrecios() { return precios; }
    public void setPrecios(List<Precio> precios) { this.precios = precios; }
    public List<Stock> getStock() { return stock; }
    public void setStock(List<Stock> stock) { this.stock = stock; }
    public String getIdDetItem() { return idDetItem; }
    public void setIdDetItem(String idDetItem) { this.idDetItem = idDetItem; }
    public String getTalle() { return talle; }
    public void setTalle(String talle) { this.talle = talle; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getHabilitado() { return habilitado; }
    public void setHabilitado(String habilitado) { this.habilitado = habilitado; }
    public String getCodigoExterno() { return codigoExterno; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
