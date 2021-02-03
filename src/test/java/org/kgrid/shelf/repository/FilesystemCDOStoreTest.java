package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Filesystem CDO Store Tests")
public class FilesystemCDOStoreTest {

  private CompoundDigitalObjectStore koStore;
  private ArkId arkId;
  private final URI helloDirName = URI.create("hello-world-v0.1.0/");

  @TempDir public File tempShelf;

  @BeforeEach
  public void setUp() throws Exception {
    FileUtils.copyDirectory(new File("src/test/resources/shelf"), tempShelf);
    String connectionURL = "filesystem:" + tempShelf.toURI();
    arkId = new ArkId("hello", "world", "v0.1.0");
    koStore = new FilesystemCDOStore(connectionURL);
  }

  @Test
  @DisplayName("Can create new shelf with full url")
  public void createShelfInRelativeLocation() {
    String connectionUrl = "filesystem:file://shelf";
    koStore = new FilesystemCDOStore(connectionUrl);
    assertTrue(Files.isDirectory(new File("shelf").toPath()));
  }

  @Test
  @DisplayName("Shelf has all kos")
  public void shelfHasKosAndKnowsHowToAccessThem() {
    URI myFirstKo = URI.create("My%20First%20Ko/");
    URI helloWorld1 = URI.create("hello-world-v0.1.0/");
    URI helloWorld2 = URI.create("hello-world-v0.2.0/");
    URI helloWorld3 = URI.create("hello-world-v0.3.0/");
    final List<URI> children = koStore.getChildren();
    assertAll(
        () -> assertTrue(children.contains(myFirstKo)),
        () -> assertTrue(children.contains(helloWorld1)),
        () -> assertTrue(children.contains(helloWorld2)),
        () -> assertTrue(children.contains(helloWorld3)),
        () -> assertNotNull(koStore.getMetadata(URI.create(arkId.getFullDashArk()))),
        () ->
            assertEquals(
                "Hello World Title",
                koStore.getMetadata(helloDirName).findValue("title").textValue()),
        () -> assertEquals(tempShelf.toURI(), koStore.getAbsoluteLocation(null)),
        () ->
            assertEquals(
                Paths.get(tempShelf.getPath(), helloDirName.toString()).toUri(),
                koStore.getAbsoluteLocation(helloDirName)),
        () -> assertTrue(koStore.getBinarySize(helloDirName.resolve("metadata.json")) > 100),
        () ->
            assertThrows(
                ShelfResourceNotFound.class, () -> koStore.getMetadata(URI.create("asdasdkas"))),
        () ->
            assertThrows(
                ShelfResourceNotFound.class, () -> koStore.getMetadata(URI.create("hello-xxxxxx"))),
        () ->
            assertThrows(
                ShelfResourceNotFound.class, () -> koStore.getBinary(URI.create("hello-xxxxxx"))));
  }

  @Test
  @DisplayName("Get binary returns correct binary")
  public void getBinaryReturnsCorrectBinary() throws Exception {
    String code =
        String.format(
            "function welcome(inputs) {%n"
                + "    var name = inputs.name;%n"
                + "    return \"Welcome to Knowledge Grid, \" + name;%n"
                + "}");
    assertEquals(
        code, new String(koStore.getBinary(helloDirName.resolve("src/").resolve("index.js"))));
  }

  @Test
  @DisplayName("Get binary stream returns correct stream data")
  public void getBinaryStreamReturnsCorrectStream() throws IOException {
    String code =
        String.format(
            "function welcome(inputs) {%n"
                + "    var name = inputs.name;%n"
                + "    return \"Welcome to Knowledge Grid, \" + name;%n"
                + "}");
    Writer writer = new StringWriter();
    InputStream stream = koStore.getBinaryStream(helloDirName.resolve("src/").resolve("index.js"));
    IOUtils.copy(stream, writer, Charset.defaultCharset());
    stream.close();
    assertEquals(code, writer.toString());
  }

  @Test
  @DisplayName("Create metadata creates metadata file")
  public void createMetadataCreatesFileWithMetadata() throws IOException {
    String metadataContent = String.format("{%n  \"@id\" : \"ark:/naan/name\"%n}");
    JsonNode metadata = new ObjectMapper().readTree(metadataContent);
    koStore.saveMetadata(metadata, helloDirName.resolve("metadata2.json"));
    Path metadataPath = Paths.get(tempShelf.getPath(), helloDirName.toString(), "metadata2.json");
    assertEquals(metadata, new ObjectMapper().readTree(Files.readAllBytes(metadataPath)));
  }

  @Test
  @DisplayName("Create metadata given dir creates metadata.json file in dir")
  public void createMetadataInDirCreatesFileWithMetadata() throws IOException {
    String metadataContent = String.format("{%n  \"@id\" : \"ark:/naan/name\"%n}");
    JsonNode metadata = new ObjectMapper().readTree(metadataContent);
    koStore.saveMetadata(metadata, helloDirName);
    Path metadataPath = Paths.get(tempShelf.getPath(), helloDirName.toString(), "metadata.json");
    assertEquals(metadata, new ObjectMapper().readTree(Files.readAllBytes(metadataPath)));
  }

  @Test
  @DisplayName("Bad create metadata throws exception")
  public void cantCreateMetadataThrowsEx() throws IOException {
    String metadataContent = "{\n  \"@id\" : \"ark:/naan/name\"\n}";
    JsonNode metadata = new ObjectMapper().readTree(metadataContent);
    assertThrows(
        ShelfException.class,
        () -> koStore.saveMetadata(metadata, URI.create("NOTHERE/").resolve("metadata2.json")));
  }

  @Test
  @DisplayName("Create binary creates binary file")
  public void createBinaryCreatesFileWithBinary() throws IOException {
    String binaryContent = "blah";
    byte[] bytes = binaryContent.getBytes();
    koStore.saveBinary(bytes, helloDirName.resolve("binary.txt"));
    Path binaryPath = Paths.get(tempShelf.getPath(), helloDirName.toString(), "binary.txt");
    assertArrayEquals(bytes, Files.readAllBytes(binaryPath));
  }

  @Test
  @DisplayName("Create binary with input stream creates binary file")
  public void createBinaryCreatesFileWithInputStream() throws IOException {
    String binaryContent = "blah";
    InputStream inputStream = new ByteArrayInputStream(binaryContent.getBytes());
    koStore.saveBinary(inputStream, helloDirName.resolve("binary.txt"));
    Path binaryPath = Paths.get(tempShelf.getPath(), helloDirName.toString(), "binary.txt");
    assertArrayEquals(binaryContent.getBytes(), Files.readAllBytes(binaryPath));
  }

  @Test
  @DisplayName("Create container creates new ko directory")
  public void createContainerCreatesDirectory() {
    koStore.createContainer(URI.create("container"));
    Path containerPath = Paths.get(tempShelf.getPath(), "container");
    assertTrue(Files.isDirectory(containerPath));
  }

  @Test
  @DisplayName("Delete ko removes directory")
  public void deleteRemovesDirectory() throws JsonProcessingException {
    koStore.createContainer(URI.create("new-ko"));
    Path binaryPath = Paths.get(tempShelf.getPath(), "new-ko");
    assertTrue(Files.exists(binaryPath));
    koStore.delete(URI.create("new-ko"));
    assertFalse(Files.exists(binaryPath));
  }

  @Test
  @DisplayName("Create transaction creates new temp dir")
  public void createTransactionCreatesTempDir() throws IOException {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(tempShelf.getPath(), dirID);
    assertTrue(Files.exists(idPath));
  }

  @Test
  @DisplayName("Commit transaction moves ko files to final location")
  public void commitTransactionCopiesDirToFinalLocation() throws IOException {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(tempShelf.getPath(), dirID);
    Files.createFile(idPath.resolve("file.txt"));
    koStore.commitTransaction(dirID);
    assertTrue(Files.exists(Paths.get(tempShelf.getPath(), "file.txt")));
  }

  @Test
  @DisplayName("Rollback transaction deletes temp dir")
  public void rollbackTransactionDeletesTempDir() throws IOException {
    String dirID = koStore.createTransaction();
    Path idPath = Paths.get(tempShelf.getPath(), dirID);
    Files.createFile(idPath.resolve("file.txt"));
    koStore.rollbackTransaction(dirID);
    assertFalse(Files.exists(idPath.resolve("file.txt")));
  }

  @Test
  @DisplayName("New cdo store escapes spaces in connection uri")
  public void cdoStoreReplacesSpacesInConnectionUri() {
    String connectionURL = "filesystem:file:///src/test/resources/shelf with spaces";
    koStore = new FilesystemCDOStore(connectionURL);
    String location = koStore.getAbsoluteLocation(null).toString();
    assertTrue(location.contains("shelf%20with%20spaces"));
  }

}
