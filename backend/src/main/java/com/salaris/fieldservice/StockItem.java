package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "stock_item")
public class StockItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public String name;
    public String location;
    public String filterSize;
    public String kind;
    public int quantity;
    public LocalDate lastUpdated;
    public Long version;

    public StockItem(String name, String location, String filterSize, String kind, int quantity, LocalDate lastUpdated, Long version) {
        this.name = name;
        this.location = location;
        this.filterSize = filterSize;
        this.kind = kind;
        this.quantity = quantity;
        this.lastUpdated = lastUpdated;
        this.version = version;
    }

    public StockItem(String name, String location, String filterSize, String kind, int quantity) {
        this.name = name;
        this.location = location;
        this.filterSize = filterSize;
        this.kind = kind;
        this.quantity = quantity;
        this.lastUpdated = LocalDate.now();
        this.version = 1L;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFilterSize() {
        return filterSize;
    }

    public void setFilterSize(String filterSize) {
        this.filterSize = filterSize;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDate getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDate lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockItem)) return false;
        StockItem that = (StockItem) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(location, that.location) &&
               Objects.equals(filterSize, that.filterSize) &&
               Objects.equals(kind, that.kind) &&
               quantity == that.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, filterSize, kind, quantity);
    }
}