package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.service.ImportService;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

@Category(FedoraIntegrationTest.class)
public class FedoraCDOStoreTest {

  FedoraCDOStore compoundDigitalObjectStore =
      new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  // The Folder will be created before each test method and (recursively) deleted after each test
  // method.
  @ClassRule public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() {

    try {

      compoundDigitalObjectStore = new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");
      ImportService zipImportService = new ImportService();

      // Load Hello-World example object
      URI helloWorldLoc =
          URI.create("file:src/test/resources/fixtures/import-export/hello-world.zip");
      zipImportService.importZip(helloWorldLoc);

      // Load ri-bmicalc example object
      URI bmiLoc = URI.create("file:src/test/resources/fixtures/import-export/ri-bmicalc.zip");
      zipImportService.importZip(bmiLoc);

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }
  }

  @Test
  public void getChildren() throws Exception {

    List<String> koList = compoundDigitalObjectStore.getChildren("");
    assertEquals(2, koList.size());
  }

  @Test
  public void getVersion() throws Exception {
    List<String> impList = compoundDigitalObjectStore.getChildren("hello-world");
    assertEquals(3, impList.size());
  }

  @Test
  public void findKO() throws Exception {

    ObjectNode koNode = compoundDigitalObjectStore.getMetadata("hello-world");

    assertEquals("Hello World Title", koNode.findValue("title").asText());
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void findKONotInThisShelf() throws Exception {

    ObjectNode koNode =
        compoundDigitalObjectStore.getMetadata(
            "http://library.kgrig.org/fcrepo/rest/hello-world/v0.0.2");
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void delete() throws Exception {

    compoundDigitalObjectStore.delete("hello-world");

    ObjectNode koNode = compoundDigitalObjectStore.getMetadata("hello-world");
  }

  @Test(expected = ShelfException.class)
  public void findKONotFound() throws Exception {

    ObjectNode koNode = compoundDigitalObjectStore.getMetadata("not-found");
  }

  @Test
  public void findBinary() {

    assertNotNull(compoundDigitalObjectStore.getBinary("hello-world/v0.1.0/src/index.js"));
  }

  @Test(expected = ShelfException.class)
  public void findBinaryNotFound() {

    compoundDigitalObjectStore.getBinary("hello-world/xxxxx/welcome.js");
  }

  @After
  public void teardown() throws Exception {

    compoundDigitalObjectStore.delete("hello-world");
    compoundDigitalObjectStore.delete("ri-bmicalc");
  }
}
