package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "${cors.url:}")
@RestController
@RequestMapping("${shelf.endpoint:}")
public class ShelfController {

  private final Logger log = LoggerFactory.getLogger(ShelfController.class);
  private final Optional<KnowledgeObjectDecorator> kod;
  private KnowledgeObjectRepository shelf;

  @Autowired
  public ShelfController(KnowledgeObjectRepository shelf, Optional<KnowledgeObjectDecorator> kod) {
    this.shelf = shelf;
    this.kod = kod;
  }

  @GetMapping
  public Map getAllObjects(@RequestHeader(value = "Prefer", required = false) String prefer,
      RequestEntity request) {
    log.info("getting all kos");
    if (prefer != null && prefer.matches(".*return\\s*=\\s*minimal.*")) {
      return shelf.findAll().keySet().stream()
          .collect(Collectors.toMap(ArkId::toString, key -> request.getUrl() + key.getSlashArk()));
    }
    Map koMap = shelf.findAll();
    log.info("found " + koMap.size() + " kos");
    return koMap;
  }

  @GetMapping(path = "/{naan}/{name}")
  public ResponseEntity<JsonNode> getKnowledgeObject(@PathVariable String naan,
      @PathVariable String name, @RequestHeader(value = "Prefer", required = false) String prefer,
      RequestEntity request) {

    log.info("get ko " + naan + "/" + name);

    // Prevent infinite loop when trying to connect to fcrepo on the same address as the library
    if ("fcrepo".equals(naan) && "rest".equals(name)) {
      throw new IllegalArgumentException(
          "Cannot connect to fcrepo at the same address as the shelf. Make sure shelf and fcrepo configuration is correct.");
    }
    ArkId arkId = new ArkId(naan, name);
    JsonNode results = shelf.findKnowledgeObjectMetadata(arkId);

    if (results == null || results.size() == 0) {
      throw new IllegalArgumentException("Object not found with id " + naan + "-" + name);
    }

    return new ResponseEntity<>(results, HttpStatus.OK);
  }

  @GetMapping(path = "/{naan}/{name}/{implementation}")
  public JsonNode getKnowledgeObjectImplementation(@PathVariable String naan,
      @PathVariable String name,
      @PathVariable String implementation, RequestEntity request,
      HttpServletRequest httpServletRequest) {

    log.info("getting ko " + naan + "/" + name + "/" + implementation + " Look at this " + request
        .getUrl());

    ArkId arkId = new ArkId(naan, name, implementation);

    return shelf.findImplementationMetadata(arkId);
  }

  @GetMapping(path = "/{naan}/{name}/{implementation}", produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name,
      @PathVariable String implementation, HttpServletResponse response) {

    log.info("get ko zip for " + naan + "/" + name);
    ArkId arkId = new ArkId(naan, name, implementation);

    exportZip(response, arkId);
  }

  @GetMapping(path = "/{naan}/{name}", produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name,
      HttpServletResponse response) {

    log.info("get ko zip for " + naan + "/" + name);
    ArkId arkId = new ArkId(naan, name);

    exportZip(response, arkId);
  }

  protected void exportZip(HttpServletResponse response, ArkId arkId) {

    response.addHeader("Content-Disposition",
        "attachment; filename=\"" + arkId.getDashArkImplementation() + "-complete.zip\"");
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

  @GetMapping(path = "/{naan}/{name}/{implementation}/service")
  public Object getServiceDescription(@PathVariable String naan, @PathVariable String name,
      @PathVariable String implementation) throws NoSuchFileException, NoSuchFieldException {

    log.info("getting ko service  " + naan + "/" + name + "/" + implementation);

    ArkId arkId = new ArkId(naan, name, implementation);

    JsonNode metadata = shelf.findImplementationMetadata(arkId);
    String childPath;
    if (metadata.has("service")) {
      childPath = metadata.get("service").asText();
    } else {
      throw new NoSuchFieldException(
          "Object has no service description location specified in metadata.");
    }
    byte[] binary = shelf.getBinaryOrMetadata(arkId, childPath);
    if (binary != null) {
      return binary;
    } else {
      throw new NoSuchFileException("Cannot fetch file at " + childPath);
    }
  }

  @GetMapping(path = "/{naan}/{name}/{implementation}/**", produces = MediaType.ALL_VALUE)
  public Object getBinary(@PathVariable String naan, @PathVariable String name,
      @PathVariable String implementation, HttpServletRequest request) throws NoSuchFileException {

    log.info("getting ko resource " + naan + "/" + name + "/" + implementation);

    ArkId arkId = new ArkId(naan, name, implementation);

    String requestURI = request.getRequestURI();
    String basePath = StringUtils.join(naan, "/", name, "/", implementation, "/");
    String childPath = StringUtils.substringAfterLast(requestURI, basePath);

    log.info("getting ko resource " + naan + "/" + name + "/" + implementation + childPath);

    byte[] binary = shelf.getBinaryOrMetadata(arkId, childPath);
    if (binary != null) {
      return binary;
    } else {
      throw new NoSuchFileException("Cannot fetch file at " + childPath);
    }
  }

  @PutMapping(path = "/{naan}/{name}")
  public ResponseEntity<Map<String, String>> importKO(@PathVariable String naan,
      @PathVariable String name,
      @RequestParam("ko") MultipartFile zippedKo) {

    Map<String, String> result = importKO(naan, name, null, zippedKo).getBody();

    return new ResponseEntity<>(result, HttpStatus.CREATED);
  }

  @PutMapping(path = "/{naan}/{name}/{implementation}")
  public ResponseEntity<Map<String, String>> importKO(@PathVariable String naan,
      @PathVariable String name,
      @PathVariable String implementation, @RequestParam("ko") MultipartFile zippedKo) {

    log.info("add ko " + naan + "/" + name + (implementation == null ? "" : "/" + implementation)
        + " zip file " + zippedKo.getOriginalFilename());

    ArkId pathArk = new ArkId(naan, name);
    ArkId arkId = shelf.importZip(pathArk, zippedKo);

    Map<String, String> response = new HashMap<>();
    response.put("Added", arkId.toString());

    return new ResponseEntity<>(response, HttpStatus.CREATED);

  }

  @PutMapping(path = "/{naan}/{name}/{implementation}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editMetadata(@PathVariable String naan,
      @PathVariable String name, @PathVariable String implementation, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name, implementation);
    shelf.editMetadata(arkId, null, data);
    return new ResponseEntity<>(shelf.findImplementationMetadata(arkId),
        HttpStatus.OK);
  }

  @DeleteMapping(path = "/{naan}/{name}")
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan,
      @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

  }

  @DeleteMapping(path = "/{naan}/{name}/{implementation}")
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan,
      @PathVariable String name, @PathVariable String implementation) {
    ArkId arkId = new ArkId(naan, name, implementation);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

  }

  //Exception handling:
  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(NullPointerException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(
      IllegalArgumentException e, WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(IOException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(NoSuchFileException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(NoSuchFileException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(NoSuchFieldException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(NoSuchFieldException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ShelfException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(ShelfException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.NOT_FOUND),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleGeneralExceptions(Exception e,
      WebRequest request) {
    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.BAD_REQUEST),
        HttpStatus.BAD_REQUEST);
  }

  private Map<String, String> getErrorMap(WebRequest request, String message, HttpStatus status) {
    Map<String, String> errorInfo = new HashMap<>();
    errorInfo.put("Status", status.toString());
    errorInfo.put("Error", message);
    errorInfo.put("Request", request.getDescription(false));
    errorInfo.put("Time", new Date().toString());
    return errorInfo;
  }


}
