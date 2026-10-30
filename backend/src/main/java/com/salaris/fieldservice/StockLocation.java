package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.Objects;

@Entity
class StockLocation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public String name;
  public String type; // 'office' or 'truck'
  public String locationCode;
  public LocalDate lastUpdated;

  public StockLocation() {}

  public StockLocation(String name, String type, String locationCode) {
    this.name = name;
    this.type = type;
    this.locationCode = locationCode;
    this.lastUpdated = LocalDate.now();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StockLocation)) return false;
    StockLocation stockLocation = (StockLocation) o;
    return Objects.equals(name, stockLocation.name) &&
        Objects.equals(type, stockLocation.type) &&
        Objects.equals(locationCode, stockLocation.locationCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, locationCode);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLocationCode() {
    return locationCode;
  }

  public void setLocationCode(String locationCode) {
    this.locationCode = locationCode;
  }

  public LocalDate getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(LocalDate lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}