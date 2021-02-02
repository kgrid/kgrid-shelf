package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.kgrid.shelf.TestHelper.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Knowledge Object Controller Tests")
public class KnowledgeObjectWrapperTest {
  JsonNode metadata;
  KnowledgeObjectWrapper kow;

  @BeforeEach
  public void setUp() {
    metadata = generateMetadata(SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
    kow = new KnowledgeObjectWrapper(metadata);
  }

  @Test
  @DisplayName("Gets return expected ko bits")
  public void wrapperGetsCorrectly() throws IOException {
    JsonNode deployment = new YAMLMapper().readTree(DEPLOYMENT_BYTES);
    JsonNode service = new YAMLMapper().readTree(SERVICE_BYTES);
    kow.addDeployment(deployment);
    kow.addService(service);
    assertAll(
        () -> assertEquals(URI.create(KO_PATH_V1 + "/"), kow.getId()),
        () -> assertEquals(metadata, kow.getMetadata()),
        () -> assertEquals(deployment, kow.getDeployment()),
        () -> assertEquals(service, kow.getService()),
        () -> assertEquals(URI.create(DEPLOYMENT_YAML_PATH), kow.getDeploymentLocation()),
        () -> assertEquals(URI.create(SERVICE_YAML_PATH), kow.getServiceSpecLocation()));
  }

  @Test
  @DisplayName("Extracts artifact locations from metadata")
  public void noDeploymentSpec() {
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(3, artifacts.size());
  }

  @Test
  @DisplayName("Extracts artifact locations from deployment")
  public void artifactLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(new YAMLMapper().readTree(DEPLOYMENT_BYTES));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(4, artifacts.size());
  }

  @Test
  @DisplayName("Extracts artifact locations from array correctly")
  public void artifactArrayLocationsCanBeExtracted() throws IOException {
    kow.addDeployment(
        new YAMLMapper()
            .readTree(
                "endpoints:\n  /welcome:\n    artifact: \n    - payload.js\n    - payload2.js\n    function: welcome\n"));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(5, artifacts.size());
  }

  @Test
  @DisplayName("Handles duplicate artifact entries")
  public void artifactArrayLocationsShouldNotHaveDuplicateEntries() throws IOException {
    kow.addDeployment(
        new YAMLMapper()
            .readTree(
                "/welcome:\n    post: \n        artifact: \n        - payload.js\n        - payload2.js\n"
                    + "        function: welcome\n/welcome-plain:\n    post: \n        artifact: \n"
                    + "        - payload.js\n        - payload2.js\n        function: welcome\n"));
    HashSet<URI> artifacts = kow.getArtifactLocations();
    assertEquals(5, artifacts.size());
  }
}
