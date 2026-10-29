package com.salaris.fieldservice;

import java.time.LocalDate;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
class DemoData {
  @Bean @org.springframework.core.annotation.Order(0) @ConditionalOnProperty(name="app.demo-enabled",havingValue="true")
  CommandLineRunner seed(JobRepository repo) { return args -> {
    if(repo.count()!=0) return;
    Job hvac=job("Seasonal HVAC maintenance","HVAC","IN_PROGRESS","NORMAL","Morgan residence","Alex Morgan","alex@example.com","(555) 010-0142","128 Maple Avenue, Albany, NY 12208","08:30 – 10:00","Annual heating maintenance. Client reports a rattling sound when the system starts.","Use side entrance. Air handler is in the basement. Friendly dog in the fenced yard.");
    hvac.equipment.add(new Equipment("Main furnace","Carrier","59TP6B060V17","DEMO-48291","Basement utility room","16 × 25 × 1 in","3/4 in gas connection"));
    hvac.equipment.add(new Equipment("Outdoor condenser","Carrier","24SCA530W003","DEMO-10284","East side of house","Not applicable","3/8 in liquid · 3/4 in suction"));
    hvac.history.add(new WorkNote("Office / Dispatch","Previous visit: replaced filter and cleaned flame sensor. Client requested a call before arrival."));
    for(String t:List.of("Confirm equipment and filter size","Inspect and clean flame sensor","Investigate startup rattle","Record work and confirm with client")) hvac.tasks.add(new TaskItem(t));
    repo.save(hvac);
    Job plumbing=job("Kitchen sink leak","Plumbing","SCHEDULED","URGENT","Chen residence","Jamie Chen","jamie@example.com","(555) 010-0176","42 State Street, Albany, NY 12207","10:30 – 12:00","Water dripping below the kitchen sink. Inspect supply connections and shutoff valves.","Park in driveway. Shutoff under sink; main shutoff in basement.");
    plumbing.clientAccount="chen@salaris.demo";
    plumbing.equipment.add(new Equipment("Kitchen faucet","Delta","Leland 9178-AR-DST","Not recorded","Kitchen","Not applicable","3/8 in compression supply · 1-1/2 in drain"));
    plumbing.tasks.add(new TaskItem("Locate leak and verify fitting sizes"));plumbing.tasks.add(new TaskItem("Test under pressure after repair"));repo.save(plumbing);
    Job appliance=job("Dishwasher not draining","Appliance","SCHEDULED","NORMAL","Rivera residence","Sam Rivera","sam@example.com","(555) 010-0188","76 Madison Avenue, Albany, NY 12202","13:00 – 14:30","Standing water after cycle. Check filter, drain hose, and pump.","Client will be home. Call on arrival.");
    appliance.clientAccount="rivera@salaris.demo";
    appliance.equipment.add(new Equipment("Dishwasher","Bosch","SHPM65Z55N","DEMO-93015","Kitchen island","Model-specific removable filter","7/8 in drain hose"));
    appliance.tasks.add(new TaskItem("Inspect drain filter and hose"));appliance.tasks.add(new TaskItem("Run and verify drain cycle"));repo.save(appliance);
  }; }
  private Job job(String title,String trade,String status,String priority,String site,String client,String email,String phone,String address,String time,String summary,String access) {
    Job j=new Job();j.title=title;j.trade=trade;j.status=status;j.priority=priority;j.clientName=client;j.clientEmail=email;j.phone=phone;j.address=address;j.worker="worker@salaris.demo";j.clientAccount="client@salaris.demo";j.date=LocalDate.now();j.timeWindow=time;j.summary=summary;j.accessNotes=access;return j;
  }
}
