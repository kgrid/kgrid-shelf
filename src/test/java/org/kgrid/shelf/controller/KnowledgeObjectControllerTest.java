package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
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
  private final String metadataString = "{\"key\":\"a different value\"}";

  @Before
  public void setup() throws JsonProcessingException {
    koRepo = Mockito.mock(KnowledgeObjectRepository.class);
    koController = new KnowledgeObjectController(koRepo);
    JsonNode koNode = objectMapper.readTree("{\"key\":\"value\"}");
    koMap.put(ARK_ID, koNode);
    MockHttpServletRequest mockServletRequest = new MockHttpServletRequest();
    String childPath = "childpath";
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childPath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
    when(koRepo.findAll()).thenReturn(koMap);
    when(koRepo.findKnowledgeObjectMetadata(ARK_ID)).thenReturn(koNode);
    when(koRepo.findKnowledgeObjectMetadata(arkNoVersion)).thenReturn(koNode);
    when(koRepo.editMetadata(ARK_ID, metadataString))
        .thenReturn((ObjectNode) objectMapper.readTree(metadataString));
    when(koRepo.editMetadata(ARK_ID, metadataString))
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
  public void editKnowledgeObjectMetadata_CallsEditMetadataOnKoRepo() {
    koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
    verify(koRepo).editMetadata(ARK_ID, metadataString);
  }

  @Test
  public void editKnowledgeObjectMetadata_ReturnsNewMetadata() {
    ResponseEntity<JsonNode> newMetaData =
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
    assertEquals(metadataString, Objects.requireNonNull(newMetaData.getBody()).toString());
  }

  @Test
  public void editVersionMetadata_CallsEditMetadataOnKoRepo() {
    koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
    verify(koRepo).editMetadata(ARK_ID, metadataString);
  }

  @Test
  public void editVersionMetadata_ReturnsNewMetadata() {
    ResponseEntity<JsonNode> newMetaData =
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);

    assertEquals(metadataString, Objects.requireNonNull(newMetaData.getBody()).toString());
  }

  @Test
  public void deleteKnowledgeObject_callsDeleteOnKoRepo() {
    koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
    verify(koRepo).delete(ARK_ID);
  }

  @Test
  public void deleteKnowledgeObject_ReturnsNoContent() {
    ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
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
}
