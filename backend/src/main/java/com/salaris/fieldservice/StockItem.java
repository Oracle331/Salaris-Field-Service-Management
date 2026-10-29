package com.salaris.fieldservice;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.util.Date;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

package com.salaris.fieldservice;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.util.*;

@Entity
@Table(name = "stock_item")
class StockItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Version
  private Long version;
  private String name;
  private String brand;
  private String model;
  private String serialNumber;
  private String location;
  private String filterSize;
  private String fittingSize;
  private Long quantity;
  private String type; // 'consumable' or 'tool'
  private String stockLocation; // 'office' or 'truck'
  private Boolean isKnownSize;
  private Boolean isKnownQuantity;
  public StockItem() {} // Default constructor
  public StockItem(String name, String brand, String model, String serial, String location, String filter, String fitting, Long quantity, String type, String stockLocation, Boolean isKnownSize, Boolean isKnownQuantity) {
    this.name = name;
    this.brand = brand;
    this.model = model;
    this.serialNumber = serial;
    this.location = location;
    this.filterSize = filter;
    this.fittingSize = fitting;
    this.quantity = quantity;
    this.type = type;
    this.stockLocation = stockLocation;
    this.isKnownSize = isKnownSize;
    this.isKnownQuantity = isKnownQuantity;
  }
}
    this.name=name;this.brand=brand;this.model=model;this.serialNumber=serial;this.location=location;this.filterSize=filter;this.fittingSize=fitting;this.quantity=quantity;this.type=type;this.stockLocation=stockLocation;this.isKnownSize=isKnownSize;this.isKnownQuantity=isKnownQuantity;
  }
}
interface StockItemRepository extends JpaRepository<StockItem, Long> {}