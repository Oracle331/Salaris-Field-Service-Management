package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
class StockLocation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public String name;
  public String type; // "office" or "truck"
  public LocalDate lastUpdated;
  public StockLocation() {}
  public StockLocation(String name, String type) {
    this.name = name;
    this.type = type;
    this.lastUpdated = LocalDate.now();
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StockLocation that = (StockLocation) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(name, that.name) &&
        Objects.equals(type, that.type);
  }
  @Override
  public int hashCode() {
    return Objects.hash(id, name, type);
  }
  @Override
  public String toString() {
    return "StockLocation{" +
        "id=" + id +
        ", name='" + name + '"' +
        ", type='" + type + '"' +
        ", lastUpdated=" + lastUpdated +
        '}';
  }
}