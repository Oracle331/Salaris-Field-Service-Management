package com.salaris.fieldservice;

import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Entity class DemoRevision {@Id public String id;}
interface DemoRevisionRepository extends JpaRepository<DemoRevision,String>{}

@Component @Order(20) @ConditionalOnProperty(name="app.demo-enabled",havingValue="true")
class CustomerDemoData implements CommandLineRunner {
  final ClientService records;final JobRepository jobs;final ScheduleService schedule;final DocumentRepository docs;final ToolUseRepository uses;final DemoRevisionRepository revisions;
  CustomerDemoData(ClientService records,JobRepository jobs,ScheduleService schedule,DocumentRepository docs,ToolUseRepository uses,DemoRevisionRepository revisions){this.records=records;this.jobs=jobs;this.schedule=schedule;this.docs=docs;this.uses=uses;this.revisions=revisions;}
  @Override @Transactional public void run(String... args){
    if(revisions.existsById("customer-workspace-v1"))return;
    // This upgrade adds only fictional sample records and references. Never rewrites existing jobs.
    if(records.clients().findFirstByEmailIgnoreCase("facilities@hudson-demo.example").isEmpty()){
      var c=new ClientRecord();c.name="Hudson Workspace Group";c.email="facilities@hudson-demo.example";c.phone="(555) 010-0220";c.clientType="COMMERCIAL";c.notes="Fictional commercial demo. Two office locations; coordinate access with the facilities desk.";records.clients().save(c);
      var downtown=new ServiceLocation();downtown.clientId=c.id;downtown.name="Downtown office";downtown.address="100 State Street, Albany, NY 12207";downtown.accessNotes="Demo: check in at reception. Breakroom on second floor. Shutoff in cabinet below sink.";records.locations().save(downtown);
      var west=new ServiceLocation();west.clientId=c.id;west.name="West office";west.address="1450 Western Avenue, Albany, NY 12203";west.accessNotes="Demo: use the rear service entrance. Facilities contact meets contractors at 9 AM.";records.locations().save(west);
      var faucet=new ClientAsset();faucet.clientId=c.id;faucet.locationId=downtown.id;faucet.name="Breakroom faucet";faucet.brand="Delta";faucet.model="Leland 9178-AR-DST";faucet.serialNumber="DEMO-COM-001";faucet.location="Second-floor breakroom";faucet.fittingSize="3/8 in compression supply";records.assets().save(faucet);
      var dishwasher=new ClientAsset();dishwasher.clientId=c.id;dishwasher.locationId=west.id;dishwasher.name="Office dishwasher";dishwasher.brand="Bosch";dishwasher.model="SHPM65Z55N";dishwasher.serialNumber="DEMO-COM-002";dishwasher.location="Staff kitchenette";dishwasher.filterSize="Model-specific removable filter";dishwasher.fittingSize="Verify drain connection on site";records.assets().save(dishwasher);
      visit(c,downtown,"Faucet installation","Plumbing",false,0,"Demo: install replacement faucet and verify all connections.");
      var old=visit(c,west,"Dishwasher not draining","Appliance",true,75,"Demo history: cleared debris from the filter and checked the drain hose. Drain cycle passed.");
      used(dishwasher,old,"Torx driver set; shallow tray; towels; flashlight");
      visit(c,west,"Dishwasher not draining","Appliance",false,0,"Demo: recurrent slow draining. Review prior service and inspect for a new obstruction.");
      visit(c,downtown,"Breakroom leak inspection","Plumbing",true,120,"Demo history: tightened supply connection, dried cabinet, and verified no leak during operation.");
    }
    var sam=records.clients().findFirstByEmailIgnoreCase("sam@example.com");
    if(sam.isPresent()){
      var loc=records.locations().findByClientId(sam.get().id).get(0);var old=visit(sam.get(),loc,"Dishwasher not draining","Appliance",true,90,"Demo history: removed food debris from filter, inspected hose, and verified successful drain cycle.");
      records.assets().findByClientId(sam.get().id).stream().filter(a->"SHPM65Z55N".equals(a.model)).findFirst().ifPresent(a->used(a,old,"Torx driver set; shallow tray; towels; flashlight"));
    }
    for(var c:records.clients().findAll()){
      if(!Set.of("alex@example.com","jamie@example.com","sam@example.com","facilities@hudson-demo.example").contains(c.email))continue;
      for(var a:records.assets().findByClientId(c.id)){
        if(a.model==null)continue;
        if(a.model.contains("59TP6")){
          document(a,"Carrier 59TP6 product literature (family reference)","https://www.carrier.com/residential/en/ca/products/furnaces/59tp6/",null,null,null);
        }else if(a.model.contains("9178-AR-DST")){
          document(a,"Delta maintenance & installation · 106557 Rev H","https://media.deltafaucet.com/MandI/106557%20Rev%20H.pdf","Phillips screwdriver; supplied mounting wrench","installation","Page 4 · optional escutcheon / mounting hardware; confirm supplied hardware variant");
        }else if(a.model.equals("SHPM65Z55N")){
          document(a,"Bosch SHPM65Z55N specifications","https://media3.bosch-home.com/Documents/MCDOC03180017_SHPM65Z55N.pdf",null,null,null);
          document(a,"Bosch manuals & support · /24 revision (verify suffix)","https://www.bosch-home.com/us/en/productservice/SHPM65Z55N-24",null,null,null);
        }else if(a.model.startsWith("24SCA5"))document(a,"Carrier product literature lookup · verify complete model","https://www.carrier.com/residential/en/us/products/product-literature/",null,null,null);
      }
    }
    var revision=new DemoRevision();revision.id="customer-workspace-v1";revisions.save(revision);
  }
  Job visit(ClientRecord c,ServiceLocation l,String title,String trade,boolean complete,int days,String summary){
    var j=new Job();j.clientId=c.id;j.locationId=l.id;j.clientName=c.name;j.clientEmail=c.email;j.phone=c.phone;j.clientAccount=c.clientAccount;j.address=l.address;j.accessNotes=l.accessNotes;j.title=title;j.trade=trade;j.worker="worker@salaris.demo";j.status=complete?"COMPLETED":"SCHEDULED";j.priority="NORMAL";j.date=LocalDate.now(schedule.zone).minusDays(days);j.timeWindow="09:00 – 10:30";j.summary=summary;j.createdAt=Instant.now().minus(Duration.ofDays(days+7));schedule.initialize(j);if(complete)j.completedAt=j.scheduledEnd;records.link(j);j.history.add(new WorkNote("Demo service history",summary));j.history.get(0).createdAt=complete?j.completedAt:j.createdAt;return jobs.save(j);
  }
  void used(ClientAsset a,Job j,String tools){var u=new EquipmentToolUse();u.assetId=a.id;u.jobId=j.id;u.tools=tools;u.author="Demo technician";u.createdAt=j.completedAt;uses.save(u);}
  void document(ClientAsset a,String title,String url,String tools,String task,String page){var d=new EquipmentDocument();d.assetId=a.id;d.modelReference=a.model;d.title=title;d.url=url;d.tools=tools;d.taskKeyword=task;d.pageReference=page;d.author="Demo source review";docs.save(d);}
}
