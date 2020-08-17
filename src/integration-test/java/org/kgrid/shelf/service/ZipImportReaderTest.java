package org.kgrid.shelf.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.kgrid.shelf.domain.KoFields;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.ZipImportExportTestHelper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

class ZipImportReaderTest {

  @Spy
  ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock
  CompoundDigitalObjectStore cdoStore;

  @InjectMocks
  ImportService importService;

  URI resourceUri;
  Resource zippedKo;
  File tempDir;
  File tempKo;
  JsonNode metadata;

  @Before
  public void setUp() throws IOException {
    //    applicationContext = new ClassPathXmlApplicationContext();
    // /[kgrid-shelf]/src/test/resources/fixtures/import-export/kozip.zip
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");
    zippedKo = applicationContext.getResource(resourceUri.toString());
    tempDir = Files.createTempDir();
    tempKo = new File(tempDir, "mycoolko");
    ZipUtil.unpack(zippedKo.getInputStream(), tempDir);
    FileUtils.forceDeleteOnExit(tempDir);
    metadata =
        ZipImportExportTestHelper.generateMetadata(
            ZipImportExportTestHelper.SERVICE_YAML_PATH,
            ZipImportExportTestHelper.DEPLOYMENT_YAML_PATH,
            true,
            true,
            true,
            true);
  }


//  @Test
  public void serviceSpecCanBeExtractedToJsonNode() throws IOException {
    File tempKo = new File(tempDir, "mycoolko");
    Map<KoFields, URI> metadataURIs = importService.getKoParts(metadata);
//    JsonNode serviceSpec =
//        importService.getSpecification(tempKo, metadataURIs.get(KoFields.SERVICE_SPEC_TERM));
//
//    assertTrue(serviceSpec.has("paths"));
  }

//  @Test
  public void canGetListOfArtifactLocations() throws IOException {
//    JsonNode deploymentSpec = importService.getSpecification(tempKo, URI.create("deployment.yaml"));
//    JsonNode serviceSpec = importService.getSpecification(tempKo, URI.create("service.yaml"));
//    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

//    assertEquals("dist/main.js", artifactLocations.get(0).toString());
  }

//  @Test
  public void canGetListOfArtifactLocationsFromArray() throws IOException {

    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");
    zippedKo = applicationContext.getResource(resourceUri.toString());
    File multiartifactDir = Files.createTempDir();
    ZipUtil.unpack(zippedKo.getInputStream(), multiartifactDir);
    tempKo = new File(multiartifactDir, "artifact-array");
    FileUtils.forceDeleteOnExit(multiartifactDir);

//    JsonNode deploymentSpec = importService.getSpecification(tempKo, URI.create("deployment.yaml"));
//    JsonNode serviceSpec = importService.getSpecification(tempKo, URI.create("service.yaml"));
//    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

//    assertTrue(artifactLocations.contains(URI.create("dist/main.js")));
//    assertTrue(artifactLocations.contains(URI.create("src/index.js")));
  }


}
