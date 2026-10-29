package com.salaris.fieldservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobInventoryService {
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public List<InventoryItem> getTruckInventory(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        List<Equipment> jobEquipment = job.getEquipment();
        List<InventoryItem> inventoryItems = jobEquipment.stream()
                .map(equipment -> new InventoryItem(equipment.getAssetId(), equipment.getName(), equipment.getBrand(), equipment.getModel(), equipment.getSerialNumber(), equipment.getLocation(), equipment.getFilterSize(), equipment.getFittingSize(), equipment.getType(), equipmentRepository.findByAssetId(equipment.getAssetId()).orElseThrow(() -> new RuntimeException("Equipment not found"))))
                .collect(Collectors.toList());
        return inventoryItems;
    }

    @Transactional
    public List<InventoryItem> getOfficeInventory(Long jobId) {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        return allEquipment.stream()
                .filter(equipment -> Objects.equals(equipment.getLocation(), "OFFICE"))
                .map(equipment -> new InventoryItem(equipment.getId(), equipment.getName(), equipment.getBrand(), equipment.getModel(), equipment.getSerialNumber(), equipment.getLocation(), equipment.getFilterSize(), equipment.getFittingSize(), equipment.getType(), equipmentRepository.findByAssetId(equipment.getAssetId()).orElseThrow(() -> new RuntimeException("Equipment not found"))))
                .collect(Collectors.toList());
    }

    @Transactional
    public void populateJobWithEquipment(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        List<Equipment> jobEquipment = job.getEquipment();
        for (Equipment equipment : jobEquipment) {
            if (equipment.getType().equals("CONSUMABLE")) {
                // Handle consumables
            } else {
                // Handle reusable equipment
            }
        }
    }
}
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        List<Equipment> jobEquipment = job.getEquipment();
        List<InventoryItem> inventoryItems = jobEquipment.stream()
                .map(equipment -> new InventoryItem(equipment.getAssetId(), equipment.getName(), equipment.getBrand(), equipment.getModel(), equipment.getSerialNumber(), equipment.getLocation(), equipment.getFilterSize(), equipment.getFittingSize(), equipment.getType(), equipmentRepository.findByAssetId(equipment.getAssetId()).orElseThrow(() -> new RuntimeException("Equipment not found"))))
                .collect(Collectors.toList());
        return inventoryItems;
    }

    @Transactional
    public List<InventoryItem> getOfficeInventory(Long jobId) {
        List<Equipment> allEquipment = equipmentRepository.findAll();
        return allEquipment.stream()
                .filter(equipment -> Objects.equals(equipment.getLocation(), "OFFICE"))
                .map(equipment -> new InventoryItem(equipment.getId(), equipment.getName(), equipment.getBrand(), equipment.getModel(), equipment.getSerialNumber(), equipment.getLocation(), equipment.getFilterSize(), equipment.getFittingSize(), equipment.getType(), equipmentRepository.findByAssetId(equipment.getAssetId()).orElseThrow(() -> new RuntimeException("Equipment not found"))))
                .collect(Collectors.toList());
    }

    @Transactional
    public void populateJobWithEquipment(Long jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        List<Equipment> jobEquipment = job.getEquipment();
        for (Equipment equipment : jobEquipment) {
            if (equipment.getType().equals("CONSUMABLE")) {
                // Handle consumables
            } else {
                // Handle reusable equipment
            }
        }
    }
}
