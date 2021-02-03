package org.kgrid.shelf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kgrid.shelf.TestHelper.DEPLOYMENT_YAML_PATH;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipException;

@ExtendWith(MockitoExtension.class)
public class ZipImportReaderTest {

  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();

  ZipImportReader importReader;

  URI resourceUri;
  Resource zippedKo;

  @BeforeEach
  public void setUp() {
    resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    zippedKo = applicationContext.getResource(resourceUri.toString());
  }

  @Test
  @DisplayName( "Ko base ends with mycoolko")
  public void canCreateNewZipImportReader() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    assertTrue(importReader.getKoBase().toString().endsWith("mycoolko"));
  }

  @Test
  @DisplayName("Ko reader gets json metadata")
  public void canGetJsonMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create(KoFields.METADATA_FILENAME.asStr()));
    assertFalse(metadata.isEmpty());
    assertEquals(DEPLOYMENT_YAML_PATH,
        metadata.get(KoFields.DEPLOYMENT_SPEC_TERM.asStr()).asText());
  }

  @Test
  @DisplayName("Ko reader gets yaml deployment with endpoint defined")
  public void canGetYamlMetadata() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    JsonNode metadata = importReader.getMetadata(URI.create("deployment.yaml"));
    assertFalse(metadata.isEmpty());
    assertEquals("{\"/welcome\"",
        metadata.get(KoFields.ENDPOINTS.asStr()).toString().substring(0, 11)
    );
  }

  @Test
  @DisplayName("Ko reader gets correct artifact")
  public void canGetFileStream() throws IOException {
    importReader = new ZipImportReader(zippedKo);
    InputStream inputStream = importReader.getFileStream(URI.create("dist/main.js"));
    assertNotNull(inputStream);
    assertEquals("var welcome",
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
