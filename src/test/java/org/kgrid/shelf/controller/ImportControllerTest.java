package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
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

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImportControllerTest {

  private ArkId validArkId;
  private ImportController importController;
  private MultipartFile multiPartFile;
  private ImportService mockImportService;
  private ManifestReader mockManifestReader;

  @BeforeEach
  public void setup() throws Exception {
    mockImportService = Mockito.mock(ImportService.class);
    KnowledgeObjectRepository mockKnowledgeObjectRepository =
        Mockito.mock(KnowledgeObjectRepository.class);
    mockManifestReader = Mockito.mock(ManifestReader.class);
    validArkId = new ArkId(NAAN, NAME, VERSION_1);
    multiPartFile = mock(MultipartFile.class);
    importController =
        new ImportController(mockKnowledgeObjectRepository, mockImportService, mockManifestReader);
  }

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
        importController.depositKnowledgeObject(multiPartFile);

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
    ResponseEntity<ArrayNode> response = importController.depositManifest(manifest);
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
    ResponseEntity<ArrayNode> response = importController.depositManifest(manifest);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  @DisplayName("Deposit manifests handles multiple manifests")
  public void depositManifests_SendsManifestsToManifestReader() {
    ArrayNode manifestList = getManifestListNode();
    importController.depositManifests(manifestList);
    verify(mockManifestReader).loadManifests(manifestList);
  }
}
