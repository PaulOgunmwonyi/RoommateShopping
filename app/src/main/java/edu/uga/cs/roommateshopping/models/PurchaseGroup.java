/**
 * Model class representing a group of items purchased together.
 * Used for settlement calculations and purchase history.
 */
package edu.uga.cs.roommateshopping.models;

import java.util.List;

public class PurchaseGroup {
    private String key;
    private String purchasedBy;
    private List<ShoppingItem> items;
    private double totalPrice;
    private boolean settled;
    private String purchaseDate;

    public PurchaseGroup() {
    }

    public PurchaseGroup(String purchasedBy, List<ShoppingItem> items, double totalPrice) {
        this.purchasedBy = purchasedBy;
        this.items = items;
        this.totalPrice = totalPrice;
        this.settled = false;
    }

    public String getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(String purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPurchasedBy() {
        return purchasedBy;
    }

    public void setPurchasedBy(String purchasedBy) {
        this.purchasedBy = purchasedBy;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
