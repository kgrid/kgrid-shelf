package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static org.junit.Assert.*;
import static org.kgrid.shelf.TestHelper.DEPLOYMENT_YAML_PATH;

@RunWith(MockitoJUnitRunner.class)
public class ZipImportReaderTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();

  ZipImportReader importReader;

  URI resourceUri;
  Resource zippedKo;

  @Before
  public void setUp() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    zippedKo = applicationContext.getResource(resourceUri.toString());
  }

  @Test
  public void canCreateNewZipImportReader() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    assertTrue(
        "Ko base ends with mycoolko", importReader.getKoBase().toString().endsWith("mycoolko"));
  }

  @Test
  public void canGetJsonMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create(KoFields.METADATA_FILENAME.asStr()));
    assertFalse("Ko reader gets json metadata", metadata.isEmpty());
    assertEquals(
        "Metadata in temp folder has deployment spec",
        DEPLOYMENT_YAML_PATH,
        metadata.get(KoFields.DEPLOYMENT_SPEC_TERM.asStr()).asText());
  }

  @Test
  public void canGetYamlMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create("deployment.yaml"));
    assertFalse("Ko reader gets yaml deployment", metadata.isEmpty());
    assertEquals(
        "Deployment spec has endpoint",
        "{\"/welcome\"",
        metadata.get(KoFields.ENDPOINTS.asStr()).toString().substring(0, 11));
  }

  @Test
  public void canGetFileStream() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    InputStream inputStream = importReader.getFileStream(URI.create("dist/main.js"));
    assertNotNull("Ko reader gets artifact", inputStream);
    assertEquals(
        "Artifact in temp folder is js code",
        "var welcome",
        new String(inputStream.readAllBytes()).substring(0, 11));
  }

  @Test
  public void throwsErrorOnBadResource() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/badZip.zip");

    zippedKo = applicationContext.getResource(resourceUri.toString());
    Throwable cause =
        assertThrows(
                ImportExportException.class,
                () -> {
                  importReader = new ZipImportReader(zippedKo);
                })
            .getCause();

    assertEquals(ZipException.class, cause.getClass());
  }

  @Test
  public void throwsErrorOnMissingResource() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/missingZip.zip");

    zippedKo = applicationContext.getResource(resourceUri.toString());
    assertThrows(
            FileNotFoundException.class,
            () -> {
              importReader = new ZipImportReader(zippedKo);
            })
        .getCause();
  }
}
