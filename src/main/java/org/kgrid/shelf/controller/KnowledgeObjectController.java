package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

  @PutMapping(path = "/{naan}/{name}/{version}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editVersionMetadata(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      @RequestBody String data) {
    return new ResponseEntity<>(
        koRepo.editMetadata(new ArkId(naan, name, version), data), HttpStatus.OK);
  }

  @DeleteMapping(path = "/{naan}/{name}/{version}")
  public ResponseEntity<String> deleteKnowledgeObject(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    koRepo.delete(new ArkId(naan, name, version));
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
