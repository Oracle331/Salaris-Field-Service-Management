package com.salaris.fieldservice;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;

class ScheduleTests {
  ScheduleService service=new ScheduleService("America/New_York");
  Instant now=Instant.parse("2026-10-24T16:00:00Z");
  Job job(long id,String status,String window,LocalDate date) {Job j=new Job();j.id=id;j.status=status;j.priority="NORMAL";j.createdAt=now.minusSeconds(id*3600);service.setWindow(j,date,window);return j;}
  @Test void queueKeepsActiveThenOldestOverdueThenFutureAndCompleted() {
    Job active=job(1,"IN_PROGRESS","09:00 – 10:00",LocalDate.of(2026,10,24));
    Job oldest=job(2,"SCHEDULED","09:00 – 10:00",LocalDate.of(2026,10,22));
    Job recent=job(3,"SCHEDULED","09:00 – 10:00",LocalDate.of(2026,10,23));
    Job future=job(4,"SCHEDULED","14:00 – 15:00",LocalDate.of(2026,10,24));
    Job completed=job(5,"COMPLETED","08:00 – 09:00",LocalDate.of(2026,10,20));
    var queue=new ArrayList<>(List.of(completed,future,recent,oldest,active));queue.sort(service.order(now));
    assertEquals(List.of(active,oldest,recent,future,completed),queue);
  }
  @Test void stagesChangeAtCurrentTimeWithoutChangingBookings() {
    Job j=job(1,"SCHEDULED","12:30 – 13:30",LocalDate.of(2026,10,24));Instant start=j.scheduledStart,end=j.scheduledEnd;
    assertEquals("DUE_SOON",service.stage(j,now));assertEquals("DUE_NOW",service.stage(j,start));assertEquals("OVERDUE",service.stage(j,end));
    assertEquals(start,j.scheduledStart);assertEquals(end,j.scheduledEnd);
  }
  @Test void timeWindowsUseBusinessZoneAndRejectAmbiguousOrInvalidTimes() {
    Job j=new Job();service.setWindow(j,LocalDate.of(2026,10,24),"09:00 – 10:00");assertEquals(Instant.parse("2026-10-24T13:00:00Z"),j.scheduledStart);
    assertThrows(ResponseStatusException.class,()->service.setWindow(j,LocalDate.now(),"tomorrow morning"));
    assertThrows(ResponseStatusException.class,()->service.setWindow(j,LocalDate.now(),"15:00 – 10:00"));
    assertThrows(ResponseStatusException.class,()->service.setWindow(j,LocalDate.of(2026,11,1),"01:15 – 01:45"));
  }
}
