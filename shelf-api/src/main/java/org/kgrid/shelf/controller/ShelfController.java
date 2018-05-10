package org.kgrid.shelf.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import javax.servlet.http.HttpServletRequest;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

@CrossOrigin(origins = "${cors.url}")
@RestController
@RequestMapping("${shelf.endpoint:}")
public class ShelfController {

  private KnowledgeObjectRepository shelf;

  @Autowired
  public ShelfController(KnowledgeObjectRepository shelf) {
    this.shelf = shelf;
  }

  @GetMapping
  public Map<String, Map<String, ObjectNode>> getAllObjects() {
    return shelf.findAll();
  }

  @GetMapping(path = {"/{naan}/{name}", "/{naan}-{name}"})
  public Map<String, ObjectNode> getKnowledgeObjectVersion(@PathVariable String naan, @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    return shelf.findByArkId(arkId);
  }

  @GetMapping(path = {"/{naan}/{name}/{version}", "/{naan}-{name}/{version}"})
  public KnowledgeObject getKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name);

    return shelf.findByArkIdAndVersion(arkId, version);
  }

  // Order of the path mappings matters here, do not reverse or hyphenated naan-name paths will stop working
  @GetMapping(path = {"/{naan}-{name}/{version}/**", "/{naan}/{name}/{version}/**"})
  public ObjectNode getKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version, HttpServletRequest request) {
    ArkId arkId = new ArkId(naan, name);
    String requestURI = request.getRequestURI();
    String path;
    // if the path has a hyphen and it's before the second slash (the first character is always a slash)
    if(requestURI.indexOf('-') > 0 && requestURI.indexOf('-') < requestURI.substring(1).indexOf('/')) {
      path = new AntPathMatcher().extractPathWithinPattern("/{naan}-{name}/{version}/**", request.getRequestURI());
    } else {
      path = new AntPathMatcher().extractPathWithinPattern("/{naan}/{name}/{version}/**", request.getRequestURI());
    }
    return shelf.getMetadataAtPath(arkId, version, path);
  }

  @GetMapping(path = {"/{naan}/{name}/{version}", "/{naan}-{name}"}, produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version, HttpServletResponse response) {
    ArkId arkId = new ArkId(naan, name);
    response.addHeader("Content-Disposition", "attachment; filename=\"" + naan + "-" + name + "-" + version + ".zip\"");
    try {
      shelf.findByArkIdAndVersion(arkId, version, response.getOutputStream());
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

  @PutMapping(path = {"/{naan}/{name}", "{naan}-{name}"})
  public ResponseEntity<String> addKOZipFolder(@PathVariable String naan, @PathVariable String name, @RequestParam("ko") MultipartFile zippedKo) {
    ArkId pathArk = new ArkId(naan, name);

    ArkId arkId = shelf.save(zippedKo);
    if(!pathArk.equals(arkId)) {
      throw new InputMismatchException("URL must match internal arkId of object");
    }
    String response = arkId + " added to the shelf";

    ResponseEntity<String> result = new ResponseEntity<>(response, HttpStatus.CREATED);

    return result;
  }

  @PutMapping(path = {"/{naan}/{name}/{version}", "/{naan}-{name}/{version}"})
  public ResponseEntity<String> addKOZipFolder(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @RequestParam("ko") MultipartFile zippedKo) {
    ArkId pathArk = new ArkId(naan, name);
    ArkId arkId = shelf.save(zippedKo);
    String response = arkId + " added to the shelf";

    ResponseEntity<String> result = new ResponseEntity<>(response, HttpStatus.CREATED);

    return result;
  }

  @PutMapping(path = {"/{naan}/{name}/{version}", "/{naan}-{name}/{version}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<KnowledgeObject> editMetadata(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, version, null, data);
    return new ResponseEntity<>(shelf.findByArkIdAndVersion(arkId, version), HttpStatus.OK);
  }

  @PutMapping(path = {"/{naan}/{name}/{version}/{path}", "/{naan}-{name}/{version}/{path}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<KnowledgeObject> editMetadata(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @PathVariable String path, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, version, path, data);
    return new ResponseEntity<>(shelf.findByArkIdAndVersion(arkId, version), HttpStatus.OK);
  }

  @DeleteMapping(path = {"/{naan}/{name}", "/{naan}-{name}"})
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan, @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    try {
      shelf.delete(arkId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  @DeleteMapping(path = {"/{naan}/{name}/{version}", "/{naan}-{name}/{version}"})
  public ResponseEntity<String> deleteKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name);
    try {
      shelf.delete(arkId, version);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  //Exception handling:
  @ExceptionHandler(NullPointerException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(NullPointerException e, WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(IllegalArgumentException e, WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(IOException.class)
  public ResponseEntity<Map<String, String>> handleObjectNotFoundExceptions(IOException e, WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage()), HttpStatus.NOT_FOUND);
  }

  private Map<String, String> getErrorMap(WebRequest request, String message) {
    Map<String, String> errorInfo = new HashMap<>();
    errorInfo.put("Error", message);
    errorInfo.put("Request", request.getDescription(false));
    errorInfo.put("Time", new Date().toString());
    return errorInfo;
  }


}
