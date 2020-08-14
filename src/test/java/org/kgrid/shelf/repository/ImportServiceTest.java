package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipFile;
import org.apache.jena.rdfxml.xmlinput.AResource;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.zeroturnaround.zip.ZipUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock CompoundDigitalObjectStore cdoStore;

  @InjectMocks
  ImportService importService;

  URI resourceUri;
  Resource zippedKo;

  @Before
  public void setUp() {
//    applicationContext = new ClassPathXmlApplicationContext();
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
    URI zipBase = importService.getZipBase(metadataURIs.get(KoFields.METADATA_FILENAME));
    JsonNode serviceSpec =
        importService.getSpecification(
            metadataURIs.get(KoFields.SERVICE_SPEC_TERM), zippedKo.getInputStream(), zipBase);

    assertTrue(serviceSpec.has("paths"));
  }

  @Test
  public void canGetListOfArtifactLocations() throws IOException {

    URI zipBase = URI.create("mycoolko");
    JsonNode deploymentSpec =
        importService.getSpecification(
            URI.create("mycoolko/deployment.yaml"), zippedKo.getInputStream(), zipBase);
    JsonNode serviceSpec =
        importService.getSpecification(
            URI.create("mycoolko/service.yaml"), zippedKo.getInputStream(), zipBase);
    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

    assertEquals("dist/main.js", artifactLocations.get(0).toString());
  }

  @Test
  public void canGetListOfArtifactLocationsFromArray() throws IOException {

    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");
    zippedKo = applicationContext.getResource(resourceUri.toString());
    URI zipBase = URI.create("mycoolko");

    JsonNode deploymentSpec =
        importService.getSpecification(
            URI.create("mycoolko/deployment.yaml"), zippedKo.getInputStream(), zipBase);
    JsonNode serviceSpec =
        importService.getSpecification(
            URI.create("mycoolko/service.yaml"), zippedKo.getInputStream(), zipBase);
    List<URI> artifactLocations = importService.getArtifactLocations(deploymentSpec, serviceSpec);

    assertTrue(artifactLocations.contains(URI.create("dist/main.js")));
    assertTrue(artifactLocations.contains(URI.create("src/index.js")));
  }


  @Test
  public void canExtractAndSaveArtifacts() throws IOException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/kozip.zip");

    importService.importZip(resourceUri);

    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/service.yaml"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/deployment.yaml"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/dist/main.js"));
  }
  @Test
  public void canExtractAndSaveMultipleArtifacts() throws IOException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");

    importService.importZip(resourceUri);

    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/service.yaml"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/deployment.yaml"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/dist/main.js"));
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/src/index.js"));
  }

  @Test
  public void noDeploymentLogsThrowsAndSkips() throws IOException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/missing-deployment.zip");

    Throwable cause = assertThrows(ImportExportException.class, () -> {
      importService.importZip(resourceUri);
    }).getCause();

    assertEquals(IllegalArgumentException.class,cause.getClass());
  }

  @Test
  public void badMetadataLogsThrowsAndSkips() throws IOException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");

    Throwable cause = assertThrows(ImportExportException.class, () -> {
      importService.importZip(resourceUri);
    }).getCause();

    assertEquals(cause.getClass(), NullPointerException.class);
  }

  @Test
  public void noMetadataLogsThrowsAndSkips() throws IOException {

    ByteArrayInputStream funnyZipStream = ZipImportExportTestHelper.packZipForImport(
        null,
        ZipImportExportTestHelper.DEPLOYMENT_BYTES,
        null,
        null
    );

    final File test = File.createTempFile("test", ".zip");
    Files.write(funnyZipStream.readAllBytes(), test);

    final FileSystemResource zipResource = new FileSystemResource(test);

    Throwable cause = assertThrows(ImportExportException.class, () -> {
      importService.importZip(zipResource);
    }).getCause();

    assertEquals(cause.getClass(), NullPointerException.class);
  }

  @Test
  public void canLoadHelloWorld() throws IOException {
    resourceUri = URI.create("file:src/test/resources/static/hello-world-v1.3.zip");

    URI uri = importService.importZip(resourceUri);

    verify(cdoStore, times(4)).saveBinary(any(), any());
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));

  }
}
