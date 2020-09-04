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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class KnowledgeObjectController extends ShelfExceptionHandler {

  public KnowledgeObjectController(
      KnowledgeObjectRepository shelf, Optional<KnowledgeObjectDecorator> kod) {
    super(shelf, kod);
  }

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Collection getAllObjects() {
    log.info("getting all kos");
    Map koMap = koRepo.findAll();
    log.info("found " + koMap.size() + " kos");
    return koMap.values();
  }

  @GetMapping(path = "/{naan}/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> findKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {
    log.info("get ko " + naan + "/" + name);
    return new ResponseEntity<>(
        koRepo.findKnowledgeObjectMetadata(new ArkId(naan, name, version)), HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/{version}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> getKnowledgeObjectOldVersion(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    return findKnowledgeObject(naan, name, version);
  }

  @GetMapping(
      path = "/{naan}/{name}/{version}/service",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> getServiceDescriptionOldVersionJson(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    return getServiceDescriptionJson(naan, name, version);
  }

  @GetMapping(path = "/{naan}/{name}/service", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> getServiceDescriptionJson(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {
    log.info("getting ko service  " + naan + "/" + name + "/" + version);
    return new ResponseEntity<>(
        koRepo.findServiceSpecification(new ArkId(naan, name, version)), HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/deployment", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> getDeploymentDescriptionJson(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version) {
    log.info("getting ko deployment  " + naan + "/" + name + "/" + version);
    return new ResponseEntity<>(
        koRepo.findDeploymentSpecification(new ArkId(naan, name, version)), HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/service", produces = MediaType.ALL_VALUE)
  public ResponseEntity<String> getServiceDescriptionYaml(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version)
      throws JsonProcessingException {
    log.info("getting ko service  " + naan + "/" + name + "/" + version);
    return new ResponseEntity<>(
        new YAMLMapper()
            .writeValueAsString(koRepo.findServiceSpecification(new ArkId(naan, name, version))),
        HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/{version}/service", produces = MediaType.ALL_VALUE)
  public ResponseEntity<String> getOldServiceDescriptionYaml(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version)
      throws JsonProcessingException {
    log.info("getting ko service  " + naan + "/" + name + "/" + version);
    return getServiceDescriptionYaml(naan, name, version);
  }

  @GetMapping(path = "/{naan}/{name}/{version}/**", produces = MediaType.ALL_VALUE)
  public ResponseEntity<Object> getBinary(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletRequest request) {
    String childPath = getChildPath(naan, name, version, request.getRequestURI());
    log.info("getting ko resource " + naan + "/" + name + "/" + version + childPath);
    return new ResponseEntity<>(
        koRepo.getBinary(new ArkId(naan, name, version), childPath), HttpStatus.OK);
  }

  @PutMapping(
      path = "/{naan}/{name}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editKnowledgeObjectMetadata(
      @PathVariable String naan, @PathVariable String name, @RequestBody String data) {
    return new ResponseEntity<>(koRepo.editMetadata(new ArkId(naan, name), data), HttpStatus.OK);
  }

  @PutMapping(path = "/{naan}/{name}/{version}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editVersionMetadata(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      @RequestBody String data) {
    return new ResponseEntity<>(
        koRepo.editMetadata(new ArkId(naan, name, version), data), HttpStatus.OK);
  }

  @DeleteMapping(path = "/{naan}/{name}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan, @PathVariable String name) {
    koRepo.delete(new ArkId(naan, name));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @DeleteMapping(path = "/{naan}/{name}/{version}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    koRepo.delete(new ArkId(naan, name, version));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  private String getChildPath(String naan, String name, String version, String requestURI) {
    return StringUtils.substringAfterLast(
        requestURI, StringUtils.join(naan, "/", name, "/", version, "/"));
  }
}
