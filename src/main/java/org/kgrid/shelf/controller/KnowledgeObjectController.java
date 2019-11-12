package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class KnowledgeObjectController extends ShelfController {

  public KnowledgeObjectController(KnowledgeObjectRepository shelf,
      Optional<KnowledgeObjectDecorator> kod) {
    super(shelf, kod);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Map getAllObjects() {
    log.info("getting all kos");
    Map koMap = shelf.findAll();
    log.info("found " + koMap.size() + " kos");
    return koMap;
  }

  @GetMapping(path = "/{naan}/{name}",  produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> findKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {

    log.info("get ko " + naan + "/" + name );

    // Prevent infinite loop when trying to connect to fcrepo on the same address as the library
    if ("fcrepo".equals(naan) && "rest".equals(name)) {
      throw new IllegalArgumentException(
          "Cannot connect to fcrepo at the same address as the shelf. Make sure shelf and fcrepo configuration is correct.");
    }
    ArkId arkId;
    JsonNode results;
    if(version != null && !"".equals(version)) {
      arkId = new ArkId(naan, name, version);
      results = shelf.findKnowledgeObjectMetadata(arkId);
    } else {
     arkId = new ArkId(naan, name);
      results = shelf.findKnowledgeObjectMetadata(arkId);
    }


    if (results == null || results.size() == 0) {
      throw new IllegalArgumentException("Object not found with id " + naan + "-" + name);
    }

    return new ResponseEntity<>(results, HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode getKnowledgeObjectOldVersion(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version) {

    log.info("getting ko " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return shelf.findKnowledgeObjectMetadata(arkId);
  }

  @GetMapping(path = "/{naan}/{name}/{version}", headers = "Accept=application/zip", produces = "application/zip")
  public void exportKnowledgeObjectVersion(@PathVariable String naan, @PathVariable String name,
      @PathVariable String version, HttpServletResponse response) {

    log.info("get ko zip for " + naan + "/" + name);
    ArkId arkId = new ArkId(naan, name, version);

    exportZip(response, arkId);
  }

  @GetMapping(path = "/{naan}/{name}",  headers = "Accept=application/zip", produces = "application/zip")
  public void exportKnowledgeObject( @PathVariable String naan, @PathVariable String name,
      @RequestParam(name = "v", required = false) String version,
      HttpServletResponse response) {

    ArkId arkId;
    if(version != null && !"".equals(version)) {
      log.info("get ko zip for " + naan + "/" + name + "/" + version);
      arkId = new ArkId(naan, name, version);
    } else {
      log.info("get ko zip for " + naan + "/" + name);
      arkId = new ArkId(naan, name);
    }
    exportZip(response, arkId);
  }

  @GetMapping(path = "/{naan}/{name}/{version}/service", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object getServiceDescriptionOldVersionJson(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version) {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return shelf.findServiceSpecification(arkId);
  }

  @GetMapping(path = "/{naan}/{name}/service", produces = MediaType.APPLICATION_JSON_VALUE)
  public Object getServiceDescriptionJson(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return shelf.findServiceSpecification(arkId);
  }

  @GetMapping(path = "/{naan}/{name}/service", produces = MediaType.ALL_VALUE)
  public Object getServiceDescriptionYaml(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) throws JsonProcessingException {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return new YAMLMapper().writeValueAsString(shelf.findServiceSpecification(arkId));
  }

//  @GetMapping(path = "/{naan}/{name}/{version}/service", produces = MediaType.APPLICATION_JSON_VALUE)
//  public Object getOldServiceDescriptionJson(
//      @PathVariable String naan,
//      @PathVariable String name,
//      @PathVariable String version) {
//
//    log.info("getting ko service  " + naan + "/" + name + "/" + version);
//
//    ArkId arkId = new ArkId(naan, name, version);
//
//    return shelf.findServiceSpecification(arkId);
//  }


  @GetMapping(path = "/{naan}/{name}/{version}/service", produces = MediaType.ALL_VALUE)
  public Object getOldServiceDescriptionYaml(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version) throws JsonProcessingException {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return new YAMLMapper().writeValueAsString(shelf.findServiceSpecification(arkId));
  }


  @GetMapping(path = "/{naan}/{name}/{version}/**", produces = MediaType.ALL_VALUE)
  public Object getBinary(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version, HttpServletRequest request) throws NoSuchFileException {

    log.info("getting ko resource " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    String requestURI = request.getRequestURI();
    String basePath = StringUtils.join(naan, "/", name, "/", version, "/");
    String childPath = StringUtils.substringAfterLast(requestURI, basePath);

    log.info("getting ko resource " + naan + "/" + name + "/" + version + childPath);

    byte[] binary = shelf.getBinaryOrMetadata(arkId, childPath);
    if (binary != null) {
      return binary;
    } else {
      throw new NoSuchFileException("Cannot fetch file at " + childPath);
    }
  }


  @PutMapping(path = "/{naan}/{name}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)

  public ResponseEntity<JsonNode> editKnowledgeObjectOMetadata(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, null, data);
    return new ResponseEntity<>(shelf.findKnowledgeObjectMetadata(arkId),
        HttpStatus.OK);
  }

  @PutMapping(path = "/{naan}/{name}/{version}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editVersionMetadata(@PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name, version);
    shelf.editMetadata(arkId, null, data);
    return new ResponseEntity<>(shelf.findKnowledgeObjectMetadata(arkId),
        HttpStatus.OK);
  }

  @DeleteMapping(path = "/{naan}/{name}")

  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

  }

  @DeleteMapping(path = "/{naan}/{name}/{version}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name, version);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> depositKnowledgeObject(
     @RequestParam("ko") MultipartFile zippedKo) {

    log.info("Add ko via zip");
    ArkId arkId = shelf.importZip(zippedKo);

    Map<String, String> response = new HashMap<>();
    HttpHeaders headers = addKOHeaderLocation(arkId);
    response.put("Added", arkId.getDashArk());

    return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
  }

  @PostMapping( path = "/manifest", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> depositKnowledgeObject (
      @RequestBody JsonNode requestBody) {

    log.info("Add kos from manifest {}", requestBody.asText());

    if(!requestBody.has("manifest")) {
      throw new IllegalArgumentException("Provide manifest field with url or array of urls as the value");
    }

    Map<String, Object> response = new HashMap<>();
    try {
      if(requestBody.get("manifest").isArray()) {
        ArrayNode arkList = new ObjectMapper().createArrayNode();
        log.info( "importing {} kos", requestBody.get("manifest").size());
        requestBody.get("manifest").forEach(ko -> {
          String koLocation = ko.asText();
          try {
            URL koURL = new URL(koLocation);
            log.info( "import {}", koLocation);
            arkList.add((shelf.importZip(koURL.openStream())).toString());
          }  catch (Exception ex) {
            log.warn( "Error importing {}, {}",koLocation, ex.getMessage());
          }
        });
        response.put("Added", arkList);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
      } else {
        String koLocation = requestBody.get("ko").asText();
        URL koURL = new URL(koLocation);
        ArkId arkId = shelf.importZip(koURL.openStream());
        response.put("Added", arkId);

        HttpHeaders headers = addKOHeaderLocation(arkId);

        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
      }

    } catch (IOException ex) {
      throw new ShelfException(ex);
    }

  }

  protected void exportZip(HttpServletResponse response, ArkId arkId) {

    response.setHeader("Content-Type","application/octet-stream");
    response.addHeader("Content-Disposition",
        "attachment; filename=\"" + (arkId.hasVersion() ?
            arkId.getDashArk() + "-" + arkId.getVersion() : arkId.getDashArk()) + ".zip\"");
    try {
      shelf.extractZip(arkId, response.getOutputStream());
    } catch (IOException ex) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } finally {
      try {
        response.getOutputStream().close();
      } catch (IOException e) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
    }
  }


  private HttpHeaders addKOHeaderLocation(ArkId arkId) {

    URI loc = ServletUriComponentsBuilder
        .fromCurrentRequestUri()
        .pathSegment(arkId.getSlashArk())
        .build().toUri();
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(loc);
    return headers;
  }

}
