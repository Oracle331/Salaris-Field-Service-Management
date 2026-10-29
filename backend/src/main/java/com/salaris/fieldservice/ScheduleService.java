package com.salaris.fieldservice;

import java.time.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
class ScheduleService {
  final ZoneId zone;
  ScheduleService(@Value("${app.time-zone:America/New_York}") String zone) {this.zone=ZoneId.of(zone);}
  void initialize(Job j) {
    if(j.scheduledStart==null||j.scheduledEnd==null)setWindow(j,j.date,j.timeWindow);
    if(j.createdAt==null){j.createdAt=j.scheduledStart.isBefore(Instant.now())?j.scheduledStart:Instant.now();j.createdAtEstimated=true;}
  }
  void setWindow(Job j,LocalDate date,String window) {
    Matcher m=Pattern.compile("^\\s*(\\d{1,2}:\\d{2})\\s*[-–—]\\s*(\\d{1,2}:\\d{2})\\s*$").matcher(window);
    try {
      if(!m.matches())throw new IllegalArgumentException();
      LocalTime start=LocalTime.parse(m.group(1).length()==4?"0"+m.group(1):m.group(1));
      LocalTime end=LocalTime.parse(m.group(2).length()==4?"0"+m.group(2):m.group(2));
      if(!end.isAfter(start))throw new IllegalArgumentException();
      if(zone.getRules().getValidOffsets(date.atTime(start)).size()!=1||zone.getRules().getValidOffsets(date.atTime(end)).size()!=1)throw new IllegalArgumentException();
      j.date=date;j.timeWindow=start+" – "+end;j.scheduledStart=date.atTime(start).atZone(zone).toInstant();j.scheduledEnd=date.atTime(end).atZone(zone).toInstant();
    } catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use an unambiguous same-day time window: HH:mm – HH:mm in "+zone);}
  }
  String stage(Job j,Instant now) {
    if("COMPLETED".equals(j.status))return "COMPLETED";
    if(Set.of("IN_PROGRESS","EN_ROUTE").contains(j.status))return "ACTIVE";
    if(!j.scheduledEnd.isAfter(now))return "OVERDUE";
    if(!j.scheduledStart.isAfter(now))return "DUE_NOW";
    if(!j.scheduledStart.isAfter(now.plusSeconds(3600)))return "DUE_SOON";
    return "UPCOMING";
  }
  Comparator<Job> order(Instant now) {
    return Comparator.comparingInt((Job j)->switch(stage(j,now)){case "ACTIVE"->0;case "OVERDUE"->1;case "DUE_NOW"->2;case "DUE_SOON"->3;case "UPCOMING"->4;default->5;})
      .thenComparing(j->"COMPLETED".equals(j.status)?Instant.MAX:j.scheduledEnd)
      .thenComparing(j->"URGENT".equals(j.priority)?0:1)
      .thenComparing(j->j.createdAt).thenComparing(j->j.id);
  }
}
