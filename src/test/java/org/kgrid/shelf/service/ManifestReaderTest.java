package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ManifestReaderTest extends TestCase {

  private static final String GOOD_MANIFEST_PATH = "GOOD_MANIFEST_PATH";
  private static final String BAD_MANIFEST_PATH = "BAD_MANIFEST_PATH";
  private static final String RESOURCE_1_URI = "RESOURCE_1_URI";
  private static final String RESOURCE_2_URI = "RESOURCE_2_URI";

  @Mock ImportService importService;
  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock ObjectMapper mapper;

  @InjectMocks ManifestReader manifestReader;

  private InputStream mockResourceInputStream;
  private Resource mockResource;

  @Before
  public void setUp() throws IOException {
    ObjectNode manifestNode = getManifestNode();
    mockResourceInputStream = Mockito.mock(InputStream.class);
    mockResourceInputStream = Mockito.mock(InputStream.class);
    mockResource = Mockito.mock(Resource.class);
    when(applicationContext.getResource(GOOD_MANIFEST_PATH)).thenReturn(mockResource);
    when(applicationContext.getResource(BAD_MANIFEST_PATH)).thenThrow(new NullPointerException());
    when(mapper.readTree(mockResourceInputStream)).thenReturn(manifestNode);
    when(mockResource.getInputStream()).thenReturn(mockResourceInputStream);
    when(mapper.createArrayNode()).thenReturn(new ObjectMapper().createArrayNode());
  }

  @Test
  public void afterPropertiesSet_LoadsGoodManifestsAndHandlesBadManifests() {
    String[] manifests = new String[] {GOOD_MANIFEST_PATH, BAD_MANIFEST_PATH, GOOD_MANIFEST_PATH};
    ReflectionTestUtils.setField(manifestReader, "startupManifestLocations", manifests);

    manifestReader.afterPropertiesSet();

    verify(importService, times(4)).importZip(any(URI.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void afterPropertiesSet_malformedManifestThrowsIllegalArgumentException()
      throws IOException {

    ObjectNode badManifest = JsonNodeFactory.instance.objectNode().put("shmanifest", "bad");
    when(mapper.readTree(mockResourceInputStream)).thenReturn(badManifest);

    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});

    manifestReader.afterPropertiesSet();
  }

  @Test
  public void afterPropertiesSet_readTreeThrowsIOException() throws IOException {
    when(mapper.readTree(mockResourceInputStream)).thenThrow(IOException.class);

    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});
    manifestReader.afterPropertiesSet();
    verify(importService, never()).importZip(any(URI.class));
  }

  @Test
  public void loadManifest_SkipsKoWhenImportFails() {

    ReflectionTestUtils.setField(
        manifestReader, "startupManifestLocations", new String[] {GOOD_MANIFEST_PATH});

    when(importService.importZip(URI.create(RESOURCE_1_URI))).thenThrow(RuntimeException.class);
    final URI resource2 = URI.create(RESOURCE_2_URI);
    when(importService.importZip(resource2)).thenReturn(resource2);

    Map<String, ArrayNode> map = manifestReader.loadManifest(getManifestNode());
    assertEquals(1, map.get("Added").size());
  }

  private ObjectNode getManifestNode() {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    ArrayNode uris = node.putArray("manifest");
    uris.add(RESOURCE_1_URI);
    uris.add(RESOURCE_2_URI);
    return node;
  }
}
