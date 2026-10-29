package com.salaris.fieldservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs/inventory")
class JobInventoryController {
  @Autowired
  private JobInventoryService jobInventoryService;

  @GetMapping("/truck/{jobId}")
  public ResponseEntity<InventoryView> getTruckInventory(@PathVariable Long jobId) {
    List<InventoryItem> truckInventory = jobInventoryService.getTruckInventory(jobId);
    return ResponseEntity.ok(new InventoryView(truckInventory));
  }

  @GetMapping("/office/{jobId}")
  public ResponseEntity<InventoryView> getOfficeInventory(@PathVariable Long jobId) {
    List<InventoryItem> officeInventory = jobInventoryService.getOfficeInventory(jobId);
    return ResponseEntity.ok(new InventoryView(officeInventory));
  }
}
