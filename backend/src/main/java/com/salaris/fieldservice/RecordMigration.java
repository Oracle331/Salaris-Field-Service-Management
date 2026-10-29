package com.salaris.fieldservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Order(10)
class RecordMigration implements CommandLineRunner {
  final JobRepository jobs;final ClientService records;final ScheduleService schedule;
  RecordMigration(JobRepository jobs,ClientService records,ScheduleService schedule){this.jobs=jobs;this.records=records;this.schedule=schedule;}
  @Override @Transactional public void run(String... args) {
    for(Job j:jobs.findAll()) {
      boolean needs=j.clientId==null||j.locationId==null||j.scheduledStart==null||j.createdAt==null||j.equipment.stream().anyMatch(e->e.assetId==null);
      if(needs){records.link(j);schedule.initialize(j);jobs.save(j);}
    }
  }
}
