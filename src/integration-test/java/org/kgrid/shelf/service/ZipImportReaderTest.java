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
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ZipImportReaderTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();

  ZipImportReader importReader;

  URI resourceUri;
  Resource zippedKo;

  @Before
  public void setUp() throws IOException {
    // /[kgrid-shelf]/src/test/resources/fixtures/import-export/mycoolko.zip
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    zippedKo = applicationContext.getResource(resourceUri.toString());
  }

  @Test
  public void canCreateNewZipImportReader() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    assertTrue(
        importReader.getKoBase().toString().endsWith("mycoolko"), "Ko base ends with mycoolko");
  }

  @Test
  public void canGetJsonMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create(KoFields.METADATA_FILENAME.asStr()));
    assertFalse(metadata.isEmpty(), "Ko reader gets json metadata");
    assertEquals(
        "Metadata in temp folder has deployment spec",
        "deployment.yaml",
        metadata.get(KoFields.DEPLOYMENT_SPEC_TERM.asStr()).asText());
  }

  @Test
  public void canGetYamlMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create("deployment.yaml"));
    assertFalse(metadata.isEmpty(), "Ko reader gets yaml deployment");
    assertEquals(
        "Deployment spec has endpoint",
        "{\"/welcome\"",
        metadata.get(KoFields.ENDPOINTS.asStr()).toString().substring(0, 11));
  }

  @Test
  public void canGetBinary() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    byte[] artifact = importReader.getBinary(URI.create("dist/main.js"));
    assertFalse(artifact.length < 1, "Ko reader gets artifact");
    assertEquals(
        "Artifact in temp folder is js code", "var welcome", new String(artifact).substring(0, 11));
  }

  @Test
  public void throwsErrorOnBadResource() throws IOException {
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
  public void throwsErrorOnMissingResource() throws IOException {
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
