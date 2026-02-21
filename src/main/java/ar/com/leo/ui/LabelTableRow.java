package ar.com.leo.ui;

import javafx.beans.property.*;

public class LabelTableRow {

    private final StringProperty zone;
    private final StringProperty sku;
    private final StringProperty productDescription;
    private final StringProperty details;
    private final IntegerProperty quantity;

    public LabelTableRow(String zone, String sku, String productDescription, String details, int quantity) {
        this.zone = new SimpleStringProperty(zone);
        this.sku = new SimpleStringProperty(sku);
        this.productDescription = new SimpleStringProperty(productDescription);
        this.details = new SimpleStringProperty(details);
        this.quantity = new SimpleIntegerProperty(quantity);
    }

    public StringProperty zoneProperty() { return zone; }
    public StringProperty skuProperty() { return sku; }
    public StringProperty productDescriptionProperty() { return productDescription; }
    public StringProperty detailsProperty() { return details; }
    public IntegerProperty quantityProperty() { return quantity; }

    public String getZone() { return zone.get(); }
    public String getSku() { return sku.get(); }
    public String getProductDescription() { return productDescription.get(); }
    public String getDetails() { return details.get(); }
    public int getQuantity() { return quantity.get(); }
}
