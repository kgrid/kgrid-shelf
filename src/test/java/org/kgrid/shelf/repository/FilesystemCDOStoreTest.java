package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
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
  public void cleanUp() throws IOException {
    Path shelf = Paths.get(koStore.getAbsoluteLocation(null));
    if (Files.isDirectory(shelf)) {
      nukeTestShelf(shelf);
    }
  }

  @Test
  public void createShelfInRelativeLocation() {
    String connectionURL = "filesystem:file://shelf";
    koStore = new FilesystemCDOStore(connectionURL);
    assertTrue(Files.isDirectory(new File("shelf").toPath()));
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
  public void cannotGetChildrenOfBadPath() {
    koStore.getChildren("asdasdkas");
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
    assertEquals(folder.getRoot().toURI(), koStore.getAbsoluteLocation(""));
  }

  @Test
  public void getAbsoluteLocationReturnsObject() {
    assertEquals(
        Paths.get(folder.getRoot().getPath(), helloDirName).toUri(),
        koStore.getAbsoluteLocation(helloDirName));
  }

  @Test
  public void getBinaryReturnsCorrectBinary() {
    String code =
        String.format(
            "function welcome(inputs) {%n"
                + "    var name = inputs.name;%n"
                + "    return \"Welcome to Knowledge Grid, \" + name;%n"
                + "}");
    assertEquals(code, new String(koStore.getBinary(helloDirName, "src", "index.js")));
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void getMissingBinaryThrowsError() {
    byte[] koNode = koStore.getBinary("hello-xxxxxx");
  }

  @Test
  public void createMetadataCreatesFileWithMetadata() throws IOException {
    String metadataContent = String.format("{%n  \"@id\" : \"ark:/naan/name\"%n}");
    JsonNode metadata = new ObjectMapper().readTree(metadataContent);
    koStore.saveMetadata(metadata, helloDirName, "metadata2.json");
    Path metadataPath = Paths.get(folder.getRoot().getPath(), helloDirName, "metadata2.json");
    assertEquals(metadata, new ObjectMapper().readTree(Files.readAllBytes(metadataPath)));
  }

  @Test(expected = ShelfException.class)
  public void cantCreateMetadataThrowsEx() throws IOException {
    String metadataContent = "{\n  \"@id\" : \"ark:/naan/name\"\n}";
    JsonNode metadata = new ObjectMapper().readTree(metadataContent);
    koStore.saveMetadata(metadata, "NOTHERE", "metadata2.json");
  }

  @Test
  public void createBinaryCreatesFileWithBinary() throws IOException {
    String binaryContent = "blah";
    byte[] bytes = binaryContent.getBytes();
    koStore.saveBinary(bytes, helloDirName, "binary.txt");
    Path binaryPath = Paths.get(folder.getRoot().getPath(), helloDirName, "binary.txt");
    assertArrayEquals(bytes, Files.readAllBytes(binaryPath));
  }

  @Test(expected = ShelfException.class)
  public void cantCreateBinaryThrowsEx() throws IOException {
    String binaryContent = "blah";
    byte[] bytes = binaryContent.getBytes();
    String filename = "test.txt";
    koStore.saveBinary(bytes, filename);
    Path binaryPath = Paths.get(folder.getRoot().getPath(), filename);
    binaryPath.toFile().setWritable(false);
    koStore.saveBinary(bytes, filename);
    assertArrayEquals(bytes, Files.readAllBytes(binaryPath));
  }

  @Test
  public void createContainerCreatesDirectory() {
    koStore.createContainer("container");
    Path containerPath = Paths.get(folder.getRoot().getPath(), "container");
    assertTrue(Files.isDirectory(containerPath));
  }

  @Test
  public void deleteRemovesDirectory() throws JsonProcessingException {
    koStore.createContainer("new-ko");
    Path binaryPath = Paths.get(folder.getRoot().getPath(), "new-ko");
    assertTrue(Files.exists(binaryPath));
    koStore.delete("new-ko");
    assertFalse(Files.exists(binaryPath));
  }

  @Test
  public void createTransactionCreatesTempDir() {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(folder.getRoot().getPath(), dirID);
    assertTrue(Files.exists(idPath));
  }

  @Test
  public void commitTransactionCopiesDirToFinalLocation() throws IOException {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(folder.getRoot().getPath(), dirID);
    Files.createFile(idPath.resolve("file.txt"));
    koStore.commitTransaction(dirID);
    assertTrue(Files.exists(Paths.get(folder.getRoot().getPath(), "file.txt")));
  }

  @Test
  public void rollbackTransactionDeletesTempDir() throws IOException {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(folder.getRoot().getPath(), dirID);
    Files.createFile(idPath.resolve("file.txt"));
    koStore.rollbackTransaction(dirID);
    assertFalse(Files.exists(idPath.resolve("file.txt")));
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
