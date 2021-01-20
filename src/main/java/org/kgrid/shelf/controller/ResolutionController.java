package org.kgrid.shelf.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}/ark:/")
@CrossOrigin(origins = "${cors.url:}")
public class ResolutionController {

  @GetMapping(path = {"{naan}/{name}","{naan}/{name}/**"})
  public ResponseEntity<Map> resolve(HttpServletRequest request) {

    String redirectURI = request.getRequestURI().replace("ark:/","");
    ResponseEntity<Map> response = ResponseEntity
            .status(HttpStatus.FOUND)
            .header("Location", redirectURI)
            .body(Collections.singletonMap("Location", redirectURI));

    return response;
  }
}
