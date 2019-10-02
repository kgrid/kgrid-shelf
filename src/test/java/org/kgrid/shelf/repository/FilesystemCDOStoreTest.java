package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.File;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;

public class FilesystemCDOStoreTest {

  private CompoundDigitalObjectStore koStore;
  private ZipImportService zis;
  private ArkId arkId;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();


  @Before
  public void setUp() throws Exception {
    FileUtils.copyDirectory(
        new File("src/test/resources/shelf"),
        new File(folder.getRoot().getPath()));
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    arkId = new ArkId("hello", "world");
    koStore = new FilesystemCDOStore(connectionURL);
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void testMetaDataNotFound(){
    ObjectNode koNode = koStore.getMetadata("hello-xxxxxx");
    assertEquals("Hello  World Title", koNode.findValue("title").textValue());
  }
  @Test
  public void testGetMetaData(){
    ObjectNode koNode = koStore.getMetadata("hello-world");
    assertEquals("Hello World Title", koNode.findValue("title").textValue());
  }
  @Test
  public void testURIPathWindows(){
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    koStore = new FilesystemCDOStore(connectionURL);
    koStore.getAbsoluteLocation("");
  }
  @After
  public void deleteKO() throws Exception {
    koStore.delete(this.arkId.getDashArk());
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

    assertTrue(shelfIds.size()>1);
  }

  @Test
  public void getImplementations() throws Exception {
    List<String> expectedImplementations = new ArrayList<>();
    expectedImplementations.add("v0.1.0");
    expectedImplementations.add("v0.2.0");
    expectedImplementations.add("v0.3.0");
    List<String> implementations = koStore.getChildren(arkId.getDashArk()).stream().map(child -> StringUtils
        .substringAfterLast(child, FileSystems.getDefault().getSeparator())).collect(Collectors.toList());
    implementations.sort(Comparator.naturalOrder());
    assertEquals(expectedImplementations, implementations);
  }

  @Test
  public void getImplementationMetadata() throws Exception {

    ObjectNode metadata = koStore.getMetadata(arkId.getDashArk(), "v0.1.0");
    assertTrue(metadata.get("title").asText().contains("Hello World"));
    metadata.replace("title", new TextNode("TEST"));
    koStore.saveMetadata(metadata, arkId.getDashArk(), "v0.1.0");
    metadata = koStore.getMetadata(arkId.getDashArk(), "v0.1.0");
    assertEquals("TEST", metadata.get("title").asText());
  }


  // TODO: Redo this test PLZKTHX
  @Test
  public void getResource() throws Exception {

      JsonNode metadata = koStore.getMetadata(arkId.getDashArk(), "v0.1.0");
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
