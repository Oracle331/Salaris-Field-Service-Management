package com.salaris.fieldservice;

import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
class ClientController {
  final ClientService records;final ClientNoteRepository notes;final PhotoRepository photos;
  ClientController(ClientService records,ClientNoteRepository notes,PhotoRepository photos){this.records=records;this.notes=notes;this.photos=photos;}
  @GetMapping("/clients") List<ClientRecord> clients(Authentication a) {
    if(!records.office(a)&&!records.worker(a))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return records.clients().findAll().stream().filter(c->records.visible(c,a)).toList();
  }
  @GetMapping("/clients/{id}") Map<String,Object> client(@PathVariable long id,Authentication a) {
    ClientRecord c=records.require(id,a);
    return Map.of("client",c,"locations",records.locations().findByClientId(id),"equipment",records.assets().findByClientId(id),"notes",notes.findByClientIdOrderByCreatedAtDesc(id),"photos",photos.findByClientIdOrderByCreatedAtDesc(id));
  }
  record ClientInput(@NotBlank @Size(max=150) String name,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=40) String phone,@Email @Size(max=254) String clientAccount,@Size(max=4000) String notes,Long version,@Pattern(regexp="RESIDENTIAL|COMMERCIAL") String clientType) {}
  void fields(ClientRecord c,ClientInput b){c.name=b.name().trim();c.email=b.email().trim().toLowerCase(Locale.ROOT);c.phone=b.phone();c.clientAccount=b.clientAccount();c.notes=b.notes();if(b.clientType()!=null)c.clientType=b.clientType();}
  void duplicate(String email,Long id){if(records.clients().findFirstByEmailIgnoreCase(email.trim()).filter(c->!Objects.equals(c.id,id)).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Client email already exists.");}
  @PostMapping("/clients") @Transactional ClientRecord create(@Valid @RequestBody ClientInput b,Authentication a) {
    records.requireOffice(a);duplicate(b.email(),null);ClientRecord c=new ClientRecord();fields(c,b);return records.clients().save(c);
  }
  @PatchMapping("/clients/{id}") @Transactional ClientRecord update(@PathVariable long id,@Valid @RequestBody ClientInput b,Authentication a){
    records.requireOffice(a);ClientRecord c=records.require(id,a);if(!Objects.equals(c.version,b.version()))throw new ResponseStatusException(HttpStatus.CONFLICT);duplicate(b.email(),id);fields(c,b);return records.clients().saveAndFlush(c);
  }
  record LocationInput(@NotBlank @Size(max=150) String name,@NotBlank @Size(max=500) String address,@Size(max=4000) String accessNotes) {}
  @PostMapping("/clients/{id}/locations") ServiceLocation location(@PathVariable long id,@Valid @RequestBody LocationInput b,Authentication a){
    records.require(id,a);ServiceLocation l=new ServiceLocation();l.clientId=id;l.name=b.name();l.address=b.address();l.accessNotes=b.accessNotes();return records.locations().save(l);
  }
  record AssetInput(@NotNull Long locationId,@NotBlank @Size(max=100) String name,@Size(max=100) String brand,@Size(max=100) String model,@Size(max=100) String serialNumber,@Size(max=200) String location,@Size(max=100) String filterSize,@Size(max=200) String fittingSize) {}
  @PostMapping("/clients/{id}/equipment") ClientAsset asset(@PathVariable long id,@Valid @RequestBody AssetInput b,Authentication a){
    records.require(id,a);records.location(b.locationId(),id);ClientAsset n=new ClientAsset();n.clientId=id;n.locationId=b.locationId();n.name=b.name();n.brand=b.brand();n.model=b.model();n.serialNumber=b.serialNumber();n.location=b.location();n.filterSize=b.filterSize();n.fittingSize=b.fittingSize();return records.assets().save(n);
  }
  record NoteInput(@NotBlank @Size(max=4000) String body) {}
  @PostMapping("/clients/{id}/notes") ClientNote note(@PathVariable long id,@Valid @RequestBody NoteInput b,Authentication a){
    records.require(id,a);ClientNote n=new ClientNote();n.clientId=id;n.author=a.getName();n.body=b.body().trim();return notes.save(n);
  }
  @PostMapping(value="/clients/{id}/photos",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  RecordPhoto photo(@PathVariable long id,@RequestParam MultipartFile file,@RequestParam(required=false) Long locationId,@RequestParam(required=false) Long assetId,@RequestParam(defaultValue="") String caption,Authentication a) throws IOException {
    records.require(id,a);
    if((locationId==null)==(assetId==null))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select one equipment item or one location.");
    if(caption.length()>200)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Caption must be 200 characters or fewer.");
    if(locationId!=null)records.location(locationId,id);
    if(assetId!=null)records.assets().findById(assetId).filter(x->Objects.equals(x.clientId,id)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(file.isEmpty()||file.getSize()>5*1024*1024)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a JPG or PNG up to 5 MB.");
    if(photos.findByClientIdOrderByCreatedAtDesc(id).stream().filter(p->Objects.equals(p.locationId,locationId)&&Objects.equals(p.assetId,assetId)).count()>=50)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Limit of 50 photos per equipment item or location.");
    byte[] encoded;String contentType;
    try(var input=new javax.imageio.stream.MemoryCacheImageInputStream(file.getInputStream())) {
      var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException();
      ImageReader reader=readers.next();try {
        reader.setInput(input);String format=reader.getFormatName().toLowerCase(Locale.ROOT);
        if(!Set.of("jpeg","jpg","png").contains(format))throw new IOException();
        int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||(long)w*h>20_000_000)throw new IOException();
        BufferedImage image=reader.read(0);ByteArrayOutputStream out=new ByteArrayOutputStream();
        String output=format.equals("png")?"png":"jpg";
        try(var imageOut=new javax.imageio.stream.MemoryCacheImageOutputStream(out)){if(!ImageIO.write(image,output,imageOut))throw new IOException();}
        encoded=out.toByteArray();contentType=output.equals("png")?"image/png":"image/jpeg";
        if(encoded.length>10*1024*1024)throw new IOException();
      }finally{reader.dispose();}
    }catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a valid JPG or PNG up to 5 MB and 20 megapixels.");}
    RecordPhoto p=new RecordPhoto();p.clientId=id;p.locationId=locationId;p.assetId=assetId;p.caption=caption.trim();p.author=a.getName();p.image=encoded;p.contentType=contentType;return photos.save(p);
  }
  @GetMapping("/photos/{id}") ResponseEntity<byte[]> image(@PathVariable long id,Authentication a){
    RecordPhoto p=photos.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));records.require(p.clientId,a);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.contentType)).header("Content-Disposition","inline; filename=photo-"+id+(p.contentType.equals("image/png")?".png":".jpg")).cacheControl(CacheControl.noStore()).body(p.image);
  }
}
