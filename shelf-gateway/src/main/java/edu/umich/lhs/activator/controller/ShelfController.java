package edu.umich.lhs.activator.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umich.lhs.activator.domain.ArkId;
import edu.umich.lhs.activator.domain.KnowledgeObject;
import edu.umich.lhs.activator.repository.KnowledgeObjectRepository;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "${cors.url}")
@RestController
public class ShelfController {

  private KnowledgeObjectRepository shelf;

  @Autowired
  public ShelfController(KnowledgeObjectRepository shelf) {
    this.shelf = shelf;
  }

  @GetMapping("/")
  public Map<String, Map<String, ObjectNode>> getAllObjects() {
    return shelf.findAll();
  }

  @GetMapping("/ark:/{naan}/{name}")
  public Map<String, ObjectNode> getKnowledgeObjectVersion(@PathVariable String naan, @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    return shelf.knowledgeObjectVersions(arkId);
  }

  @GetMapping("/ark:/{naan}/{name}/{version}")
  public KnowledgeObject getKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name);

    return shelf.getCompoundKnowledgeObject(arkId, version);
  }

  @GetMapping(path = "/ark:/{naan}/{name}/{version}", produces = "application/zip")
  public void getZippedKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version, HttpServletResponse response) {
    ArkId arkId = new ArkId(naan, name);
    response.addHeader("Content-Disposition", "attachment; filename=\"" + naan + "-" + name + "-" + version + ".zip\"");
    try {
      shelf.getZippedKnowledgeObject(arkId, version, response.getOutputStream());
    } catch (IOException ex) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  @PutMapping(path = {"/ark:/{naan}/{name}"})
  public ResponseEntity<String> addKOZipFolder(@PathVariable String naan, @PathVariable String name, @RequestParam("ko") MultipartFile zippedKo) {
    ArkId pathArk = new ArkId(naan, name);

    ArkId arkId = shelf.saveKnowledgeObject(zippedKo);
    if(!pathArk.equals(arkId)) {
    }
    String response = arkId + " added to the shelf";

    ResponseEntity<String> result = new ResponseEntity<>(response, HttpStatus.CREATED);

    return result;
  }

  @PutMapping(path = {"/ark:/{naan}/{name}/{version}"})
  public ResponseEntity<String> addKOZipFolder(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @RequestParam("ko") MultipartFile zippedKo) {
    ArkId pathArk = new ArkId(naan, name);
    ArkId arkId = shelf.saveKnowledgeObject(zippedKo);
    String response = arkId + " added to the shelf";

    ResponseEntity<String> result = new ResponseEntity<>(response, HttpStatus.CREATED);

    return result;
  }

  @PutMapping(path = {"/ark:/{naan}/{name}/{version}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<KnowledgeObject> editMetadata(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, version, null, data);
    return new ResponseEntity<>(shelf.getCompoundKnowledgeObject(arkId, version), HttpStatus.OK);
  }

  @PutMapping(path = {"/ark:/{naan}/{name}/{version}/{path}"}, consumes = MediaType.APPLICATION_JSON_VALUE )
  public ResponseEntity<KnowledgeObject> editMetadata(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @PathVariable String path, @RequestBody String data) {
    ArkId arkId = new ArkId(naan, name);
    shelf.editMetadata(arkId, version, path, data);
    return new ResponseEntity<>(shelf.getCompoundKnowledgeObject(arkId, version), HttpStatus.OK);
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
