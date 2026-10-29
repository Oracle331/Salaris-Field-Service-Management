package com.salaris.fieldservice;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.net.URI;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Entity
class EquipmentDocument {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long assetId;
  public String title, modelReference, author;
  @Column(length=2000) public String url;
  @Column(length=4000) public String tools;
  public String taskKeyword, pageReference;
  public Instant createdAt=Instant.now();
}
@Entity
class EquipmentToolUse {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long assetId, jobId;
  public String author;
  @Column(length=4000) public String tools;
  public Instant createdAt=Instant.now();
}
interface DocumentRepository extends JpaRepository<EquipmentDocument,Long>{List<EquipmentDocument> findByAssetId(Long id);}
interface ToolUseRepository extends JpaRepository<EquipmentToolUse,Long>{List<EquipmentToolUse> findByAssetIdOrderByCreatedAtDesc(Long id);}

@RestController @RequestMapping("/api")
class EquipmentKnowledge {
  final ClientService records;final JobRepository jobs;final DocumentRepository docs;final ToolUseRepository uses;
  EquipmentKnowledge(ClientService records,JobRepository jobs,DocumentRepository docs,ToolUseRepository uses){this.records=records;this.jobs=jobs;this.docs=docs;this.uses=uses;}
  ClientAsset asset(long id,Authentication a){var asset=records.assets().findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));records.require(asset.clientId,a);return asset;}
  Job job(long id,Authentication a){var j=jobs.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!records.office(a)&&!(records.worker(a)&&Objects.equals(j.worker,a.getName())))throw new ResponseStatusException(HttpStatus.NOT_FOUND);return j;}
  static boolean safeUrl(String value){try{var u=URI.create(value);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
  @GetMapping("/equipment/{id}/knowledge") Map<String,Object> knowledge(@PathVariable long id,Authentication a){asset(id,a);return Map.of("documents",docs.findByAssetId(id),"toolHistory",uses.findByAssetIdOrderByCreatedAtDesc(id));}
  record DocumentInput(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=2000) String url,@NotBlank @Size(max=150) String modelReference,@Size(max=4000) String tools,@Size(max=100) String taskKeyword,@Size(max=150) String pageReference){}
  @PostMapping("/equipment/{id}/documents") EquipmentDocument document(@PathVariable long id,@Valid @RequestBody DocumentInput b,Authentication a){
    var asset=asset(id,a);if(!safeUrl(b.url()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use a full HTTPS documentation link.");
    if(b.tools()!=null&&!b.tools().isBlank()&&(b.pageReference()==null||b.pageReference().isBlank()||b.taskKeyword()==null||b.taskKeyword().isBlank()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"For manufacturer tools, include the page/section and a job keyword such as installation or filter.");
    var d=new EquipmentDocument();d.assetId=asset.id;d.title=b.title();d.url=b.url();d.modelReference=b.modelReference();d.tools=b.tools();d.taskKeyword=b.taskKeyword();d.pageReference=b.pageReference();d.author=a.getName();return docs.save(d);
  }
  record UsedInput(@NotNull Long assetId,@NotBlank @Size(max=4000) String tools){}
  @PostMapping("/jobs/{id}/tools-used") EquipmentToolUse used(@PathVariable long id,@Valid @RequestBody UsedInput b,Authentication a){
    var j=job(id,a);var asset=asset(b.assetId(),a);if(!Objects.equals(asset.clientId,j.clientId)||!Objects.equals(asset.locationId,j.locationId)||j.equipment.stream().noneMatch(e->Objects.equals(e.assetId,asset.id)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose equipment included in this job.");
    var use=new EquipmentToolUse();use.jobId=id;use.assetId=asset.id;use.tools=b.tools().trim();use.author=a.getName();return uses.save(use);
  }
  @GetMapping("/jobs/{id}/tool-plan") List<Map<String,Object>> plan(@PathVariable long id,Authentication a){
    var j=job(id,a);var result=new ArrayList<Map<String,Object>>();
    for(var e:j.equipment){if(e.assetId==null)continue;
      var matching=docs.findByAssetId(e.assetId).stream().filter(d->d.tools!=null&&!d.tools.isBlank()&&Objects.equals(d.modelReference,e.model)&&d.taskKeyword!=null&&!d.taskKeyword.isBlank()&&j.title.toLowerCase(Locale.ROOT).contains(d.taskKeyword.toLowerCase(Locale.ROOT))).toList();
      if(!matching.isEmpty()){for(var d:matching)result.add(Map.of("assetId",e.assetId,"equipment",e.name,"source","MANUFACTURER","tools",d.tools,"reference",d.title+" · "+d.pageReference,"url",d.url));continue;}
      var previous=uses.findByAssetIdOrderByCreatedAtDesc(e.assetId).stream().filter(u->!Objects.equals(u.jobId,j.id)).filter(u->jobs.findById(u.jobId).map(old->"COMPLETED".equals(old.status)&&Objects.equals(old.trade,j.trade)&&Objects.equals(old.title,j.title)).orElse(false)).findFirst();
      if(previous.isPresent()){var u=previous.get();result.add(Map.of("assetId",e.assetId,"equipment",e.name,"source","PREVIOUS_VISIT","tools",u.tools,"reference","Same equipment and job type · job #"+u.jobId,"url",""));}
      else result.add(Map.of("assetId",e.assetId,"equipment",e.name,"source","ESTIMATE","tools",estimate(j.trade),"reference","Suggested preparation for "+j.trade+"; confirm against the model and work scope.","url",""));
    }
    if(result.isEmpty())result.add(Map.of("assetId",0,"equipment","Job preparation","source","ESTIMATE","tools",estimate(j.trade),"reference","No equipment linked yet; confirm tools before departure.","url",""));
    return result;
  }
  static String estimate(String trade){return switch(trade.toLowerCase(Locale.ROOT)){case "hvac"->"Flashlight; nut driver set; screwdrivers; multimeter; inspection mirror";case "plumbing"->"Adjustable wrench; tongue-and-groove pliers; bucket; flashlight; towels";case "appliance"->"Screwdriver set; nut driver set; flashlight; towels; shallow collection tray";default->"Flashlight; screwdriver set; measuring tape; appropriate protective equipment";};}
}
