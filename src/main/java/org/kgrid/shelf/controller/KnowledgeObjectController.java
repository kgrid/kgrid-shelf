package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class KnowledgeObjectController extends ShelfController {

  public KnowledgeObjectController(
      KnowledgeObjectRepository shelf, Optional<KnowledgeObjectDecorator> kod) {
    super(shelf, kod);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Collection getAllObjects() {
    log.info("getting all kos");
    Map koMap = shelf.findAll();
    log.info("found " + koMap.size() + " kos");
    return koMap.values();
  }

  @GetMapping(path = "/{naan}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> findKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {

    log.info("get ko " + naan + "/" + name);

    // Prevent infinite loop when trying to connect to fcrepo on the same address as the library
    if ("fcrepo".equals(naan) && "rest".equals(name)) {
      throw new IllegalArgumentException(
          "Cannot connect to fcrepo at the same address as the shelf. Make sure shelf and fcrepo configuration is correct.");
    }
    ArkId arkId;
    JsonNode results;
    if (version != null && !"".equals(version)) {
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
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {

    log.info("getting ko " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return shelf.findKnowledgeObjectMetadata(arkId);
  }

  @GetMapping(
      path = "/{naan}/{name}/{version}/service",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Object getServiceDescriptionOldVersionJson(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {

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
      @RequestParam(name = "v", required = false) String version)
      throws JsonProcessingException {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return new YAMLMapper().writeValueAsString(shelf.findServiceSpecification(arkId));
  }

  //  @GetMapping(path = "/{naan}/{name}/{version}/service", produces =
  // MediaType.APPLICATION_JSON_VALUE)
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
      @PathVariable String naan, @PathVariable String name, @PathVariable String version)
      throws JsonProcessingException {

    log.info("getting ko service  " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    return new YAMLMapper().writeValueAsString(shelf.findServiceSpecification(arkId));
  }

  @GetMapping(path = "/{naan}/{name}/{version}/**", produces = MediaType.ALL_VALUE)
  public Object getBinary(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletRequest request)
      throws NoSuchFileException {

    log.info("getting ko resource " + naan + "/" + name + "/" + version);

    ArkId arkId = new ArkId(naan, name, version);

    String requestURI = request.getRequestURI();
    String basePath = StringUtils.join(naan, "/", name, "/", version, "/");
    String childPath = StringUtils.substringAfterLast(requestURI, basePath);

    log.info("getting ko resource " + naan + "/" + name + "/" + version + childPath);

    byte[] binary = shelf.getBinary(arkId, childPath);
    if (binary != null) {
      return binary;
    } else {
      throw new NoSuchFileException("Cannot fetch file at " + childPath);
    }
  }

  @PutMapping(
      path = "/{naan}/{name}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editKnowledgeObjectMetadata(
      @PathVariable String naan, @PathVariable String name, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, data);
    return new ResponseEntity<>(shelf.findKnowledgeObjectMetadata(arkId), HttpStatus.OK);
  }

  @PutMapping(path = "/{naan}/{name}/{version}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editVersionMetadata(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name, version);
    shelf.editMetadata(arkId, data);
    return new ResponseEntity<>(shelf.findKnowledgeObjectMetadata(arkId), HttpStatus.OK);
  }

  @DeleteMapping(path = "/{naan}/{name}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan, @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @DeleteMapping(path = "/{naan}/{name}/{version}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name, version);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
