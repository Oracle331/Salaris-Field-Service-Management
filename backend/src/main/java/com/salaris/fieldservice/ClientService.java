package com.salaris.fieldservice;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
class ClientService {
  final ClientRepository clients;
  final LocationRepository locations;
  final AssetRepository assets;
  final JobRepository jobs;
  ClientService(ClientRepository clients,LocationRepository locations,AssetRepository assets,JobRepository jobs) {
    this.clients=clients;this.locations=locations;this.assets=assets;this.jobs=jobs;
  }
  ClientService(ClientRepository clients, LocationRepository locations, AssetRepository assets, JobRepository jobs) {
    this.clients = clients;
    this.locations = locations;
    this.assets = assets;
    this.jobs = jobs;
  }
  boolean office(Authentication a) {
    return a.getAuthorities().stream().anyMatch(r -> Set.of("ROLE_OFFICE", "ROLE_ADMIN").contains(r.getAuthority()));
  }
  boolean worker(Authentication a) {
    return a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_WORKER"));
  }
  void requireOffice(Authentication a) {
    if (!office(a)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
  boolean visible(ClientRecord c, Authentication a) {
    return office(a) || (worker(a) && jobs.findAll().stream().anyMatch(j -> Objects.equals(j.clientId, c.id) && a.getName().equals(j.worker)));
  }
  ClientRecord require(long id, Authentication a) {
    ClientRecord c = clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!visible(c, a)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return c;
  }
  ServiceLocation location(long id, long clientId) {
    return locations.findById(id).filter(l -> Objects.equals(l.clientId, clientId)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }
  @Transactional
  void link(Job j) {
    if (j.clientId == null) {
      ClientRecord c = clients.findFirstByEmailIgnoreCase(j.clientEmail).orElseGet(() -> {
        ClientRecord n = new ClientRecord();
        n.name = j.clientName;
        n.email = j.clientEmail;
        n.phone = j.phone;
        n.clientAccount = j.clientAccount;
        return clients.save(n);
      });
      j.clientId = c.id;
    }
    if (j.locationId == null) {
      ServiceLocation l = locations.findByClientId(j.clientId).stream().filter(x -> Objects.equals(x.address, j.address)).findFirst().orElseGet(() -> {
        ServiceLocation n = new ServiceLocation();
        n.clientId = j.clientId;
        n.name = "Service location";
        n.address = j.address;
        n.accessNotes = j.accessNotes;
        return locations.save(n);
      });
      j.locationId = l.id;
    }
    if (j.equipment.isEmpty()) {
      for (ClientAsset a : assets.findByClientId(j.clientId)) {
        if (Objects.equals(a.locationId, j.locationId)) {
          Equipment e = new Equipment(a.name, a.brand, a.model, a.serialNumber, a.location, a.filterSize, a.fittingSize);
          e.assetId = a.id;
          j.equipment.add(e);
        }
      }
    }
    for (Equipment e : j.equipment) {
      if (e.assetId == null) {
        ClientAsset asset = assets.findByClientId(j.clientId).stream()
            .filter(x -> Objects.equals(x.locationId, j.locationId) && Objects.equals(x.name, e.name) && Objects.equals(x.model, e.model) && Objects.equals(x.serialNumber, e.serialNumber))
            .findFirst()
            .orElseGet(() -> {
              ClientAsset n = new ClientAsset();
              n.clientId = j.clientId;
              n.locationId = j.locationId;
              n.name = e.name;
              n.brand = e.brand;
              n.model = e.model;
              n.serialNumber = e.serialNumber;
              n.location = e.location;
              n.filterSize = e.filterSize;
              n.fittingSize = e.fittingSize;
              return assets.save(n);
            });
        e.assetId = asset.id;
      }
    }
  }
}
