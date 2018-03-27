package edu.umich.lhs.activator.controller;


import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umich.lhs.activator.domain.ArkId;
import edu.umich.lhs.activator.domain.KnowledgeObject;
import edu.umich.lhs.activator.repository.KnowledgeObjectRepository;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@RestController
public class ShelfController {

  @Autowired
  KnowledgeObjectRepository shelf;

  @GetMapping("/ark:/{naan}/{name}/{version}")
  public KnowledgeObject getKnowledgeObject(@PathVariable String naan, @PathVariable String name, @PathVariable String version) {
    ArkId arkId = new ArkId(naan, name);

    return shelf.getCompoundKnowledgeObject(arkId, version);
  }

  @GetMapping("/ark:/{naan}/{name}")
  public Map<String, ObjectNode> getKnowledgeObjectVersion(@PathVariable String naan, @PathVariable String name) {
    ArkId arkId = new ArkId(naan, name);
    return shelf.knowledgeObjectVersions(arkId);
  }

  @GetMapping("/")
  public List<ObjectNode> getAllObjects() {
     return shelf.getAllObjects();
  }

  @PutMapping(path = {"/ark:/{naan}/{name}/{version}", "ark:/{naan}/{name}"})
  public ResponseEntity<String> addKOZipFolder(@PathVariable String naan, @PathVariable String name, @PathVariable String version, @RequestParam("ko") MultipartFile zippedKo) {
    ArkId pathArk = new ArkId(naan, name);
    ArkId arkId = shelf.saveKnowledgeObject(zippedKo);
    String response = arkId + " added to the shelf";

    ResponseEntity<String> result = new ResponseEntity<>(response, HttpStatus.CREATED);

    return result;
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

  private Map<String, String> getErrorMap(WebRequest request, String message) {
    Map<String, String> errorInfo = new HashMap<>();
    errorInfo.put("Error", message);
    errorInfo.put("Request", request.getDescription(false));
    errorInfo.put("Time", new Date().toString());
    return errorInfo;
  }


}
