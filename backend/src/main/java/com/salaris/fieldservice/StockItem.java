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

@Entity
@Entity
class StockItem {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public String name;
  public String brand;
  public String model;
  public String serialNumber;
  public String location;
  public String filterSize;
  public String fittingSize;
  public Long quantity;
  public String type; // 'consumable' or 'tool'
  public String stockLocation; // 'office' or 'truck'
  public Boolean isKnownSize;
  public Boolean isKnownQuantity;
  public StockItem() {}
  StockItem(String name,String brand,String model,String serial,String location,String filter,String fitting,Long quantity,String type,String stockLocation,Boolean isKnownSize,Boolean isKnownQuantity) {
    this.name=name;this.brand=brand;this.model=model;this.serialNumber=serial;this.location=location;this.filterSize=filter;this.fittingSize=fitting;this.quantity=quantity;this.type=type;this.stockLocation=stockLocation;this.isKnownSize=isKnownSize;this.isKnownQuantity=isKnownQuantity;
  }
}
interface StockItemRepository extends JpaRepository<StockItem, Long> {}