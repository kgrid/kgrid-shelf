package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceTest {

  @Spy ApplicationContext applicationContext;
  @Mock CompoundDigitalObjectStore cdoStore;

  @InjectMocks ImportService importService;

  URI resourceUri;
  Resource zippedKo;

  @Before
  public void setUp() {
    applicationContext = new ClassPathXmlApplicationContext();
    // /[kgrid-shelf]/src/test/resources/fixtures/import-export/kozip.zip
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/kozip.zip");
    zippedKo = applicationContext.getResource(resourceUri.toString());
  }

  @Test
  public void metadataCanBeExtractedToJsonNode() throws IOException {

    Map<KoFields, URI> metadataURIs = importService.getMetadataURIs(zippedKo.getInputStream());

    assertEquals(3, metadataURIs.size());
    assertTrue(metadataURIs.containsValue(URI.create("mycoolko/metadata.json")));
  }

  @Test
  public void serviceSpecCanBeExtractedToJsonNode() throws IOException {
    Map<KoFields, URI> metadataURIs = importService.getMetadataURIs(zippedKo.getInputStream());
    JsonNode serviceSpec =
        importService.getSpecification(
            metadataURIs.get(KoFields.SERVICE_SPEC_TERM), zippedKo.getInputStream());

    assertTrue(serviceSpec.has("paths"));
  }

  @Test
  public void canGetListOfArtifactLocations() throws IOException {

    JsonNode deploymentSpec =
        importService.getSpecification(
            URI.create("mycoolko/deployment.yaml"), zippedKo.getInputStream());
    JsonNode serviceSpec =
        importService.getSpecification(
            URI.create("mycoolko/service.yaml"), zippedKo.getInputStream());
    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

    assertEquals("dist/main.js", artifactLocations.get(0).toString());
  }

  @Test
  public void canGetListOfArtifactLocationsFromArray() throws IOException {

    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");
    zippedKo = applicationContext.getResource(resourceUri.toString());

    JsonNode deploymentSpec =
        importService.getSpecification(
            URI.create("mycoolko/deployment.yaml"), zippedKo.getInputStream());
    JsonNode serviceSpec =
        importService.getSpecification(
            URI.create("mycoolko/service.yaml"), zippedKo.getInputStream());
    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

    assertTrue(artifactLocations.contains(URI.create("dist/main.js")));
    assertTrue(artifactLocations.contains(URI.create("src/index.js")));
  }

  @Test
  public void canExtractAndSaveArtifacts() throws IOException {
    List<URI> artifacts = new ArrayList<>();
    final URI metadataURI = URI.create("mycoolko/metadata.json");
    artifacts.add(metadataURI);
    URI identifier = importService.getIdentifier(metadataURI, zippedKo.getInputStream());
    importService.extractAndSaveArtifacts(zippedKo.getInputStream(), artifacts, identifier);
    verify(cdoStore).saveBinary(any(), eq("hello-world/mycoolko/metadata.json"));
  }
}
