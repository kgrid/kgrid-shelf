package org.kgrid.shelf.controller;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;


@CrossOrigin(origins = "${cors.url:}")
@RestController
@RequestMapping("${kgrid.shelf.endpoint:}")
public abstract class KOSController {

  protected Logger log = LoggerFactory.getLogger(this.getClass().getName());
  protected Optional<KnowledgeObjectDecorator> kod;
  protected KnowledgeObjectRepository shelf;

  @Autowired
  public KOSController(KnowledgeObjectRepository shelf, Optional<KnowledgeObjectDecorator> kod) {
    this.shelf = shelf;
    this.kod = kod;
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
  @ExceptionHandler(ShelfResourceNotFound.class)
  public ResponseEntity<Map<String, String>> handleShelfResourceNotFoundExceptions(ShelfException e,
      WebRequest request) {

    return new ResponseEntity<>(getErrorMap(request, e.getMessage(), HttpStatus.BAD_REQUEST),
        HttpStatus.BAD_REQUEST);
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
