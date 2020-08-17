package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.KoFields;
import org.kgrid.shelf.service.ImportExportException;
import org.kgrid.shelf.service.ImportService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
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

  @Test
  public void metadataCanBeExtractedToJsonNode() {

    JsonNode metadata =
        ZipImportExportTestHelper.generateMetadata(
            ZipImportExportTestHelper.SERVICE_YAML_PATH,
            ZipImportExportTestHelper.DEPLOYMENT_YAML_PATH,
            true,
            true,
            true,
            true);
    Map<KoFields, URI> metadataURIs = importService.getKoParts(metadata);

    assertEquals(3, metadataURIs.size());
    assertTrue(metadataURIs.containsValue(URI.create("metadata.json")));
  }


  @Test
  public void canExtractAndSaveArtifacts() throws IOException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

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
  public void noDeploymentLogsThrowsAndSkips() {
    resourceUri =
        URI.create("file:src/test/resources/fixtures/import-export/missing-deployment.zip");

    Throwable cause =
        assertThrows(
                ImportExportException.class,
                () -> {
                  importService.importZip(resourceUri);
                })
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  public void badMetadataLogsThrowsAndSkips() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");

    Throwable cause =
        assertThrows(
                ImportExportException.class,
                () -> {
                  importService.importZip(resourceUri);
                })
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  public void noMetadataLogsThrowsAndSkips() throws IOException {

    ByteArrayInputStream funnyZipStream =
        ZipImportExportTestHelper.packZipForImport(
            null, ZipImportExportTestHelper.DEPLOYMENT_BYTES, null, null);

    final File test = Files.createTempDir();
    File zipTest = new File(test, "naan-name-version.zip");
    Files.write(funnyZipStream.readAllBytes(), zipTest);
    FileUtils.forceDeleteOnExit(test);

    final FileSystemResource zipResource = new FileSystemResource(zipTest);

    Throwable cause =
        assertThrows(
                ImportExportException.class,
                () -> {
                  importService.importZip(zipResource);
                })
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  public void canLoadHelloWorld() throws IOException {
    resourceUri = URI.create("file:src/test/resources/static/hello-world-v1.3.zip");

    URI uri = importService.importZip(resourceUri);

    verify(cdoStore, times(4)).saveBinary(any(), any());
    verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
  }
}
