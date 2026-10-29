package com.salaris.fieldservice;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity
class ClientRecord {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Version public Long version;
  public String name, email, phone, clientAccount;
  public String clientType = "RESIDENTIAL";
  public Instant createdAt = Instant.now();
  @Column(length=4000) public String notes;
}
@Entity
class ServiceLocation {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long clientId;
  public String name;
  public Double latitude, longitude;
  @Column(length=500) public String address;
  @Column(length=4000) public String accessNotes;
}
@Entity
class ClientAsset {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long clientId, locationId;
  public String name, brand, model, serialNumber, location, filterSize, fittingSize;
}
@Entity
class ClientNote {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long clientId;
  public String author;
  @Column(length=4000) public String body;
  public Instant createdAt = Instant.now();
}
@Entity
class RecordPhoto {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long clientId, locationId, assetId;
  public String caption, author, contentType;
  @Column(length=2000) public String sourceUrl;
  public Instant createdAt = Instant.now();
  @Lob @JsonIgnore public byte[] image;
}
interface ClientRepository extends JpaRepository<ClientRecord,Long> {Optional<ClientRecord> findFirstByEmailIgnoreCase(String email);}
interface LocationRepository extends JpaRepository<ServiceLocation,Long> {List<ServiceLocation> findByClientId(Long clientId);}
interface AssetRepository extends JpaRepository<ClientAsset,Long> {List<ClientAsset> findByClientId(Long clientId);}
interface ClientNoteRepository extends JpaRepository<ClientNote,Long> {List<ClientNote> findByClientIdOrderByCreatedAtDesc(Long clientId);}
interface PhotoRepository extends JpaRepository<RecordPhoto,Long> {List<RecordPhoto> findByClientIdOrderByCreatedAtDesc(Long clientId);}
