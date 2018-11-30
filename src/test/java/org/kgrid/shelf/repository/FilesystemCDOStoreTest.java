package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;

public class FilesystemCDOStoreTest {

  private CompoundDigitalObjectStore koStore;
  private ZipImportService zis;
  private ArkId arkId;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();


  @Before
  public void setUp() throws Exception {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    koStore = new FilesystemCDOStore(connectionURL);
    zis = new ZipImportService();
    Path shelf = Paths.get(koStore.getAbsoluteLocation(""));
    if(Files.isDirectory(shelf)) {
      nukeTestShelf(shelf);
    }
    Files.createDirectory(shelf);
    arkId = new ArkId("hello", "world");
    // Add zip file to our test shelf:
    InputStream zipStream = FilesystemCDOStoreTest.class.getResourceAsStream("/fixtures/hello-world-jsonld.zip");
    zis.importCompoundDigitalObject(arkId, zipStream, koStore);
  }

  @Test
  public void testURIPathWindows(){
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    koStore = new FilesystemCDOStore(connectionURL);
    koStore.getAbsoluteLocation("");
  }
  @After
  public void deleteKO() throws Exception {
    koStore.removeFile(this.arkId.getDashArk());
    Path shelf = Paths.get(koStore.getAbsoluteLocation(null));
    if(Files.isDirectory(shelf)) {
      nukeTestShelf(shelf);
    }
  }

  private void nukeTestShelf(Path shelf) throws IOException {
    Files.walk(shelf)
        .sorted(Comparator.reverseOrder()) // Need to reverse the order to delete files before the directory they're in
        .forEach(file -> {
          try {
            Files.delete(file);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }

  @Test
  public void getObjectsOnShelf() throws Exception {
    List<ArkId> shelfIds = koStore.getChildren("").stream().map(name -> {try {return new ArkId(StringUtils.substringAfterLast(name, FileSystems.getDefault().getSeparator()));} catch (IllegalArgumentException e) {e.printStackTrace(); return null;}}).filter(
        Objects::nonNull).collect(Collectors.toList());
    List<ArkId> expectedIds = Collections.singletonList(arkId);

    assertEquals(expectedIds, shelfIds);
  }

  @Test
  public void getImplementations() throws Exception {
    List<String> expectedImplementations = new ArrayList<>();
    expectedImplementations.add("v0.0.1");
    expectedImplementations.add("v0.0.2");
    List<String> implementations = koStore.getChildren(arkId.getDashArk()).stream().map(child -> StringUtils
        .substringAfterLast(child, FileSystems.getDefault().getSeparator())).collect(Collectors.toList());
    implementations.sort(Comparator.naturalOrder());
    assertEquals(expectedImplementations, implementations);
  }

  @Test
  public void getImplementationMetadata() throws Exception {

    ObjectNode metadata = koStore.getMetadata(arkId.getDashArk(), "v0.0.1");
    assertEquals("Implementation 0.0.1 of Hello World", metadata.get("title").asText());
    metadata.replace("title", new TextNode("TEST"));
    koStore.saveMetadata(metadata, arkId.getDashArk(), "v0.0.1");
    metadata = koStore.getMetadata(arkId.getDashArk(), "v0.0.1");
    assertEquals("TEST", metadata.get("title").asText());
  }


  // TODO: Redo this test PLZKTHX
  @Test
  public void getResource() throws Exception {

      JsonNode metadata = koStore.getMetadata(arkId.getDashArk(), "v0.0.1");
//    ko.setModelMetadata((ObjectNode)modelMetadata);
//    Path resourceLocation = ko.resourceLocation();
//    byte[] resource = koStore.getBinary(resourceLocation);
//    assertEquals("function content(riskValues) {", new String(resource, Charset.defaultCharset()).substring(0, 30));
    String data =  "test data for broken payload";
    byte[] dataArray = data.getBytes();
//    koStore.saveBinary(resourceLocation, dataArray);

//    resource = koStore.getBinary(resourceLocation);

//    assertEquals(data, new String(resource, Charset.defaultCharset()));
  }


}