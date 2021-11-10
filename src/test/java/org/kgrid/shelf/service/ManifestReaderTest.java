package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Manifest Reader Tests")
public class ManifestReaderTest {

  @Mock private ImportService importService;
  @Spy private final ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock private ObjectMapper mapper;

  @InjectMocks private ManifestReader manifestReader;

  private InputStream mockResourceInputStream;
  private Resource mockResource;

  @BeforeEach
  public void setUp() throws IOException {

    mockResourceInputStream = Mockito.mock(InputStream.class);
    mockResourceInputStream = Mockito.mock(InputStream.class);
    mockResource = Mockito.mock(Resource.class);
    Mockito.lenient()
        .when(applicationContext.getResource(GOOD_MANIFEST_PATH))
        .thenReturn(mockResource);
    Mockito.lenient().when(mockResource.getURI()).thenReturn(URI.create(GOOD_MANIFEST_PATH));
    Mockito.lenient().when(mockResource.getInputStream()).thenReturn(mockResourceInputStream);
    Mockito.lenient()
        .when(mapper.createArrayNode())
        .thenReturn(JsonNodeFactory.instance.arrayNode());
    Mockito.lenient()
        .when(importService.importZip(URI.create(RESOLVED_RELATIVE_RESOURCE_URI)))
        .thenReturn(URI.create(RELATIVE_RESOURCE_URI));
    Mockito.lenient()
        .when(importService.importZip(URI.create(ABSOLUTE_RESOURCE_URI)))
        .thenReturn(URI.create(ABSOLUTE_RESOURCE_URI));
  }

  @Test
  @DisplayName("Loads JsonLd Manifest")
  public void loadManifest_LoadsJsonLdManifests() throws MalformedURLException {
    when(importService.importZip(URI.create(RELATIVE_RESOURCE_URI)))
            .thenReturn(URI.create(RELATIVE_RESOURCE_URI));
    when(importService.importZip(URI.create(ABSOLUTE_RESOURCE_URI)))
            .thenReturn(URI.create(ABSOLUTE_RESOURCE_URI));
    ArrayNode jsonNodes = manifestReader.loadManifest(getJsonLdManifestNode());
    assertEquals(2,jsonNodes.size());
  }

  @Test
  @DisplayName("Loads good manifest and handles bad manifest")
  public void afterPropertiesSet_LoadsGoodManifestsAndHandlesBadManifests() throws IOException {
    String[] manifests = new String[] {GOOD_MANIFEST_PATH, BAD_MANIFEST_PATH, GOOD_MANIFEST_PATH};
    ReflectionTestUtils.setField(manifestReader, "startupManifestLocations", manifests);
    when(mapper.readTree(mockResourceInputStream)).thenReturn(getManifestNode());
    manifestReader.afterPropertiesSet();

    verify(importService, times(4)).importZip(any(URI.class));
  }

  @Test
  @DisplayName("Read tree throws io exception")
  public void afterPropertiesSet_readTreeThrowsIOException() throws IOException {
    when(mapper.readTree(mockResourceInputStream)).thenThrow(IOException.class);

    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});
    manifestReader.afterPropertiesSet();
    verify(importService, never()).importZip(any(URI.class));
  }

  @Test
  @DisplayName("Skips ko when import fails")
  public void loadManifest_SkipsKoWhenImportFails() throws MalformedURLException {

    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});

    when(importService.importZip(URI.create(RELATIVE_RESOURCE_URI)))
        .thenThrow(RuntimeException.class);
    URI resource2 = URI.create(ABSOLUTE_RESOURCE_URI);
    when(importService.importZip(resource2)).thenReturn(resource2);

    ArrayNode kos = manifestReader.loadManifest(getManifestNode());
    assertEquals(1, kos.size());
  }

  @Test
  @DisplayName("Skips ko when import fails")
  public void afterPropertiesSet_ResolvesRelativeUrisButNotAbsoluteOnes() throws IOException {
    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});
    JsonNode manifestNode = getManifestNode();
    when(mapper.readTree(mockResourceInputStream)).thenReturn(manifestNode);
    manifestReader.afterPropertiesSet();
    assertAll(
        () -> verify(importService).importZip(URI.create(RESOLVED_RELATIVE_RESOURCE_URI)),
        () -> verify(importService).importZip(URI.create(ABSOLUTE_RESOURCE_URI)));
  }

  @Test
  @DisplayName("Doesn't fail when no base url")
  public void loadManifest_doesntFailWhenNoBaseUrl() {
    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});
    JsonNode manifestNode = getManifestNode();
    manifestReader.loadManifest(manifestNode);
    assertAll(
        () -> verify(importService).importZip(URI.create(RELATIVE_RESOURCE_URI)),
        () -> verify(importService).importZip(URI.create(ABSOLUTE_RESOURCE_URI)));
  }

  @Test
  @DisplayName("Loads a list of manifests")
  public void loadManifests_loadsListOfManifests() throws IOException {
    when(applicationContext.getResource(GOOD_MANIFEST_PATH)).thenReturn(mockResource);
    when(mapper.readTree(mockResourceInputStream)).thenReturn(getManifestNode());

    ArrayNode addedObjects = manifestReader.loadManifests(getManifestListNode());
    assertAll(
        () -> assertEquals(RELATIVE_RESOURCE_URI, addedObjects.get(0).asText()),
        () -> assertEquals(ABSOLUTE_RESOURCE_URI, addedObjects.get(1).asText()));
  }

  @Test
  public void loadManifests_catchesExceptionWhenFailingToCreateKoUri() throws IOException {
    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});
    JsonNode manifestNode = getManifestNodeWithBadUri();
    when(mapper.readTree(mockResourceInputStream)).thenReturn(manifestNode);
    manifestReader.afterPropertiesSet();
    assertAll(
        () -> verify(importService).importZip(URI.create(RESOLVED_RELATIVE_RESOURCE_URI)),
        () -> verify(importService).importZip(URI.create(ABSOLUTE_RESOURCE_URI)));
  }
}
