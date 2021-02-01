package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Knowledge Object Controller Tests")
public class KnowledgeObjectControllerTest {

  private KnowledgeObjectRepository koRepo;
  private KnowledgeObjectController koController;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HashMap<ArkId, JsonNode> koMap = new HashMap<>();
  private final ArkId arkNoVersion = new ArkId(NAAN, NAME);
  private final String metadataString = "{\"key\":\"a different value\"}";

  @BeforeEach
  public void setup() throws JsonProcessingException {
    koRepo = Mockito.mock(KnowledgeObjectRepository.class);
    koController = new KnowledgeObjectController(koRepo);
    JsonNode koNode = objectMapper.readTree("{\"key\":\"value\"}");
    koMap.put(ARK_ID, koNode);
    MockHttpServletRequest mockServletRequest = new MockHttpServletRequest();
    String childPath = "childPath";
    String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childPath;
    mockServletRequest.setRequestURI(requestUri);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
  }

  @Test
  @DisplayName("Get all objects collection of objects")
  public void getAllObjects_ReturnsCollectionOfKos() {
    when(koRepo.findAll()).thenReturn(koMap);
    Collection<JsonNode> kos = koController.getAllObjects();
    assertAll(() -> verify(koRepo).findAll(), () -> assertEquals(kos.size(), 1));
  }

  @Test
  @DisplayName("Find knowledge object gets ko from repo")
  public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
    koController.findKnowledgeObject(NAAN, NAME, VERSION);
    verify(koRepo).findKnowledgeObjectMetadata(ARK_ID);
  }

  @Test
  @DisplayName("Find knowledge object without version gets ko from rep")
  public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
    koController.findKnowledgeObject(NAAN, NAME, null);
    verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
  }

  @Test
  @DisplayName("Old version find knowledge object gets ko from repo")
  public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
    koController.getKnowledgeObjectOldVersion(NAAN, NAME, VERSION);
    verify(koRepo).findKnowledgeObjectMetadata(ARK_ID);
  }

  @Test
  @DisplayName("Old version find knowledge object without version gets ko from rep")
  public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
    koController.getKnowledgeObjectOldVersion(NAAN, NAME, null);
    verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
  }

  @Test
  @DisplayName("Edit metadata calls edit in ko repo and returns new metadata")
  public void editKnowledgeObjectMetadata_CallsEditMetadataOnKoRepo()
      throws JsonProcessingException {
    when(koRepo.editMetadata(ARK_ID, metadataString))
        .thenReturn((ObjectNode) objectMapper.readTree(metadataString));
    ResponseEntity<JsonNode> newMetaData =
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
    assertAll(
        () -> verify(koRepo).editMetadata(ARK_ID, metadataString),
        () ->
            assertEquals(metadataString, Objects.requireNonNull(newMetaData.getBody()).toString()));
  }

  @Test
  @DisplayName("Delete ko calls delete in repo and returns no content")
  public void deleteKnowledgeObject_callsDeleteOnKoRepoAndReturnsNoContent() {
    ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
    assertAll(
        () -> verify(koRepo).delete(ARK_ID),
        () -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()));
  }
}
