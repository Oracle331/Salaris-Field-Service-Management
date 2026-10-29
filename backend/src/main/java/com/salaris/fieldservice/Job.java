package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity
class Job {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public Long clientId, locationId;
  public Instant createdAt, scheduledStart, scheduledEnd, completedAt;
  @Column(nullable=false,columnDefinition="boolean default false") public boolean createdAtEstimated;
  public String title, trade, status, priority, clientName, clientEmail, phone, address, worker, clientAccount;
  public Instant date;
  public String timeWindow;
  @Column(length=4000) public String summary, accessNotes;
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<Equipment> equipment = new ArrayList<Equipment>();
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<WorkNote> history = new ArrayList<>();
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<TaskItem> tasks = new ArrayList<>();
  public Job() {}
}
@Embeddable
class Equipment {
  public Long assetId;
  public String name, brand, model, serialNumber, location, filterSize, fittingSize;
  public Equipment() {}
  Equipment(String name,String brand,String model,String serial,String location,String filter,String fitting) {
    this.name=name;this.brand=brand;this.model=model;this.serialNumber=serial;this.location=location;this.filterSize=filter;this.fittingSize=fitting;
  }
}
@Embeddable
class WorkNote {
  public String author;
  @Column(length=4000) public String body;
  public Instant createdAt;
  public WorkNote() {}
  WorkNote(String author,String body) {this.author=author;this.body=body;this.createdAt=Instant.now();}
}
@Embeddable
class TaskItem {
  public String label;
  public boolean done;
  public TaskItem() {}
  TaskItem(String label) {this.label=label;}
}
interface JobRepository extends JpaRepository<Job,Long> {}

@Embeddable
class InventoryItem {
  public Long equipmentId;
  public String name, brand, model, serialNumber, location, filterSize, fittingSize, type;
  public Integer quantity;
  public InventoryItem() {}
  InventoryItem(Long equipmentId, String name, String brand, String model, String serialNumber, String location, String filterSize, String fittingSize, String type, Integer quantity) {
    this.equipmentId = equipmentId;
    this.name = name;
    this.brand = brand;
    this.model = model;
    this.serialNumber = serialNumber;
    this.location = location;
    this.filterSize = filterSize;
    this.fittingSize = fittingSize;
    this.type = type;
    this.quantity = quantity;
  }
  public boolean isReusable() {
    return Objects.equals(this.type, "REUSABLE");
  }
  public boolean isConsumable() {
    return Objects.equals(this.type, "CONSUMABLE");
  }
}

@Embeddable
class InventoryView {
  public List<InventoryItem> items;
  public InventoryView() {}
  InventoryView(List<InventoryItem> items) {
    this.items = items;
  }
}

@Embeddable
class JobInventory {
  public List<InventoryItem> truckInventory;
  public List<InventoryItem> officeInventory;
  public JobInventory() {}
  JobInventory(List<InventoryItem> truckInventory, List<InventoryItem> officeInventory) {
    this.truckInventory = truckInventory;
    this.officeInventory = officeInventory;
  }
}

@Embeddable
class JobPrepResult {
  public List<String> missingItems;
  public List<String> shortages;
  public JobPrepResult() {}
  JobPrepResult(List<String> missingItems, List<String> shortages) {
    this.missingItems = missingItems;
    this.shortages = shortages;
  }
}
