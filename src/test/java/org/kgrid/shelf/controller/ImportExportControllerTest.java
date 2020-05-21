package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportExportControllerTest {

  private static final String GOOD_MANIFEST_PATH = "GOOD_MANIFEST_PATH";
  private static final String BAD_MANIFEST_PATH = "BAD_MANIFEST_PATH";
  private static final String RESOURCE_1_URI = "RESOURCE_1_URI";
  private static final String RESOURCE_2_URI = "RESOURCE_2_URI";
  private KnowledgeObjectRepository mockKnowledgeObjectRepository;
  private ApplicationContext mockApplicationContext;
  private ObjectMapper mockMapper;
  private ArkId validArkId;
  private InputStream mockResourceInputStream;
  private ImportExportController importExportController;

  @Before
  public void setup() throws Exception {
    mockKnowledgeObjectRepository = Mockito.mock(KnowledgeObjectRepository.class);
    mockApplicationContext = Mockito.mock(ApplicationContext.class);
    mockMapper = Mockito.mock(ObjectMapper.class);
    Resource mockResource = Mockito.mock(Resource.class);
    mockResourceInputStream = Mockito.mock(InputStream.class);
    validArkId = new ArkId("naan", "name");
    ObjectNode manifestNode = getManifestNode();

    when(mockResource.getInputStream()).thenReturn(mockResourceInputStream);
    when(mockApplicationContext.getResource(GOOD_MANIFEST_PATH)).thenReturn(mockResource);
    when(mockApplicationContext.getResource(BAD_MANIFEST_PATH))
        .thenThrow(new NullPointerException());
    when(mockApplicationContext.getResource(RESOURCE_1_URI)).thenReturn(mockResource);
    when(mockApplicationContext.getResource(RESOURCE_2_URI)).thenReturn(mockResource);
    when(mockMapper.readTree(mockResourceInputStream)).thenReturn(manifestNode);
    when(mockKnowledgeObjectRepository.importZip(mockResourceInputStream)).thenReturn(validArkId);
  }

  @Test
  public void afterPropertiesSet_LoadsGoodManifestsAndHandlesBadManifests() {
    String[] manifests = new String[] {GOOD_MANIFEST_PATH, BAD_MANIFEST_PATH, GOOD_MANIFEST_PATH};
    importExportController = getImportExportControllerForManifestList(manifests);
    importExportController.afterPropertiesSet();

    verify(mockKnowledgeObjectRepository, times(4)).importZip(any(InputStream.class));
  }

  @Test
  public void afterPropertiesSet_emptyLocationNeverTriesToImport() {
    importExportController = getImportExportControllerForManifestList(null);

    importExportController.afterPropertiesSet();

    verify(mockKnowledgeObjectRepository, never()).importZip((InputStream) any());
  }

  @Test(expected = IllegalArgumentException.class)
  public void afterPropertiesSet_malformedManifestThrowsIllegalArgumentException()
      throws IOException {

    ObjectNode badManifest = JsonNodeFactory.instance.objectNode().put("shmanifest", "bad");
    when(mockMapper.readTree(mockResourceInputStream)).thenReturn(badManifest);

    importExportController =
        getImportExportControllerForManifestList(new String[] {GOOD_MANIFEST_PATH});

    importExportController.afterPropertiesSet();
  }

  @Test
  public void afterPropertiesSet_DoesNothingIfUnreadableManifestResourceIsLoaded()
      throws IOException {
    when(mockMapper.readTree(mockResourceInputStream)).thenThrow(new IOException());

    importExportController =
        getImportExportControllerForManifestList(new String[] {GOOD_MANIFEST_PATH});

    importExportController.afterPropertiesSet();

    verify(mockKnowledgeObjectRepository, never()).importZip((InputStream) any());
  }

  @Test
  public void afterPropertiesSet_singleShelfErrorIsSkipped() {
    when(mockKnowledgeObjectRepository.importZip((InputStream) any()))
        .thenThrow(new RuntimeException())
        .thenReturn(validArkId);
    importExportController = getImportExportControllerForManifestList(null);

    Map<String, Object> loaded = importExportController.loadManifestIfSet(GOOD_MANIFEST_PATH);

    verify(mockKnowledgeObjectRepository, times(2)).importZip((InputStream) any());

    assertEquals("should skip one and import one:", 1, ((ArrayNode) loaded.get("Added")).size());
  }

  private ObjectNode getManifestNode() {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    ArrayNode uris = node.putArray("manifest");
    uris.add(RESOURCE_1_URI);
    uris.add(RESOURCE_2_URI);
    return node;
  }

  private ImportExportController getImportExportControllerForManifestList(String[] manifests) {
    ImportExportController controller =
        new ImportExportController(mockKnowledgeObjectRepository, null, manifests);
    controller.applicationContext = mockApplicationContext;
    controller.mapper = mockMapper;
    return controller;
  }
}
