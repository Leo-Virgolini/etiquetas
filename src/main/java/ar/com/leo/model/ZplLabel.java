package ar.com.leo.model;

public record ZplLabel(String rawZpl, String sku, String productDescription, String details, int quantity) {

    public ZplLabel(String rawZpl, String sku, String productDescription, String details) {
        this(rawZpl, sku, productDescription, details, 1);
    }
}
