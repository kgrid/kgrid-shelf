package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportExportException;
import org.kgrid.shelf.service.ImportService;
import org.kgrid.shelf.service.ManifestReader;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
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

import static org.junit.Assert.assertEquals;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportExportControllerTest {

  private KnowledgeObjectRepository mockKnowledgeObjectRepository;
  private ArkId validArkId;
  private ServletOutputStream mockServletOutputStream;
  private ImportExportController importExportController;
  private HttpServletResponse servletResponse;
  private MultipartFile multiPartFile;
  private ImportService mockImportService;
  private ExportService mockExportService;
  private ManifestReader mockManifestReader;

  @Before
  public void setup() throws Exception {
    mockImportService = Mockito.mock(ImportService.class);
    mockExportService = Mockito.mock(ExportService.class);
    mockKnowledgeObjectRepository = Mockito.mock(KnowledgeObjectRepository.class);
    mockServletOutputStream = Mockito.mock(ServletOutputStream.class);
    mockManifestReader = Mockito.mock(ManifestReader.class);

    validArkId = new ArkId(NAAN, NAME, VERSION);
    servletResponse = mock(HttpServletResponse.class);
    multiPartFile = mock(MultipartFile.class);
    when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    importExportController =
        new ImportExportController(
            mockImportService,
            mockExportService,
            mockManifestReader,
            mockKnowledgeObjectRepository,
            null);
  }

  @Test
  public void exportKnowledgeObjectVersion_SetsContentHeaderOnServletResponse() {

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setHeader("Content-Type", "application/octet-stream");
  }

  @Test
  public void exportKnowledgeObjectVersion_AddsContentDispositionHeaderToResponse() {

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse)
        .addHeader(
            "Content-Disposition",
            "attachment; filename=\"" + NAAN + "-" + NAME + "-" + VERSION + ".zip\"");
  }

  @Test
  public void exportKnowledgeObjectVersion_CallsExtractZipWithOutputStream() {

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(mockExportService).zipKnowledgeObject(validArkId, mockServletOutputStream);
  }

  @Test
  public void exportKnowledgeObjectVersion_ClosesResponseOutputStream() throws IOException {

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(mockServletOutputStream).close();
  }

  @Test
  public void exportKnowledgeObjectVersion_HandlesIOExceptionFromExportService() {
    doThrow(new ImportExportException("From Controller", new IOException("from ExportService")))
        .when(mockExportService)
        .zipKnowledgeObject(any(), any());

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void exportKnowledgeObjectVersion_HandlesIOExceptionFromClosingOStream()
      throws IOException {
    doThrow(new IOException("OPE")).when(mockServletOutputStream).close();

    importExportController.exportKnowledgeObjectVersion(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void exportKnowledgeObject_SetsContentHeaderOnServletResponse() {

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setHeader("Content-Type", "application/octet-stream");
  }

  @Test
  public void exportKnowledgeObject_AddsContentDispositionHeaderToResponse() {

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse)
        .addHeader(
            "Content-Disposition",
            "attachment; filename=\"" + NAAN + "-" + NAME + "-" + VERSION + ".zip\"");
  }

  @Test
  public void exportKnowledgeObject_CallsExtractZipWithOutputStream() {

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(mockExportService).zipKnowledgeObject(validArkId, mockServletOutputStream);
  }

  @Test
  public void exportKnowledgeObject_ClosesResponseOutputStream() throws IOException {

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(mockServletOutputStream).close();
  }

  @Test
  public void exportKnowledgeObject_HandlesIOExceptionFromKORepo() {
    doThrow(new ImportExportException("OPE", new IOException("from Export Service")))
        .when(mockExportService)
        .zipKnowledgeObject(any(), any());

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void exportKnowledgeObject_HandlesIOExceptionFromClosingOStream() throws IOException {
    doThrow(new IOException("OPE")).when(mockServletOutputStream).close();

    importExportController.exportKnowledgeObject(NAAN, NAME, VERSION, servletResponse);

    verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  public void exportKnowledgeObject_AddsContentDispositionHeaderToResponse_WhenArkIsVersionless() {

    importExportController.exportKnowledgeObject(NAAN, NAME, null, servletResponse);

    verify(servletResponse)
        .addHeader("Content-Disposition", "attachment; filename=\"" + NAAN + "-" + NAME + ".zip\"");
  }

  @Test
  public void exportKnowledgeObject_CallsExtractZipWithOutputStream_WhenArkIsVersionless() {

    importExportController.exportKnowledgeObject(NAAN, NAME, null, servletResponse);

    verify(mockExportService).zipKnowledgeObject(new ArkId(NAAN, NAME), mockServletOutputStream);
  }

  @Test
  public void depositKnowledgeObject_CallsImportZipOnKORepo() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    URI requestUri = URI.create("requestUri/");
    request.setRequestURI(requestUri.toString());
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    when(mockImportService.importZip((MultipartFile) any()))
        .thenReturn(URI.create(NAAN + "/" + NAME));

    HttpHeaders expectedHeaders = new HttpHeaders();
    expectedHeaders.setLocation(URI.create("http://localhost/requestUri/naan/name"));

    ResponseEntity<Map<String, String>> mapResponseEntity =
        importExportController.depositKnowledgeObject(multiPartFile);

    assertEquals(expectedHeaders, mapResponseEntity.getHeaders());
  }

  @Test
  public void depositManifest_SendsListToManifestReader() {
    ObjectNode manifest = getManifestNode();
    importExportController.depositManifest(manifest);
    verify(mockManifestReader, times(1)).loadManifest(manifest);
  }

  @Test
  public void depositManifests_SendsManifestsToManifestReader() {
    ArrayNode manifestList = getManifestListNode();
    importExportController.depositManifests(manifestList);
    verify(mockManifestReader, times(1)).loadManifests(manifestList);
  }
}
