package org.kgrid.shelf.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.jena.ext.com.google.common.io.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.kgrid.shelf.TestHelper.DEPLOYMENT_BYTES;
import static org.kgrid.shelf.TestHelper.packZipForImport;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
  @Mock CompoundDigitalObjectStore cdoStore;
  @Mock KnowledgeObjectRepository koRepo;
  @InjectMocks ImportService importService;
  URI resourceUri;

  @Test
  public void importZip_givenUri_canExtractAndSaveArtifacts() throws JsonProcessingException {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    importService.importZip(resourceUri);
    JsonNode metadata =
        new ObjectMapper()
            .readTree(
                "{\n"
                    + "  \"@id\" : \"hello-world\",\n"
                    + "  \"@type\" : \"koio:KnowledgeObject\",\n"
                    + "  \"identifier\" : \"ark:/hello/world\",\n"
                    + "  \"title\" : \"Hello World Title\",\n"
                    + "  \"contributors\" : \"Kgrid Team\",\n"
                    + "  \"version\":\"v3\",\n"
                    + "  \"description\" : \"Test Hello World \",\n"
                    + "  \"keywords\" : \"test hello world\",\n"
                    + "  \"hasServiceSpecification\" : \"service.yaml\",\n"
                    + "  \"hasDeploymentSpecification\" : \"deployment.yaml\",\n"
                    + "  \"hasPayload\" : \"dist/main.js\",\n"
                    + "  \"@context\" : [ \"http://kgrid.org/koio/contexts/knowledgeobject.jsonld\" ]\n"
                    + "}\n");

    verify(cdoStore)
        .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json")));
    verify(cdoStore).saveBinary(any(InputStream.class), eq(URI.create("hello-world/service.yaml")));
    verify(cdoStore)
        .saveBinary(any(InputStream.class), eq(URI.create("hello-world/deployment.yaml")));
    verify(cdoStore).saveBinary(any(InputStream.class), eq(URI.create("hello-world/dist/main.js")));
  }

  @Test
  public void importZip_givenUri_canExtractAndSaveMultipleArtifacts() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");

    importService.importZip(resourceUri);

    verify(cdoStore)
        .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json")));
    verify(cdoStore).saveBinary(any(InputStream.class), eq(URI.create("hello-world/service.yaml")));
    verify(cdoStore)
        .saveBinary(any(InputStream.class), eq(URI.create("hello-world/deployment.yaml")));
    verify(cdoStore).saveBinary(any(InputStream.class), eq(URI.create("hello-world/dist/main.js")));
    verify(cdoStore).saveBinary(any(InputStream.class), eq(URI.create("hello-world/src/index.js")));
  }

  @Test
  public void importZip_givenUri_noDeploymentLogsThrowsAndSkips() {
    resourceUri =
        URI.create("file:src/test/resources/fixtures/import-export/missing-deployment.zip");

    Throwable cause =
        assertThrows(ImportExportException.class, () -> importService.importZip(resourceUri))
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
  public void importZip_givenUri_badMetadataLogsThrowsAndSkips() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");

    Throwable cause =
        assertThrows(ImportExportException.class, () -> importService.importZip(resourceUri))
            .getCause();

    assertEquals(FileNotFoundException.class, cause.getClass());
  }

  @Test
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
  public void importZip_givenUri_canLoadHelloWorld() {
    resourceUri = URI.create("file:src/test/resources/static/hello-world-v1.3.zip");

    importService.importZip(resourceUri);

    verify(cdoStore, times(4)).saveBinary(any(InputStream.class), any());
    verify(cdoStore)
        .saveBinary(any(InputStream.class), eq(URI.create("hello-world/metadata.json")));
  }
}
