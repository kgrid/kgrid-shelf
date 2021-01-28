package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ImportService;
import org.kgrid.shelf.service.ManifestReader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class ImportController extends ShelfExceptionHandler {

  private final ImportService importService;
  private final ManifestReader manifestReader;

  public ImportController(
      KnowledgeObjectRepository koRepo,
      ImportService importService,
      ManifestReader manifestReader) {
    super(koRepo);
    this.importService = importService;
    this.manifestReader = manifestReader;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, String>> depositKnowledgeObject(
      @RequestParam("ko") MultipartFile zippedKo) {

    log.info("Add ko via zip");
    URI id = importService.importZip(zippedKo);

    Map<String, String> response = new HashMap<>();
    HttpHeaders headers = addKOHeaderLocation(id);
    response.put("Added", id.toString());

    return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
  }

  @PostMapping(path = "/manifest", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArrayNode> depositManifest(@RequestBody JsonNode manifest) {

    log.info("Add kos from manifest {}", manifest.asText());

    ArrayNode createdKos = manifestReader.loadManifest(manifest);
    return new ResponseEntity<>(
        createdKos, createdKos.size() > 0 ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST);
  }

  @PostMapping(path = "/manifest-list", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArrayNode> depositManifests(@RequestBody JsonNode manifestList) {

    log.info("Adding kos from list of manifests {}", manifestList.asText());

    return new ResponseEntity<>(manifestReader.loadManifests(manifestList), HttpStatus.CREATED);
  }

  private HttpHeaders addKOHeaderLocation(URI id) {
    URI loc = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    HttpHeaders headers = new HttpHeaders();
    if (loc.toString().endsWith("/")) {
      headers.setLocation(loc.resolve(id));
    } else {
      headers.setLocation(URI.create(loc + "/").resolve(id));
    }
    return headers;
  }
}
