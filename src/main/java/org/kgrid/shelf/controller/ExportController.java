package org.kgrid.shelf.controller;

import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportExportException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class ExportController extends ShelfExceptionHandler {

  private final ExportService exportService;

  public ExportController(KnowledgeObjectRepository koRepo, ExportService exportService) {
    super(koRepo);
    this.exportService = exportService;
  }

  @GetMapping(path = "/{naan}/{name}/{version}", produces = "application/zip")
  public void exportKnowledgeObjectVersion(
      @PathVariable String naan,
      @PathVariable String name,
      @PathVariable String version,
      HttpServletResponse response) {

    exportKnowledgeObject(naan, name, version, response);
  }

  @GetMapping(path = "/{naan}/{name}", produces = "application/zip")
  public void exportKnowledgeObject(
      @PathVariable String naan,
      @PathVariable String name,
      @RequestParam(name = "v", required = false) String version,
      HttpServletResponse response) {

    ArkId arkId;
    if (version != null && !"".equals(version)) {
      log.info("get ko zip for " + naan + "/" + name + "/" + version);
      arkId = new ArkId(naan, name, version);
    } else {
      log.info("get ko zip for " + naan + "/" + name);
      arkId = new ArkId(naan, name);
    }
    response.setHeader("Content-Type", "application/octet-stream");
    response.addHeader(
        "Content-Disposition", "attachment; filename=\"" + arkId.getFullDashArk() + ".zip\"");
    ServletOutputStream outputStream = null;
    try {
      outputStream = response.getOutputStream();
      exportService.zipKnowledgeObject(arkId, outputStream);
      outputStream.close();
    } catch (ImportExportException | ShelfResourceNotFound e) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }
}
