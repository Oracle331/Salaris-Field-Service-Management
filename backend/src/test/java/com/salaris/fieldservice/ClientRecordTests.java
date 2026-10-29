package com.salaris.fieldservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"app.demo-password=local-test-password-long-enough","spring.datasource.url=jdbc:h2:mem:client-tests;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc @Transactional
class ClientRecordTests {
  @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired ClientRepository clients;
  @Autowired JobRepository jobs;@Autowired LocationRepository locations;@Autowired AssetRepository assets;
  @Autowired RecordMigration migration;
  @Autowired CustomerDemoData demo;@Autowired DocumentRepository documents;@Autowired PhotoRepository photos;
  @Test void photoUploadDoesNotRequireWritableImageCache() throws Exception {
    var asset=assets.findByClientId(first().id).get(0);var bytes=png();
    var original=ImageIO.getCacheDirectory();var dir=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("."),"image-cache-test-").toFile();
    ImageIO.setCacheDirectory(dir);assertTrue(dir.delete());
    try{mvc.perform(multipart("/api/clients/"+first().id+"/photos").file(new MockMultipartFile("file","phone.png","image/png",bytes)).param("assetId",asset.id.toString()).with(user("worker@salaris.demo").roles("WORKER")).with(csrf())).andExpect(status().isOk());}finally{ImageIO.setCacheDirectory(original);}
  }
  @Test void rejectedPhotoExplainsTheProblem() throws Exception {
    var asset=assets.findByClientId(first().id).get(0);
    mvc.perform(multipart("/api/clients/"+first().id+"/photos").file(new MockMultipartFile("file","photo.heic","image/heic",new byte[]{1,2,3})).param("assetId",asset.id.toString()).with(user("worker@salaris.demo").roles("WORKER")).with(csrf())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error",containsString("valid JPG or PNG")));
  }
  @Test void demoUpgradeIsIdempotentAndCompanyHasSeparateLocations() throws Exception {
    long j=jobs.count(),p=photos.count(),d=documents.count();demo.run();assertEquals(j,jobs.count());assertEquals(p,photos.count());assertEquals(d,documents.count());
    var company=clients.findFirstByEmailIgnoreCase("facilities@hudson-demo.example").orElseThrow();assertEquals("COMMERCIAL",company.clientType);assertEquals(2,locations.findByClientId(company.id).size());
    assertEquals(2,assets.findByClientId(company.id).size());assertTrue(jobs.findAll().stream().anyMatch(x->x.clientId.equals(company.id)&&x.status.equals("COMPLETED")));
  }
  @Test void toolsUseManufacturerThenMatchingHistoryThenEstimate() throws Exception {
    var company=clients.findFirstByEmailIgnoreCase("facilities@hudson-demo.example").orElseThrow();
    var install=jobs.findAll().stream().filter(j->j.title.equals("Faucet installation")).findFirst().orElseThrow();
    mvc.perform(get("/api/jobs/"+install.id+"/tool-plan").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$[0].source",is("MANUFACTURER"))).andExpect(jsonPath("$[0].url",containsString("deltafaucet.com")));
    mvc.perform(get("/api/jobs/3/tool-plan").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$[0].source",is("PREVIOUS_VISIT")));
    mvc.perform(get("/api/jobs/1/tool-plan").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$[0].source",is("ESTIMATE")));
    mvc.perform(get("/api/jobs/1/tool-plan").with(user("client@salaris.demo").roles("CLIENT"))).andExpect(status().isNotFound());
    var a=assets.findByClientId(company.id).get(0);
    mvc.perform(post("/api/jobs/1/tools-used").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":"+a.id+",\"tools\":\"Wrench\"}")).andExpect(status().isBadRequest());
  }
  @Test void workerCanRecordDocumentationAndToolsWithSourceValidation() throws Exception {
    var a=assets.findByClientId(first().id).get(0);String path="/api/equipment/"+a.id+"/documents";
    mvc.perform(post(path).with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Manual\",\"modelReference\":\"59TP6\",\"url\":\"javascript:alert(1)\"}")).andExpect(status().isBadRequest());
    mvc.perform(post(path).with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Manual\",\"modelReference\":\"59TP6\",\"url\":\"https://www.carrier.com/\"}")).andExpect(status().isOk());
    mvc.perform(post("/api/jobs/1/tools-used").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":"+a.id+",\"tools\":\"Nut driver; flashlight\"}")).andExpect(status().isOk());
    mvc.perform(get("/api/equipment/"+a.id+"/knowledge").with(user("other@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
  }
  @Test void locationViewsValidateCoordinatesAndRemainScoped() throws Exception {
    var l=locations.findByClientId(first().id).get(0);
    mvc.perform(get("/api/locations/"+l.id+"/street-view").with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$.ready",is(false)));
    mvc.perform(patch("/api/locations/"+l.id+"/coordinates").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":91,\"longitude\":0}")).andExpect(status().isBadRequest());
    mvc.perform(patch("/api/locations/"+l.id+"/coordinates").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":42.65,\"longitude\":-73.75}")).andExpect(status().isOk());
    mvc.perform(get("/api/locations/"+l.id+"/street-view").with(user("other@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
  }
  ClientRecord first(){return clients.findFirstByEmailIgnoreCase("alex@example.com").orElseThrow();}
  @Test void clientsExistOutsideJobsAndCanBeUpdated() throws Exception {
    long before=jobs.count();
    var result=mvc.perform(post("/api/clients").with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New prospect\",\"email\":\"new@example.com\",\"phone\":\"5550101111\",\"notes\":\"Interested in maintenance\"}")).andExpect(status().isOk()).andReturn();
    var c=mapper.readTree(result.getResponse().getContentAsString());long id=c.get("id").asLong();assertEquals(before,jobs.count());
    mvc.perform(get("/api/clients/"+id).with(user("office@salaris.demo").roles("OFFICE"))).andExpect(jsonPath("$.client.notes",is("Interested in maintenance"))).andExpect(jsonPath("$.locations",hasSize(0)));
    String update="{\"name\":\"Updated prospect\",\"email\":\"new@example.com\",\"phone\":\"5550101111\",\"version\":"+c.get("version").asLong()+"}";
    mvc.perform(patch("/api/clients/"+id).with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk());
    mvc.perform(patch("/api/clients/"+id).with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
    mvc.perform(get("/api/clients/"+id).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
  }
  @Test void directoryAndNotesRespectRoleAndAssignment() throws Exception {
    var c=first();mvc.perform(get("/api/clients").with(user("client@salaris.demo").roles("CLIENT"))).andExpect(status().isForbidden());
    mvc.perform(get("/api/clients").with(user("other@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$",hasSize(0)));
    mvc.perform(post("/api/clients/"+c.id+"/notes").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Client prefers side entrance\"}")).andExpect(status().isOk());
    mvc.perform(get("/api/clients/"+c.id).with(user("office@salaris.demo").roles("OFFICE"))).andExpect(jsonPath("$.notes[0].body",is("Client prefers side entrance")));
    mvc.perform(post("/api/clients").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"No\",\"email\":\"no@example.com\",\"phone\":\"555\"}")).andExpect(status().isForbidden());
  }
  byte[] png() throws Exception {var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(8,8,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
  @Test void equipmentPhotoPersistsAndIsNotPublic() throws Exception {
    var c=first();var asset=assets.findByClientId(c.id).get(0);byte[] bytes=png();
    var r=mvc.perform(multipart("/api/clients/"+c.id+"/photos").file(new MockMultipartFile("file","photo.png","image/png",bytes)).param("assetId",asset.id.toString()).param("caption","Model plate").with(user("worker@salaris.demo").roles("WORKER")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.image").doesNotExist()).andReturn();
    long photo=mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(get("/api/photos/"+photo).with(user("office@salaris.demo").roles("OFFICE"))).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(bytes));
    mvc.perform(get("/api/photos/"+photo)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/photos/"+photo).with(user("client@salaris.demo").roles("CLIENT"))).andExpect(status().isNotFound());
    mvc.perform(get("/api/photos/"+photo).with(user("other@salaris.demo").roles("WORKER"))).andExpect(status().isNotFound());
    mvc.perform(get("/api/clients/"+c.id).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(jsonPath("$.photos[0].assetId",is(asset.id.intValue()))).andExpect(jsonPath("$.photos[0].image").doesNotExist());
  }
  @Test void locationPhotoWorksAndRejectsInvalidOrCrossClientTargets() throws Exception {
    var c=first();var l=locations.findByClientId(c.id).get(0);var other=clients.findFirstByEmailIgnoreCase("jamie@example.com").orElseThrow();long wrong=locations.findByClientId(other.id).get(0).id;
    var file=new MockMultipartFile("file","photo.png","image/png",png());
    mvc.perform(multipart("/api/clients/"+c.id+"/photos").file(file).param("locationId",l.id.toString()).with(user("worker@salaris.demo").roles("WORKER")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.locationId",is(l.id.intValue())));
    mvc.perform(multipart("/api/clients/"+c.id+"/photos").file(file).param("locationId",Long.toString(wrong)).with(user("office@salaris.demo").roles("OFFICE")).with(csrf())).andExpect(status().isNotFound());
    mvc.perform(multipart("/api/clients/"+c.id+"/photos").file(new MockMultipartFile("file","bad.png","image/png","not an image".getBytes())).param("locationId",l.id.toString()).with(user("office@salaris.demo").roles("OFFICE")).with(csrf())).andExpect(status().isBadRequest());
    mvc.perform(multipart("/api/clients/"+c.id+"/photos").file(file).param("locationId",l.id.toString()).with(user("worker@salaris.demo").roles("WORKER"))).andExpect(status().isForbidden());
  }
  @Test void jobFromExistingClientReusesLocationAndEquipment() throws Exception {
    var c=first();var l=locations.findByClientId(c.id).get(0);
    var b=new ApiController.NewJob("Return visit","HVAC",c.name,c.email,c.phone,l.address,java.time.LocalDate.now(),"09:00 – 10:00","Check system","", "worker@salaris.demo",c.clientAccount,c.id,l.id);
    mvc.perform(post("/api/jobs").with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(b))).andExpect(status().isOk()).andExpect(jsonPath("$.clientId",is(c.id.intValue()))).andExpect(jsonPath("$.locationId",is(l.id.intValue()))).andExpect(jsonPath("$.equipment",hasSize(2))).andExpect(jsonPath("$.scheduledStart").exists());
  }
  @Test void migrationIsIdempotent() {
    long count=clients.count(),assetCount=assets.count();migration.run();assertEquals(count,clients.count());assertEquals(assetCount,assets.count());
    assertTrue(jobs.findAll().stream().allMatch(j->j.clientId!=null&&j.locationId!=null&&j.scheduledStart!=null));
  }
  @Test void onlyDispatchCanChangeBookedWindows() throws Exception {
    Job j=jobs.findAll().get(0);String body="{\"version\":"+j.version+",\"date\":\"2026-10-26\",\"timeWindow\":\"10:00 – 11:00\"}";
    mvc.perform(patch("/api/jobs/"+j.id+"/schedule").with(user("worker@salaris.demo").roles("WORKER")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    mvc.perform(patch("/api/jobs/"+j.id+"/schedule").with(user("office@salaris.demo").roles("OFFICE")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.scheduledStart",is("2026-10-26T14:00:00Z")));
    mvc.perform(get("/api/schedule").with(user("office@salaris.demo").roles("OFFICE"))).andExpect(status().isOk()).andExpect(jsonPath("$.timeZone",is("America/New_York"))).andExpect(jsonPath("$.now").exists());
  }
}
