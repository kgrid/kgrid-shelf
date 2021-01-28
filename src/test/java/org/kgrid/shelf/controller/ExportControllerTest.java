package org.kgrid.shelf.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportExportException;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportControllerTest {

  private ArkId validArkId;
  private ServletOutputStream mockServletOutputStream;
  private ExportController exportController;
  private HttpServletResponse servletResponse;
  private ExportService mockExportService;

  @BeforeEach
  public void setup() throws Exception {
    mockExportService = Mockito.mock(ExportService.class);
    KnowledgeObjectRepository mockKnowledgeObjectRepository =
        Mockito.mock(KnowledgeObjectRepository.class);
    mockServletOutputStream = Mockito.mock(ServletOutputStream.class);
    validArkId = new ArkId(NAAN, NAME, VERSION);
    servletResponse = mock(HttpServletResponse.class);
    exportController = new ExportController(mockKnowledgeObjectRepository, mockExportService);
  }

  @Test
  @DisplayName("Export KO works correctly")
  public void exportKnowledgeObjectVersionHappyPath() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    exportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);
    assertAll(
        () -> verify(servletResponse).setHeader("Content-Type", "application/octet-stream"),
        () ->
            verify(servletResponse)
                .addHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + NAAN + "-" + NAME + "-" + VERSION + ".zip\""),
        () -> verify(mockExportService).zipKnowledgeObject(validArkId, mockServletOutputStream),
        () -> verify(mockServletOutputStream).close(),
        () -> verify(servletResponse).setHeader("Content-Type", "application/octet-stream"),
        () ->
            verify(servletResponse)
                .addHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + NAAN + "-" + NAME + "-" + VERSION + ".zip\""),
        () -> verify(mockExportService).zipKnowledgeObject(validArkId, mockServletOutputStream),
        () -> verify(mockServletOutputStream).close());
  }

  @Test
  @DisplayName("Export KO handles ImportExportException")
  public void exportKnowledgeObjectVersion_HandlesIOExceptionFromExportService()
      throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    doThrow(new ImportExportException("From Controller", new IOException("from ExportService")))
        .when(mockExportService)
        .zipKnowledgeObject(any(), any());
    exportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);
    verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  @DisplayName("Export KO handles IOException")
  public void exportKnowledgeObject_HandlesIOExceptionFromClosingOStream() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    doThrow(new IOException("OPE")).when(mockServletOutputStream).close();
    exportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);
    verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("Export KO handles null version")
  public void exportKnowledgeObject_ExportsVersionlessArk() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    exportController.exportKnowledgeObject(NAAN, NAME, null, servletResponse);
    assertAll(
        () ->
            verify(servletResponse)
                .addHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + NAAN + "-" + NAME + ".zip\""),
        () ->
            verify(mockExportService)
                .zipKnowledgeObject(new ArkId(NAAN, NAME), mockServletOutputStream));
  }
}
