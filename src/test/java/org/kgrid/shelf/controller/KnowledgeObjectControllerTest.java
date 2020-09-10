package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfResourceForbidden;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KnowledgeObjectControllerTest {

  private KnowledgeObjectRepository koRepo;
  private KnowledgeObjectController koController;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HashMap<ArkId, JsonNode> koMap = new HashMap<>();
  private final ArkId arkNoVersion = new ArkId(NAAN, NAME);
  private MockHttpServletRequest mockServletRequest;
  private final String childpath = "childpath";
  private final String metadataString = "{\"key\":\"a different value\"}";
  private JsonNode koNode;

  @Before
  public void setup() throws JsonProcessingException {
    koRepo = Mockito.mock(KnowledgeObjectRepository.class);
    koController = new KnowledgeObjectController(koRepo, null);
    koNode = objectMapper.readTree("{\"key\":\"value\"}");
    koMap.put(ARK_ID, koNode);
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childpath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
    ReflectionTestUtils.setField(koController, "binariesExposed", Boolean.valueOf("true"));
    when(koRepo.findAll()).thenReturn(koMap);
    when(koRepo.findKnowledgeObjectMetadata(ARK_ID)).thenReturn(koNode);
    when(koRepo.findKnowledgeObjectMetadata(arkNoVersion)).thenReturn(koNode);
    when(koRepo.findServiceSpecification(ARK_ID)).thenReturn(koNode);
    when(koRepo.findDeploymentSpecification(ARK_ID)).thenReturn(koNode);
    when(koRepo.getBinary(ARK_ID, childpath)).thenReturn("byteArray".getBytes());
    when(koRepo.editMetadata(ARK_ID, metadataString))
        .thenReturn((ObjectNode) objectMapper.readTree(metadataString));
    when(koRepo.editMetadata(arkNoVersion, metadataString))
        .thenReturn((ObjectNode) objectMapper.readTree(metadataString));
  }

  @Test
  public void getAllObjects_CallsFindAllOnKoRepo() {
    koController.getAllObjects();
    verify(koRepo).findAll();
  }

  @Test
  public void getAllObjects_ReturnsCollectionOfKos() {
    Collection kos = koController.getAllObjects();
    assertEquals(kos.size(), 1);
  }

  @Test
  public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
    koController.findKnowledgeObject(NAAN, NAME, VERSION);
    verify(koRepo).findKnowledgeObjectMetadata(ARK_ID);
  }

  @Test
  public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
    koController.findKnowledgeObject(NAAN, NAME, null);
    verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
  }

  @Test
  public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
    koController.getKnowledgeObjectOldVersion(NAAN, NAME, VERSION);
    verify(koRepo).findKnowledgeObjectMetadata(ARK_ID);
  }

  @Test
  public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
    koController.getKnowledgeObjectOldVersion(NAAN, NAME, null);
    verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
  }

  @Test
  public void getServiceDescriptionJson_CallsFindServiceSpecOnKoRepo() {
    koController.getServiceDescriptionJson(NAAN, NAME, VERSION);
    verify(koRepo).findServiceSpecification(ARK_ID);
  }

  @Test
  public void getServiceDescriptionJsonOldVersion_CallsFindServiceSpecOnKoRepo() {
    koController.getServiceDescriptionOldVersionJson(NAAN, NAME, VERSION);
    verify(koRepo).findServiceSpecification(ARK_ID);
  }

  @Test
  public void getServiceDescriptionYaml_CallsFindServiceSpecOnKoRepo()
      throws JsonProcessingException {
    koController.getServiceDescriptionYaml(NAAN, NAME, VERSION);
    verify(koRepo).findServiceSpecification(ARK_ID);
  }

  @Test
  public void getServiceDescriptionYaml_ReturnsYaml() throws JsonProcessingException {
    ResponseEntity<String> serviceDescriptionYaml =
        koController.getServiceDescriptionYaml(NAAN, NAME, VERSION);
    assertEquals("---\nkey: \"value\"\n", serviceDescriptionYaml.getBody());
  }

  @Test
  public void getServiceDescriptionYamlOldVersion_CallsFindServiceSpecOnKoRepo()
      throws JsonProcessingException {
    koController.getOldServiceDescriptionYaml(NAAN, NAME, VERSION);
    verify(koRepo).findServiceSpecification(ARK_ID);
  }

  @Test
  public void getServiceDescriptionYamlOldVersion_ReturnsYaml() throws JsonProcessingException {
    ResponseEntity<String> serviceDescriptionYaml =
        koController.getOldServiceDescriptionYaml(NAAN, NAME, VERSION);
    assertEquals("---\nkey: \"value\"\n", serviceDescriptionYaml.getBody());
  }

  @Test
  public void getBinary_CallsGetBinaryOnKoRepo() throws NoSuchFileException {
    koController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    verify(koRepo).getBinary(ARK_ID, childpath);
  }

  @Test
  public void getBinary_ThrowsErrorWhenTryingToEscapeKO() {
    String badChildpath = "../ko2/metadata.json";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + badChildpath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));

    assertThrows(
        ShelfResourceForbidden.class,
        () -> koController.getBinary(NAAN, NAME, VERSION, mockServletRequest));
  }

  @Test
  public void getBinary_ThrowsErrorWhenTryingAccessFileOnRestrictedShelf() {
    ReflectionTestUtils.setField(koController, "binariesExposed", Boolean.valueOf("false"));
    assertThrows(
        ShelfResourceForbidden.class,
        () -> koController.getBinary(NAAN, NAME, VERSION, mockServletRequest));
  }

  @Test
  public void getBinary_hasJsonContentTypeForJsonFileExt() {

    String jsonChildpath = "metadata.json";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + jsonChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> jsonResp =
        koController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    assertEquals(
        "Returns a json header for a path ending in '.json'",
        jsonResp.getHeaders().get("Content-Type").get(0),
        MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  public void getBinary_hasYamlContentTypeForYamlFileExt() {

    String jsonChildpath = "deployment.yaml";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + jsonChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> yamlResp =
        koController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    assertEquals(
        "Returns a yaml header for a path ending in '.yaml'",
        yamlResp.getHeaders().get("Content-Type").get(0),
        "application/yaml");
  }

  @Test
  public void getBinary_hasOctetContentTypeForUnknownFileExt() {

    String pdfChildpath = "file.pdf";
    mockServletRequest = new MockHttpServletRequest();
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + pdfChildpath;
    mockServletRequest.setRequestURI(requestUri);
    ResponseEntity<Object> yamlResp =
        koController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
    assertEquals(
        "Returns an octet header for an unknown filetype",
        yamlResp.getHeaders().get("Content-Type").get(0),
        MediaType.APPLICATION_OCTET_STREAM_VALUE);
  }

  @Test
  public void editKnowledgeObjectMetadata_CallsEditMetadataOnKoRepo() {
    koController.editKnowledgeObjectMetadata(NAAN, NAME, metadataString);
    verify(koRepo).editMetadata(arkNoVersion, metadataString);
  }

  @Test
  public void editKnowledgeObjectMetadata_ReturnsNewMetadata() {
    ResponseEntity<JsonNode> newMetaData =
        koController.editKnowledgeObjectMetadata(NAAN, NAME, metadataString);
    assertEquals(metadataString, newMetaData.getBody().toString());
  }

  @Test
  public void editVersionMetadata_CallsEditMetadataOnKoRepo() {
    koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
    verify(koRepo).editMetadata(ARK_ID, metadataString);
  }

  @Test
  public void editVersionMetadata_ReturnsNewMetadata() throws JsonProcessingException {
    ResponseEntity<JsonNode> newMetaData =
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);

    assertEquals(metadataString, newMetaData.getBody().toString());
  }

  @Test
  public void deleteKnowledgeObject_callsDeleteOnKoRepo() {
    koController.deleteKnowledgeObject(NAAN, NAME);
    verify(koRepo).delete(arkNoVersion);
  }

  @Test
  public void deleteKnowledgeObject_ReturnsNoContent() {
    ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  public void deleteKnowledgeObject_callsDeleteOnKoRepo_WithVersion() {
    koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
    verify(koRepo).delete(ARK_ID);
  }

  @Test
  public void deleteKnowledgeObject_ReturnsNoContent_WithVersion() {
    ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }

  @Test
  public void getDeploymentDescriptionJson_callsFindDeploymentSpecOnKoRepo() {
    koController.getDeploymentDescriptionJson(NAAN, NAME, VERSION);
    verify(koRepo).findDeploymentSpecification(ARK_ID);
  }

  @Test
  public void getDeploymentDescriptionJson_returnsDeploymentSpecFromKoRepo() {
    ResponseEntity<JsonNode> deploymentDescriptionJson =
        koController.getDeploymentDescriptionJson(NAAN, NAME, VERSION);
    assertEquals(koNode, deploymentDescriptionJson.getBody());
  }
}
