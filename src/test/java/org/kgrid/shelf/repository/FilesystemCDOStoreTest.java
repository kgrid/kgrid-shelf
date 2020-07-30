package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.junit.Assert.*;

public class FilesystemCDOStoreTest {

  private CompoundDigitalObjectStore koStore;
  private ArkId arkId;
  private String helloDirName = "hello-world-v0.1.0";

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    FileUtils.copyDirectory(
        new File("src/test/resources/shelf"), new File(folder.getRoot().getPath()));
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    arkId = new ArkId("hello", "world", "v0.1.0");
    koStore = new FilesystemCDOStore(connectionURL);
  }

  @After
  public void cleanUp() throws Exception {
    Path shelf = Paths.get(koStore.getAbsoluteLocation(null));
    if (Files.isDirectory(shelf)) {
      nukeTestShelf(shelf);
    }
  }

  @Test
  public void shelfHas4Objects() {
    assertTrue(koStore.getChildren("").size() == 4);
  }

  @Test
  public void shelfHasHelloWorldObject() {
    assertNotNull(koStore.getChildren((arkId.getDashArk() + "-" + arkId.getVersion())));
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void testMetaDataNotFound() {
    ObjectNode koNode = koStore.getMetadata("hello-xxxxxx");
    assertEquals("Hello  World Title", koNode.findValue("title").textValue());
  }

  @Test
  public void testGetMetadata() {
    ObjectNode koNode = koStore.getMetadata(helloDirName);
    assertEquals("Hello World Title", koNode.findValue("title").textValue());
  }

  @Test
  public void getAbsoluteLocationReturnsCorrectShelf() {
    assertEquals(folder.getRoot().getAbsolutePath(), koStore.getAbsoluteLocation(""));
  }

  @Test
  public void getAbsoluteLocationReturnsObject() {
    assertEquals(
        Paths.get(folder.getRoot().getPath(), helloDirName).toString(),
        koStore.getAbsoluteLocation(helloDirName));
  }

  @Test
  public void getBinaryReturnsCorrectBinary() {
    String code =
        "function welcome(inputs) {\r\n"
            + "    var name = inputs.name;\r\n"
            + "    return \"Welcome to Knowledge Grid, \" + name;\r\n"
            + "}";
    assertEquals(code, new String(koStore.getBinary(helloDirName, "src", "index.js")));
  }

  private void nukeTestShelf(Path shelf) throws IOException {
    Files.walk(shelf)
        .sorted(
            Comparator
                .reverseOrder()) // Need to reverse the order to delete files before the directory
        // they're in
        .forEach(
            file -> {
              try {
                Files.delete(file);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
  }
}
