package org.kgrid.shelf.controller;

import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfResourceForbidden;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
@ConditionalOnProperty(name = "kgrid.shelf.expose.artifacts", matchIfMissing = true)
public class BinaryController extends ShelfExceptionHandler {

  @Bean
  public static MimetypesFileTypeMap getFilemap() {
    MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();
    fileTypeMap.addMimeTypes(
        "application/yaml yaml YAML\n"
            + "application/json json JSON\n"
            + "text/javascript js JS\n"
            + "application/pdf pdf PDF\n"
            + "text/csv csv CSV\n"
            + "application/zip zip ZIP");
    return fileTypeMap;
  }

  FileTypeMap fileTypeMap;

  public BinaryController(
      KnowledgeObjectRepository koRepo,
      Optional<KnowledgeObjectDecorator> kod,
      MimetypesFileTypeMap fileTypeMap) {
    super(koRepo, kod);
    this.fileTypeMap = fileTypeMap;
  }

  @GetMapping(path = "/{naan}/{name}/{version}/**")
  public ResponseEntity<Object> getBinary(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletRequest request) {
    String childPath = getChildPath(naan, name, version, request.getRequestURI());
    log.info("getting ko resource " + naan + "/" + name + "/" + version + "/" + childPath);

    final ArkId arkId = new ArkId(naan, name, version);
    InputStream fileStream = koRepo.getBinaryStream(arkId, childPath);
    HttpHeaders headers = getContentHeaders(childPath, arkId);

    return new ResponseEntity<>(new InputStreamResource(fileStream), headers, HttpStatus.OK);
  }

  private HttpHeaders getContentHeaders(String childPath, ArkId arkId) {
    HttpHeaders headers = new HttpHeaders();
    String contentType = fileTypeMap.getContentType(childPath);
    headers.add("Content-Type", contentType);

    String filename =
        childPath.contains("/") ? StringUtils.substringAfterLast(childPath, "/") : childPath;
    String contentDisposition = "inline; filename=\"" + filename + "\"";

    headers.add("Content-Disposition", contentDisposition);
    headers.add("Content-Length", String.valueOf(koRepo.getBinarySize(arkId, childPath)));
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
