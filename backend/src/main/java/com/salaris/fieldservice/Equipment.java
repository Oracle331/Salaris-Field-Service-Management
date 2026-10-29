package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.util.*;

@Entity
class Equipment {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public Long clientId; public Long locationId; public String assetId;
  public String name, brand, model, serialNumber, location, filterSize, fittingSize, type;
  public Equipment() {}
  Equipment(String name, String brand, String model, String serialNumber, String location, String filterSize, String fittingSize, String type) {
    this.name = name;
    this.brand = brand;
    this.model = model;
    this.serialNumber = serialNumber;
    this.location = location;
    this.filterSize = filterSize;
    this.fittingSize = fittingSize;
    this.type = type;
  }
  public boolean isReusable() {
    return Objects.equals(this.type, "REUSABLE");
  }
  public boolean isConsumable() {
    return Objects.equals(this.type, "CONSUMABLE");
  }
  public boolean isUnknownSize() {
    return this.filterSize == null || this.filterSize.trim().isEmpty();
  }

  public boolean isUnknownQuantity() {
    return this.quantity == null || this.quantity <= 0;
  }
  public boolean isUnknownQuantity() {
    return this.quantity == null || this.quantity <= 0;
  }
    return Objects.equals(this.type, "REUSABLE");
  }
  public boolean isConsumable() {
    return Objects.equals(this.type, "CONSUMABLE");
  }
}