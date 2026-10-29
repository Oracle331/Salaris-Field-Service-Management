package com.salaris.fieldservice;

import java.util.*;
import java.time.LocalDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RestController @RequestMapping("/api")
class ApiController {
  private final JobRepository repo;
  private final JavaMailSender mail;
  private final ClientService records;
  private final ScheduleService schedule;
  @Value("${app.mail-enabled}") boolean mailEnabled;
  @Value("${app.mail-from}") String mailFrom;
  @Value("${app.maps-api-key}") String mapsKey;
  ApiController(JobRepository repo,JavaMailSender mail,ClientService records,ScheduleService schedule) {this.repo=repo;this.mail=mail;this.records=records;this.schedule=schedule;}
  @GetMapping("/csrf") Map<String,String> csrf(CsrfToken token) {return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
  @GetMapping("/session") Map<String,String> session(Authentication a) {return Map.of("email",a.getName(),"role",a.getAuthorities().iterator().next().getAuthority().substring(5),"timeZone",schedule.zone.toString());}
  boolean role(Authentication a,String role) {return a.getAuthorities().stream().anyMatch(r->r.getAuthority().equals("ROLE_"+role));}
  boolean office(Authentication a) {return role(a,"ADMIN")||role(a,"OFFICE");}
  boolean visible(Job j,Authentication a) {return office(a)||(role(a,"WORKER")&&a.getName().equals(j.worker))||(role(a,"CLIENT")&&a.getName().equals(j.clientAccount));}
  Job requireJob(long id,Authentication a) {
    Job j=repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
    if(!visible(j,a)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return j;
  }
  void requireOffice(Authentication a) {if(!office(a)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
  void requireWork(Authentication a) {if(role(a,"CLIENT")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
  Object view(Job j,Authentication a) {
    if(!role(a,"CLIENT")) return j;
    return Map.of("id",j.id,"title",j.title,"trade",j.trade,"status",j.status,"clientName",j.clientName,"address",j.address,"date",j.date,"timeWindow",j.timeWindow,"summary",Objects.toString(j.summary,""));
  }
  @GetMapping("/jobs") List<Object> jobs(Authentication a) {return repo.findAll().stream().filter(j->visible(j,a)).map(j->view(j,a)).toList();}
  @GetMapping("/jobs/{id}") Object job(@PathVariable long id,Authentication a) {return view(requireJob(id,a),a);}
  record StatusUpdate(@NotNull Long version,@NotNull @Pattern(regexp="SCHEDULED|EN_ROUTE|IN_PROGRESS|COMPLETED") String status) {}
  void version(Job j,Long expected) {if(!Objects.equals(j.version,expected)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Job changed. Refresh and try again.");}
  @PatchMapping("/jobs/{id}/status") @Transactional
  Job status(@PathVariable long id,@Valid @RequestBody StatusUpdate body,Authentication a) {
    requireWork(a);Job j=requireJob(id,a);version(j,body.version());j.status=body.status();j.completedAt="COMPLETED".equals(j.status)?java.time.Instant.now():null;j.history.add(new WorkNote(a.getName(),"Status changed to "+j.status));return repo.saveAndFlush(j);
  }
  record Note(@NotBlank @Size(max=4000) String body,@NotNull Long version) {}
  @PostMapping("/jobs/{id}/notes") @Transactional
  Job note(@PathVariable long id,@Valid @RequestBody Note body,Authentication a) {
    requireWork(a);Job j=requireJob(id,a);version(j,body.version());j.history.add(new WorkNote(a.getName(),body.body().trim()));return repo.saveAndFlush(j);
  }
  record TaskUpdate(boolean done,@NotNull Long version) {}
  @PatchMapping("/jobs/{id}/tasks/{index}") @Transactional
  Job task(@PathVariable long id,@PathVariable int index,@Valid @RequestBody TaskUpdate body,Authentication a) {
    requireWork(a);Job j=requireJob(id,a);version(j,body.version());
    if(index<0||index>=j.tasks.size()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    j.tasks.get(index).done=body.done();return repo.saveAndFlush(j);
  }
  record NewJob(@NotBlank @Size(max=150) String title,@NotBlank @Size(max=60) String trade,
    @NotBlank @Size(max=150) String clientName,@NotBlank @Email @Size(max=254) String clientEmail,
    @NotBlank @Size(max=40) String phone,@NotBlank @Size(max=300) String address,
    @NotNull LocalDate date,@NotBlank @Size(max=80) String timeWindow,@Size(max=4000) String summary,
    @Size(max=4000) String accessNotes,@NotNull @Pattern(regexp="worker@salaris[.]demo") String worker,
    @Email @Size(max=254) String clientAccount,Long clientId,Long locationId) {
    NewJob(String title,String trade,String clientName,String clientEmail,String phone,String address,LocalDate date,String timeWindow,String summary,String accessNotes,String worker,String clientAccount){this(title,trade,clientName,clientEmail,phone,address,date,timeWindow,summary,accessNotes,worker,clientAccount,null,null);}
  }
  @PostMapping("/jobs") @Transactional Job create(@Valid @RequestBody NewJob b,Authentication a) {
    requireOffice(a);Job j=new Job();j.title=b.title();j.trade=b.trade();j.clientName=b.clientName();j.clientEmail=b.clientEmail();j.phone=b.phone();j.address=b.address();j.date=b.date();j.timeWindow=b.timeWindow();j.summary=b.summary();j.accessNotes=b.accessNotes();j.worker=b.worker();j.clientAccount=b.clientAccount()==null?"":b.clientAccount();j.status="SCHEDULED";j.priority="NORMAL";j.createdAt=java.time.Instant.now();
    if(b.clientId()!=null){ClientRecord c=records.require(b.clientId(),a);if(b.locationId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);ServiceLocation l=records.location(b.locationId(),c.id);j.clientId=c.id;j.locationId=l.id;j.clientName=c.name;j.clientEmail=c.email;j.phone=c.phone;j.clientAccount=c.clientAccount==null?"":c.clientAccount;j.address=l.address;j.accessNotes=l.accessNotes;}else if(b.locationId()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    schedule.initialize(j);records.link(j);j.history.add(new WorkNote(a.getName(),"Job created"));return repo.save(j);
  }
  record Assignment(@NotNull Long version,@NotNull @Pattern(regexp="worker@salaris[.]demo|UNASSIGNED") String worker) {}
  @PatchMapping("/jobs/{id}/assignment") @Transactional
  Job assign(@PathVariable long id,@Valid @RequestBody Assignment b,Authentication a) {requireOffice(a);Job j=requireJob(id,a);version(j,b.version());j.worker=b.worker();j.history.add(new WorkNote(a.getName(),"Assigned to "+j.worker));return repo.saveAndFlush(j);}
  record EquipmentUpdate(@NotNull Long version,@NotBlank @Size(max=100) String name,@Size(max=100) String brand,@Size(max=100) String model,@Size(max=100) String serialNumber,@Size(max=200) String location,@Size(max=100) String filterSize,@Size(max=200) String fittingSize) {}
  @PostMapping("/jobs/{id}/equipment") @Transactional
  Job equipment(@PathVariable long id,@Valid @RequestBody EquipmentUpdate b,Authentication a) {requireWork(a);Job j=requireJob(id,a);version(j,b.version());j.equipment.add(new Equipment(b.name(),b.brand(),b.model(),b.serialNumber(),b.location(),b.filterSize(),b.fittingSize()));records.link(j);return repo.saveAndFlush(j);}
  @GetMapping("/schedule") Map<String,Object> schedule(Authentication a) {
    java.time.Instant now=java.time.Instant.now();var jobs=repo.findAll().stream().filter(j->visible(j,a)).sorted(schedule.order(now)).map(j->Map.of("job",view(j,a),"stage",schedule.stage(j,now),"start",j.scheduledStart,"end",j.scheduledEnd,"ageHours",Math.max(0,java.time.Duration.between(j.createdAt,now).toHours()))).toList();
    return Map.of("now",now,"timeZone",schedule.zone.toString(),"entries",jobs);
  }
  record WindowUpdate(@NotNull Long version,@NotNull LocalDate date,@NotBlank String timeWindow) {}
  @PatchMapping("/jobs/{id}/schedule") @Transactional Job reschedule(@PathVariable long id,@Valid @RequestBody WindowUpdate b,Authentication a){
    requireOffice(a);Job j=requireJob(id,a);version(j,b.version());schedule.setWindow(j,b.date(),b.timeWindow());j.history.add(new WorkNote(a.getName(),"Appointment changed to "+j.date+" "+j.timeWindow+" ("+schedule.zone+")"));return repo.saveAndFlush(j);
  }
  @GetMapping("/integrations") Map<String,Object> integrations(Authentication a) {
    if(!role(a,"ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return Map.of("emailEnabled",mailEnabled&&!mailFrom.isBlank(),"mapsApiConfigured",!mapsKey.isBlank(),"directionsEnabled",true);
  }
  @PostMapping("/jobs/{id}/email") Map<String,String> email(@PathVariable long id,Authentication a) {
    requireOffice(a);Job j=requireJob(id,a);
    if(!mailEnabled||mailFrom.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Email is not configured. Use the email draft instead.");
    SimpleMailMessage message=new SimpleMailMessage();message.setFrom(mailFrom);message.setTo(j.clientEmail);message.setSubject("Service appointment: "+j.title);message.setText("Hello "+j.clientName+",\nYour service appointment is "+j.date+" during "+j.timeWindow+" at "+j.address+".\nCurrent status: "+j.status.replace('_',' ')+".\nSalaris Field Service");
    mail.send(message);return Map.of("status","sent");
  }
}
