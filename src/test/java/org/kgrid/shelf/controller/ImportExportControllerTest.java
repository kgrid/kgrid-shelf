package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportExportException;
import org.kgrid.shelf.service.ImportService;
import org.kgrid.shelf.service.ManifestReader;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImportExportControllerTest {

  private ArkId validArkId;
  private ServletOutputStream mockServletOutputStream;
  private ImportExportController importExportController;
  private HttpServletResponse servletResponse;
  private MultipartFile multiPartFile;
  private ImportService mockImportService;
  private ExportService mockExportService;
  private ManifestReader mockManifestReader;

  @BeforeEach
  public void setup() throws Exception {
    mockImportService = Mockito.mock(ImportService.class);
    mockExportService = Mockito.mock(ExportService.class);
    KnowledgeObjectRepository mockKnowledgeObjectRepository =
        Mockito.mock(KnowledgeObjectRepository.class);
    mockServletOutputStream = Mockito.mock(ServletOutputStream.class);
    mockManifestReader = Mockito.mock(ManifestReader.class);

    validArkId = new ArkId(NAAN, NAME, VERSION);
    servletResponse = mock(HttpServletResponse.class);
    multiPartFile = mock(MultipartFile.class);

    importExportController =
        new ImportExportController(
            mockImportService,
            mockExportService,
            mockManifestReader,
            mockKnowledgeObjectRepository,
            null);
  }

  // Export Tests:

  @Test
  @DisplayName("Export KO works correctly")
  public void exportKnowledgeObjectVersionHappyPath() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);
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
    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);
    verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  @DisplayName("Export KO handles IOException")
  public void exportKnowledgeObject_HandlesIOExceptionFromClosingOStream() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    doThrow(new IOException("OPE")).when(mockServletOutputStream).close();
    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);
    verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("Export KO handles null version")
  public void exportKnowledgeObject_ExportsVersionlessArk() throws IOException {
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    importExportController.exportKnowledgeObject(NAAN, NAME, null, servletResponse);
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

  // Import Tests:

  @Test
  @DisplayName("Deposit KO happy path")
  public void depositKnowledgeObject_CallsImportZipOnKORepo() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    URI requestUri = URI.create("requestUri/");
    request.setRequestURI(requestUri.toString());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    when(mockImportService.importZip(multiPartFile)).thenReturn(URI.create(NAAN + "/" + NAME));

    HttpHeaders expectedHeaders = new HttpHeaders();
    expectedHeaders.setLocation(URI.create("http://localhost/requestUri/naan/name"));

    ResponseEntity<Map<String, String>> mapResponseEntity =
        importExportController.depositKnowledgeObject(multiPartFile);

    assertAll(
        () -> verify(mockImportService).importZip(multiPartFile),
        () -> assertEquals(expectedHeaders, mapResponseEntity.getHeaders()));
  }

  @Test
  @DisplayName("Deposit manifest happy path")
  public void depositManifest_returns201InSuccessfulCase() {
    ObjectNode manifest = getManifestNode();
    when(mockManifestReader.loadManifest(manifest))
        .thenReturn(new ObjectMapper().createArrayNode().add(validArkId.getSlashArkVersion()));
    ResponseEntity<ArrayNode> response = importExportController.depositManifest(manifest);
    assertAll(
        () -> verify(mockManifestReader).loadManifest(manifest),
        () -> assertEquals(HttpStatus.CREATED, response.getStatusCode()));
  }

  @Test
  @DisplayName("Deposit manifest returns 400 if none added")
  public void depositManifest_returns400IfNoKosWereAdded() {
    ObjectNode manifest = getManifestNode();
    when(mockManifestReader.loadManifest(manifest))
        .thenReturn(new ObjectMapper().createArrayNode());
    ResponseEntity<ArrayNode> response = importExportController.depositManifest(manifest);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  @DisplayName("Deposit manifests handles multiple manifests")
  public void depositManifests_SendsManifestsToManifestReader() {
    ArrayNode manifestList = getManifestListNode();
    importExportController.depositManifests(manifestList);
    verify(mockManifestReader).loadManifests(manifestList);
  }
}
