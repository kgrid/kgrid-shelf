package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportExportException;
import org.kgrid.shelf.service.ImportService;
import org.kgrid.shelf.service.ManifestReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class ImportExportController extends ShelfExceptionHandler {

  @Autowired ApplicationContext applicationContext;

  @Autowired ObjectMapper mapper;

  ImportService importService;

  ExportService exportService;

  ManifestReader manifestReader;

  KnowledgeObjectRepository shelf;

  public ImportExportController(
      ImportService importService,
      ExportService exportService,
      ManifestReader manifestReader,
      KnowledgeObjectRepository shelf,
      Optional<KnowledgeObjectDecorator> kod) {
    super(shelf, kod);
    this.shelf = shelf;
    this.importService = importService;
    this.exportService = exportService;
    this.manifestReader = manifestReader;
  }

  @GetMapping(path = "/{naan}/{name}/{version}", produces = "application/zip")
  public void exportKnowledgeObjectVersion(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletResponse response) {

    exportKnowledgeObject(naan, name, version, response);
  }

  @GetMapping(path = "/{naan}/{name}", produces = "application/zip")
  public void exportKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version,
      HttpServletResponse response) {

    ArkId arkId;
    if (version != null && !"".equals(version)) {
      log.info("get ko zip for " + naan + "/" + name + "/" + version);
      arkId = new ArkId(naan, name, version);
    } else {
      log.info("get ko zip for " + naan + "/" + name);
      arkId = new ArkId(naan, name);
    }
    response.setHeader("Content-Type", "application/octet-stream");
    response.addHeader(
        "Content-Disposition", "attachment; filename=\"" + arkId.getFullDashArk() + ".zip\"");
    ServletOutputStream outputStream = null;
    try {
      outputStream = response.getOutputStream();
      exportService.zipKnowledgeObject(arkId, outputStream);
      outputStream.close();
    } catch (ImportExportException e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
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

    return new ResponseEntity<>(manifestReader.loadManifest(manifest), HttpStatus.CREATED);
  }

  @PostMapping(path = "/manifest-list", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArrayNode> depositManifests(@RequestBody JsonNode manifestList) {

    log.info("Adding kos from list of manifests {}", manifestList.asText());

    return new ResponseEntity<>(manifestReader.loadManifests(manifestList), HttpStatus.CREATED);
  }

  private HttpHeaders addKOHeaderLocation(URI id) {
    URI loc = ServletUriComponentsBuilder.fromCurrentRequestUri().build().toUri();

    HttpHeaders headers = new HttpHeaders();

    headers.setLocation(loc.resolve(id));
    return headers;
  }
}
