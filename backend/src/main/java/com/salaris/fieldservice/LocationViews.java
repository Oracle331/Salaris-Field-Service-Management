package com.salaris.fieldservice;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api")
class LocationViews {
  final ClientService records;
  final String key;
  LocationViews(ClientService records,@Value("${app.maps-embed-key:}") String key){this.records=records;this.key=key;}
  ServiceLocation location(long id,Authentication a){var l=records.locations().findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));records.require(l.clientId,a);return l;}
  record Coordinates(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude){}
  @PatchMapping("/locations/{id}/coordinates") ServiceLocation coordinates(@PathVariable long id,@Valid @RequestBody Coordinates b,Authentication a){var l=location(id,a);l.latitude=b.latitude();l.longitude=b.longitude();return records.locations().save(l);}
  @GetMapping("/locations/{id}/street-view") Map<String,Object> view(@PathVariable long id,Authentication a){var l=location(id,a);boolean ready=!key.isBlank()&&l.latitude!=null&&l.longitude!=null;String url=ready?"https://www.google.com/maps/embed/v1/streetview?key="+java.net.URLEncoder.encode(key,java.nio.charset.StandardCharsets.UTF_8)+"&location="+l.latitude+","+l.longitude+"&radius=50&source=outdoor":"";return Map.of("ready",ready,"url",url,"message",key.isBlank()?"Street View is not configured. Site photos and directions are available.":"Set location coordinates to look for nearby Street View imagery.");}
}
