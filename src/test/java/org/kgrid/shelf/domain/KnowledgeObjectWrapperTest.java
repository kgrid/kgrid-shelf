package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.kgrid.shelf.TestHelper.*;

@RunWith(MockitoJUnitRunner.class)
public class KnowledgeObjectWrapperTest {
  JsonNode metadata;
  KnowledgeObjectWrapper kow;

  @Before
  public void setUp() {
    metadata = generateMetadata(SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
    kow = new KnowledgeObjectWrapper(metadata);
  }

  @Test
  public void artifactLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(new YAMLMapper().readTree(DEPLOYMENT_BYTES));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(4, artifacts.size());
  }

  @Test
  public void artifactArrayLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(
        new YAMLMapper()
            .readTree(
                "endpoints:\n  /welcome:\n    artifact: \n    - payload.js\n    - payload2.js\n    function: welcome\n"));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(5, artifacts.size());
  }

  @Test
  public void noDeploymentSpecWarns() {
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(3, artifacts.size());
  }

  @Test
  public void artifactArrayLocationsShouldNotHaveDuplicateEntries() throws IOException {
    kow.addDeployment(
        new YAMLMapper()
            .readTree(
                "/welcome:\n    post: \n        artifact: \n        - payload.js\n        - payload2.js\n        function: welcome\n/welcome-plain:\n    post: \n        artifact: \n        - payload.js\n        - payload2.js\n        function: welcome\n"));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(5, artifacts.size());
  }
}
