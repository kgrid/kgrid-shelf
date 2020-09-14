package org.kgrid.shelf.controller;

import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ResolutionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class ResolutionController extends ShelfExceptionHandler {

  public ResolutionController(
      KnowledgeObjectRepository koRepo,
      Optional<KnowledgeObjectDecorator> kod,
      ResolutionService resolutionService) {
    super(koRepo, kod);
    this.resolutionService = resolutionService;
  }

  final ResolutionService resolutionService;

  @GetMapping(
      path = "/{naan}/{name}/{version}/artifacts",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<URI>> resolveArtifactsForArk(
      @PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    log.info("Resolving list of artifacts for: " + naan + "/" + name + "/" + version);
    return new ResponseEntity<>(
        resolutionService.resolveArtifactsForArk(new ArkId(naan, name, version)), HttpStatus.OK);
  }
}
