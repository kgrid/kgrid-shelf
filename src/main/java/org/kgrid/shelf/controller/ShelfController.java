package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@CrossOrigin(origins = "${cors.url:}")
@RestController
@RequestMapping("${shelf.endpoint:}")
@Api(tags = "Knowledge Object API" )
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
  @ApiOperation(value = "Finds all knowledge objects",
      notes = "Returns a collection of knowledge objects in the from of a JSON array.  ",
      response = JsonNode.class,
      responseContainer = "List")
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

  @ApiOperation(value = "Find a Knowledge Object based on naan and name",
      notes = "Returns a knowledge object based on naan and name",
      response = JsonNode.class)
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
  @ApiOperation(value = "Find a Knowledge Object Implementation",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam sagittis justo ut quam maximus, sed pharetra libero tempus.",
      response = JsonNode.class)
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
  @PostMapping
  @ApiOperation(value = "Imports a packaged Knowledge Object",
      notes = "Mulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum primis in faucibus. Etiam sagittis justo ut quam maximus, sed pharetra libero tempus.",
      response = JsonNode.class,
      responseContainer = "List")

  public ResponseEntity<Map<String, String>> depositKnowledgeObject(
      @RequestParam("ko") MultipartFile zippedKo) {

    log.info("Add ko via zip");
    ArkId arkId = shelf.importZip(zippedKo);

    Map<String, String> response = new HashMap<>();
    HttpHeaders headers = addKOHeaderLocation(arkId);
    response.put("Added", arkId.getDashArk());

    return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
  }

  @ApiOperation(value = "Import Knowledge Objects based on a list URLs to packaged Knowledge Objects",
      notes = "Multiple status values can be provided with comma seperated strings"
  )
  @PostMapping( consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Object>> depositKnowledgeObject (
      @RequestBody JsonNode requestBody) {

    log.info("Add kos from manifest {}", requestBody.asText());

    if(!requestBody.has("ko")) {
      throw new IllegalArgumentException("Provide ko field with url or array of urls as the value");
    }

    Map<String, Object> response = new HashMap<>();
    try {
      if(requestBody.get("ko").isArray()) {
        ArrayNode arkList = new ObjectMapper().createArrayNode();
        log.info( "importing {} kos", requestBody.get("ko").size());
        requestBody.get("ko").forEach(ko -> {
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
  @ApiOperation(value = "Export Knowledge Object Implementation",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @GetMapping(path = "/{naan}/{name}/{implementation}", produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name,
      @PathVariable String implementation, HttpServletResponse response) {

    log.info("get ko zip for " + naan + "/" + name);
    ArkId arkId = new ArkId(naan, name, implementation);

    exportZip(response, arkId);
  }

  @ApiOperation(value = "Export Knowledge Object",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @GetMapping(path = "/{naan}/{name}", produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name,
      HttpServletResponse response) {

    log.info("get ko zip for " + naan + "/" + name);
    ArkId arkId = new ArkId(naan, name);

    exportZip(response, arkId);
  }

  protected void exportZip(HttpServletResponse response, ArkId arkId) {

    response.addHeader("Content-Disposition",
        "attachment; filename=\"" + (arkId.isImplementation() ?
            arkId.getDashArk() + "-" + arkId.getImplementation() : arkId.getDashArk()) + ".zip\"");
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

  @ApiOperation(value = "Find Knowledge Object Implementation Open API Service specification",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @GetMapping(path = "/{naan}/{name}/{implementation}/service")
  public Object getServiceDescription(@PathVariable String naan, @PathVariable String name,
      @PathVariable String implementation) throws NoSuchFileException, NoSuchFieldException {

    log.info("getting ko service  " + naan + "/" + name + "/" + implementation);

    ArkId arkId = new ArkId(naan, name, implementation);

    return shelf.findServiceSpecification(arkId);

  }

  @ApiOperation(value = "Find Knowledge Object Implementation resource",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
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

  @ApiOperation(value = "Update Knowledge Object ",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @PutMapping(path = "/{naan}/{name}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editKnowledgeObjectOMetadata(@PathVariable String naan,
      @PathVariable String name, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, null, data);
    return new ResponseEntity<>(shelf.findKnowledgeObjectMetadata(arkId),
        HttpStatus.OK);
  }
  @ApiOperation(value = "Update Knowledge Object Implementation",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @PutMapping(path = "/{naan}/{name}/{implementation}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonNode> editImplmentaionMetadata(@PathVariable String naan,
      @PathVariable String name, @PathVariable String implementation, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name, implementation);
    shelf.editMetadata(arkId, null, data);
    return new ResponseEntity<>(shelf.findImplementationMetadata(arkId),
        HttpStatus.OK);
  }
  @ApiOperation(value = "Delete Knowledge Object ",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @DeleteMapping(path = "/{naan}/{name}")
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan,
      @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    shelf.delete(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

  }
  @ApiOperation(value = "Delete Knowledge Object Implementation",
      notes = "ulla nibh velit, porttitor sit amet viverra at, rhoncus vitae sapien. Fusce non eleifend mauris. Interdum et malesuada fames ac ante ipsum")
  @DeleteMapping(path = "/{naan}/{name}/{implementation}")
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan,
      @PathVariable String name, @PathVariable String implementation) {
    ArkId arkId = new ArkId(naan, name, implementation);
    shelf.deleteImpl(arkId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);

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

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.BAD_REQUEST),
        HttpStatus.BAD_REQUEST);
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
  public ResponseEntity<Map<String, String>> handleGeneralShelfExceptions(ShelfException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.BAD_REQUEST),
        HttpStatus.BAD_REQUEST);
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
