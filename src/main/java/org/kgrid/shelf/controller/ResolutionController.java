package org.kgrid.shelf.controller;

import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}/ark:/")
@CrossOrigin(origins = "${cors.url:}")
public class ResolutionController extends ShelfExceptionHandler {

  public ResolutionController(
      KnowledgeObjectRepository koRepo,
      Optional<KnowledgeObjectDecorator> kod) {
    super(koRepo, kod);
  }

  @GetMapping(path = "{naan}/{name}/**")
//  @ResponseStatus(HttpStatus.FOUND)
  public ResponseEntity<String> resolve(
      @PathVariable String naan, @PathVariable String name,
      HttpServletRequest request) {

    log.info("Resolving list of artifacts for: " + naan + "/" + name + "/");
    log.info("Resolving list of artifacts for: " + request.getRequestURI());


    ResponseEntity<String> response = ResponseEntity
            .status(HttpStatus.FOUND)
            .header("Location", "http://google.com")
            .body("Hi!");

    return response;

  }
}
