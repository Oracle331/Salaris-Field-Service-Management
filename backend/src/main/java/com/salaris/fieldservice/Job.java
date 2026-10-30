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
  @Column(nullable=false) public LocalDate date;
  @Transient // Temporary preparation scaffold; stock persistence is a separate pending task.
  public List<StockItem> stockItems = new ArrayList<>();
  public String filterSize;
  public Integer filterQuantity;
  public String timeWindow;
  @Column(length=4000) public String summary, accessNotes;
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<Equipment> equipment = new ArrayList<>();
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<WorkNote> history = new ArrayList<>();
  @ElementCollection(fetch=FetchType.EAGER) @OrderColumn public List<TaskItem> tasks = new ArrayList<>();
  public Job() {}

  public String getFilterSize() {
    return filterSize;
  }

  public void setFilterSize(String filterSize) {
    this.filterSize = filterSize;
  }

  public Integer getFilterQuantity() {
    return filterQuantity;
  }

  public void setFilterQuantity(Integer filterQuantity) {
    this.filterQuantity = filterQuantity;
  }

  public String getTimeWindow() {
    return timeWindow;
  }

  public void setTimeWindow(String timeWindow) {
    this.timeWindow = timeWindow;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getAccessNotes() {
    return accessNotes;
  }

  public void setAccessNotes(String accessNotes) {
    this.accessNotes = accessNotes;
  }

  public List<Equipment> getEquipment() {
    return equipment;
  }

  public void setEquipment(List<Equipment> equipment) {
    this.equipment = equipment;
  }

  public List<WorkNote> getHistory() {
    return history;
  }

  public void setHistory(List<WorkNote> history) {
    this.history = history;
  }

  public List<TaskItem> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskItem> tasks) {
    this.tasks = tasks;
  }
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