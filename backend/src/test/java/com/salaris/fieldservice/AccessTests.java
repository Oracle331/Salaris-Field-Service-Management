package com.salaris.fieldservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
@SpringBootTest
@AutoConfigureMockMvc
public class AccessTests {
    // ... existing code ...

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import static org.hamcrest.Matchers.*;
@SpringBootTest(properties={"app.demo-password=local-test-password-long-enough","spring.datasource.url=jdbc:h2:mem:access-tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=true"})
@AutoConfigureMockMvc
class AccessTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JobRepository repo;
  @Test void anonymousCannotReadJobs() throws Exception {mvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());}
  @Test void workerSeesOnlyAssignedJobs() throws Exception {
    mvc.perform(get("/api/jobs").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()",is((int)repo.findAll().stream().filter(j -> "worker@salaris.demo".equals(j.worker)).count())));
    mvc.perform(get("/api/jobs").with(user("different-worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$.length()",is(0)));
    mvc.perform(get("/api/jobs/1").with(user("different-worker@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
  }
  @Test void clientDataIsScopedAndRedacted() throws Exception {
    mvc.perform(get("/api/jobs").with(user("client@salaris.demo").roles("CLIENT"))).andExpect(jsonPath("$.length()",is(1))).andExpect(jsonPath("$[0].accessNotes").doesNotExist()).andExpect(jsonPath("$[0].history").doesNotExist()).andExpect(jsonPath("$[0].clientEmail").doesNotExist());
    mvc.perform(get("/api/jobs/2").with(user("client@salaris.demo").roles("CLIENT"))).andExpect(status().isNotFound());
    mvc.perform(post("/api/jobs/1/notes").with(user("client@salaris.demo").roles("CLIENT")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"no\",\"version\":0}")).andExpect(status().isForbidden());
  }
  @Test void csrfRequiredForChanges() throws Exception {
    mvc.perform(patch("/api/jobs/1/status").with(user("worker@salaris.demo").roles("WORKER")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\",\"version\":0}")).andExpect(status().isForbidden());
  }
  @Test void integrationSetupIsAdminOnly() throws Exception {
    mvc.perform(get("/api/integrations").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/integrations").with(user("admin@salaris.demo").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.emailEnabled",is(false)));
    mvc.perform(post("/api/jobs/1/email").with(user("office@salaris.demo").roles("OFFICE")).with(csrf())).andExpect(status().isServiceUnavailable());
  }
  @Test void checklistPersistsAndRejectsStaleWrites() throws Exception {
    Job j=repo.findAll().stream().filter(x->!x.tasks.isEmpty()).findFirst().orElseThrow();long version=j.version;
    mvc.perform(patch("/api/jobs/"+j.id+"/tasks/0").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"done\":true,\"version\":"+version+"}")).andExpect(status().isOk()).andExpect(jsonPath("$.tasks[0].done",is(true)));
    mvc.perform(get("/api/jobs/"+j.id).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$.tasks[0].done",is(true)));
    mvc.perform(patch("/api/jobs/"+j.id+"/tasks/0").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"done\":false,\"version\":"+version+"}")).andExpect(status().isConflict());
  }
  @Test void notesAndEquipmentPersist() throws Exception {
    Job j=repo.findById(3L).orElseThrow();
    mvc.perform(post("/api/jobs/3/notes").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new ApiController.Note("Drain tested successfully",j.version)))).andExpect(status().isOk());
    j=repo.findById(3L).orElseThrow();
    mvc.perform(post("/api/jobs/3/equipment").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new ApiController.EquipmentUpdate(j.version,"Drain pump","Bosch","Test","123","Kitchen","N/A","7/8 in")))).andExpect(status().isOk()).andExpect(jsonPath("$.equipment",hasSize(2)));
    mvc.perform(get("/api/jobs/3").with(user("office@salaris.demo").roles("OFFICE"))).andExpect(jsonPath("$.history[*].body",hasItem("Drain tested successfully")));
  }
  @Test void loginUsesRealSessionAndCsrf() throws Exception {
    var result=mvc.perform(get("/api/csrf")).andExpect(status().isOk()).andReturn();
    String token=mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    var login=mvc.perform(post("/api/login").cookie(result.getResponse().getCookies()).header("X-XSRF-TOKEN",token).param("username","worker@salaris.demo").param("password","local-test-password-long-enough")).andExpect(status().isOk()).andReturn();
    mvc.perform(get("/api/session").session((org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false))).andExpect(status().isOk()).andExpect(jsonPath("$.role",is("WORKER")));
  }
  @Test void dispatchCreatesJobsAndControlsAssignments() throws Exception {
    var body=new ApiController.NewJob("Test visit","HVAC","Test client","test@example.com","5550100000","1 Sample Street",java.time.LocalDate.now(),"09:00 – 10:00","Inspect equipment","Call first","worker@salaris.demo","client@salaris.demo");
    String json=mapper.writeValueAsString(body);
    mvc.perform(post("/api/jobs").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
    var result=mvc.perform(post("/api/jobs").with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk()).andReturn();
    var created=mapper.readTree(result.getResponse().getContentAsString());long id=created.get("id").asLong();
    try {
      mvc.perform(get("/api/jobs/"+id).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isOk());
      mvc.perform(patch("/api/jobs/"+id+"/assignment").with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"version\":"+created.get("version").asLong()+",\"worker\":\"UNASSIGNED\"}")).andExpect(status().isOk());
      mvc.perform(get("/api/jobs/"+id).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
    } finally {repo.deleteById(id);}
  }
}
