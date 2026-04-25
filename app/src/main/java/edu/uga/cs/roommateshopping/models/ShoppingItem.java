package edu.uga.cs.roommateshopping.models;

import java.io.Serializable;

public class ShoppingItem implements Serializable {
    private String key;
    private String name;
    private String quantity;
    private String price;
    private String purchasedBy;
    private String imageUrl;

    public ShoppingItem() {
        this.key = null;
        this.name = null;
        this.quantity = null;
        this.price = null;
        this.purchasedBy = null;
        this.imageUrl = null;
    }

    public ShoppingItem(String name, String quantity) {
        this.key = null;
        this.name = name;
        this.quantity = quantity;
        this.price = null;
        this.purchasedBy = null;
        this.imageUrl = null;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(String purchasedBy) {
        this.purchasedBy = purchasedBy;
    }
}
