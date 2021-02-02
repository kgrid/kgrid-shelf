package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.io.Files;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.kgrid.shelf.TestHelper.DEPLOYMENT_BYTES;
import static org.kgrid.shelf.TestHelper.packZipForImport;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Import service tests")
public class ImportServiceTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock KnowledgeObjectRepository koRepo;
  @Mock CompoundDigitalObjectStore cdoStore;
  @InjectMocks ImportService importService;
  URI resourceUri;

  @Test
  @DisplayName("Import zip takes a uri and extracts and saves artifacts")
  public void importZip_givenUri_canExtractAndSaveArtifacts() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    importService.importZip(resourceUri);
    assertAll(
        () -> verify(applicationContext).getResource(anyString()),
        () -> verify(koRepo).addKnowledgeObjectToLocationMap(any(URI.class), any(JsonNode.class)),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/service.yaml"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/deployment.yaml"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/dist/main.js"))));
  }

  @Test
  @DisplayName("Import zip takes a uri and extracts and saves artifacts from array")
  public void importZip_givenUri_canExtractAndSaveMultipleArtifacts() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");

    importService.importZip(resourceUri);
    assertAll(
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/service.yaml"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/deployment.yaml"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/dist/main.js"))),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/src/index.js"))));
  }

  @Test
  @DisplayName("Import zip takes a uri with no deployment throws and skips")
  public void importZip_givenUri_noDeploymentLogsThrowsAndSkips() {
    resourceUri =
        URI.create("file:src/test/resources/fixtures/import-export/missing-deployment.zip");

    Throwable cause =
        assertThrows(ImportExportException.class, () -> importService.importZip(resourceUri))
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  @DisplayName("Import zip takes a uri with bad metadata throws and skips")
  public void importZip_givenUri_badMetadataLogsThrowsAndSkips() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");

    Throwable cause =
        assertThrows(ImportExportException.class, () -> importService.importZip(resourceUri))
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  @DisplayName("Import zip takes a uri with no metadata throws and skips")
  public void importZip_givenResource_noMetadataLogsThrowsAndSkips() throws IOException {

    ByteArrayInputStream funnyZipStream = packZipForImport(null, DEPLOYMENT_BYTES, null, null);

    final File test = Files.createTempDir();
    File zipTest = new File(test, "naan-name-version.zip");
    Files.write(funnyZipStream.readAllBytes(), zipTest);
    FileUtils.forceDeleteOnExit(test);

    final FileSystemResource zipResource = new FileSystemResource(zipTest);

    Throwable cause =
        assertThrows(ImportExportException.class, () -> importService.importZip(zipResource))
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  @DisplayName("Import loads hello world object")
  public void importZip_givenUri_canLoadHelloWorld() {
    resourceUri = URI.create("file:src/test/resources/static/hello-world-v1.3.zip");

    importService.importZip(resourceUri);
    assertAll(
        () -> verify(cdoStore, times(4)).saveBinary(any(InputStream.class), any()),
        () ->
            verify(cdoStore)
                .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json"))));
  }
}
