package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.repository.ZipImportExportTestHelper;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class KnowledgeObjectWrapperTest {
  JsonNode metadata;
  KnowledgeObjectWrapper kow;

  @Before
  public void setUp() {
    metadata =
        ZipImportExportTestHelper.generateMetadata(
            ZipImportExportTestHelper.SERVICE_YAML_PATH,
            ZipImportExportTestHelper.DEPLOYMENT_YAML_PATH,
            true,
            true,
            true,
            true);
    kow = new KnowledgeObjectWrapper(metadata);
  }

  @Test
  public void metadataCanBeExtractedToJsonNode() {

    Map<KoFields, URI> metadataURIs = kow.getKoParts();

    assertEquals(3, metadataURIs.size());
    assertTrue(metadataURIs.containsValue(URI.create("metadata.json")));
  }

  @Test
  public void artifactLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(new YAMLMapper().readTree(ZipImportExportTestHelper.DEPLOYMENT_BYTES));
    List<URI> artifacts = kow.getArtifactLocations();
    assertEquals(4, artifacts.size());
  }

  @Test
  public void artifactArrayLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(
        new YAMLMapper()
            .readTree(
                "endpoints:\n  /welcome:\n    artifact: \n    - payload.js\n    - payload2.js\n    function: welcome\n"));
    List<URI> artifacts = kow.getArtifactLocations();
    assertEquals(5, artifacts.size());
  }

  @Test
  public void noDeploymentSpecWarns() {
    List<URI> artifacts = kow.getArtifactLocations();
    assertEquals(3, artifacts.size());
  }
}
