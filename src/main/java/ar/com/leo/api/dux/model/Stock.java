package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stock {

    @JsonProperty("id")
    private int id;

    @JsonProperty("nombre")
    private String nombre;

    @JsonProperty("ctd_disponible")
    private String ctdDisponible;

    @JsonProperty("stock_real")
    private String stockReal;

    @JsonProperty("stock_reservado")
    private String stockReservado;

    @JsonProperty("stock_disponible")
    private String stockDisponible;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCtdDisponible() { return ctdDisponible; }
    public void setCtdDisponible(String ctdDisponible) { this.ctdDisponible = ctdDisponible; }
    public String getStockReal() { return stockReal; }
    public void setStockReal(String stockReal) { this.stockReal = stockReal; }
    public String getStockReservado() { return stockReservado; }
    public void setStockReservado(String stockReservado) { this.stockReservado = stockReservado; }
    public String getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(String stockDisponible) { this.stockDisponible = stockDisponible; }
}
