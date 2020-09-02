package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.service.ImportService;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.*;

@Category(FedoraIntegrationTest.class)
public class FedoraZipImportServiceTest {

  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";

  @ClassRule public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ImportService service = new ImportService();

  FedoraCDOStore compoundDigitalObjectStore =
      new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  @Test
  public void testImportKnowledgeObject() {

    URI helloWorldLoc =
        URI.create("file:src/test/resources/fixtures/import-export/hello-world.zip");
    service.importZip(helloWorldLoc);

    final ArkId arkId = new ArkId("hello", "world");
    ObjectNode metadata = compoundDigitalObjectStore.getMetadata(URI.create(arkId.getDashArk()));

    assertNotNull(metadata);

    assertEquals("should have 2 versions", 3, metadata.findValue(IMPLEMENTATIONS_TERM).size());

    metadata =
        compoundDigitalObjectStore.getMetadata(URI.create(arkId.getDashArk() + "/" + "v0.1.0"));

    assertEquals(
        "should have ",
        "http://localhost:8080/fcrepo/rest/hello-world/v0.1.0/service.yaml",
        metadata.findValue(SERVICE_SPEC_TERM).asText());

    compoundDigitalObjectStore.delete(URI.create("hello-world"));
  }

  @Test
  public void testImportKnowledgeObjectFolder() {

    URI mycoolkoLoc = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

    try {
      service.importZip(mycoolkoLoc);
      final ArkId arkId = new ArkId("hello", "folder");
      ObjectNode metadata =
          compoundDigitalObjectStore.getMetadata(URI.create(arkId.getDashArk() + "/" + "v0.1.0"));
      assertEquals(
          "should have ",
          "http://localhost:8080/fcrepo/rest/hello-folder/v0.1.0/service-specification.yaml",
          metadata.findValue(SERVICE_SPEC_TERM).asText());

    } catch (ShelfException e) {
      assertTrue(
          "Should not be able to import an object with a folder name that does not match the ark id",
          true);
    }

    compoundDigitalObjectStore.delete(URI.create("hello-folder"));
  }

  @Test
  public void testBadKOMetaData() throws IOException {

    URI badKoMetadataLoc =
        URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");
    try {

      service.importZip(badKoMetadataLoc);

      fail("should throw exception");

    } catch (ShelfException se) {

      try {
        compoundDigitalObjectStore.getMetadata(
            URI.create((new ArkId("hello", "world").getDashArk())));
        fail("Should throw exception");
      } catch (ShelfException e) {
        assertTrue(
            "Should not find  hello world because not defined in meatadata, not found will throw exception",
            true);
      }
    }
  }
}
