package org.kgrid.shelf.controller;

import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfResourceForbidden;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
@ConditionalOnProperty(name = "kgrid.shelf.expose.artifacts", matchIfMissing = true)
public class BinaryController extends ShelfExceptionHandler {

  public BinaryController(
      KnowledgeObjectRepository koRepo, Optional<KnowledgeObjectDecorator> kod) {
    super(koRepo, kod);
  }

  @GetMapping(path = "/{naan}/{name}/{version}/**")
  public ResponseEntity<Object> getBinary(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletRequest request) {
    String childPath = getChildPath(naan, name, version, request.getRequestURI());
    log.info("getting ko resource " + naan + "/" + name + "/" + version + childPath);
    HttpHeaders headers = getHeadersFromFileExt(childPath);
    return new ResponseEntity<>(
        koRepo.getBinary(new ArkId(naan, name, version), childPath), headers, HttpStatus.OK);
  }

  private HttpHeaders getHeadersFromFileExt(String childPath) {
    HttpHeaders headers = new HttpHeaders();
    if (childPath.endsWith(".json")) {
      headers.add("Content-Type", "application/json");
    } else if (childPath.endsWith(".yaml")) {
      headers.add("Content-Type", "application/yaml");
    } else {
      headers.add("Content-Type", "application/octet-stream");
    }
    return headers;
  }

  private String getChildPath(String naan, String name, String version, String requestURI) {
    String filepath =
        StringUtils.substringAfterLast(
            requestURI, StringUtils.join(naan, "/", name, "/", version, "/"));
    if (filepath.contains("../")) {
      throw new ShelfResourceForbidden(
          "Cannot navigate up a directory to get a resource outside of the ark:/"
              + naan
              + "/"
              + name
              + "/"
              + version
              + " knowledge object.");
    }
    return filepath;
  }
}
