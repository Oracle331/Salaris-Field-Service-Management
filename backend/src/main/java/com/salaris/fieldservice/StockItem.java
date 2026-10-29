package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
class StockItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public Long stockLocationId;
  public String name;
  public String kind; // "consumable" or "reusable"
  public String filterSize;
  public Integer quantity;
  public LocalDate lastUpdated;
  public StockItem() {}
  public StockItem(Long stockLocationId, String name, String kind, String filterSize, Integer quantity) {
    this.stockLocationId = stockLocationId;
    this.name = name;
    this.kind = kind;
    this.filterSize = filterSize;
    this.quantity = quantity;
    this.lastUpdated = LocalDate.now();
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StockItem that = (StockItem) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(stockLocationId, that.stockLocationId) &&
        Objects.equals(name, that.name) &&
        Objects.equals(kind, that.kind) &&
        Objects.equals(filterSize, that.filterSize) &&
        Objects.equals(quantity, that.quantity);
  }
  @Override
  public int hashCode() {
    return Objects.hash(id, stockLocationId, name, kind, filterSize, quantity);
  }
  @Override
  public String toString() {
    return "StockItem{" +
        "id=" + id +
        ", stockLocationId=" + stockLocationId +
        ", name='" + name + '"' +
        ", kind='" + kind + '"' +
        ", filterSize='" + filterSize + '"' +
        ", quantity=" + quantity +
        ", lastUpdated=" + lastUpdated +
        '}';
  }
}