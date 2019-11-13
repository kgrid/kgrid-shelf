package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    arkId = new ArkId("hello", "world", "v0.1.0");
    koStore = new FilesystemCDOStore(connectionURL);
  }

  @Test(expected = ShelfResourceNotFound.class)
  public void testMetaDataNotFound(){
    ObjectNode koNode = koStore.getMetadata("hello-xxxxxx");
    assertEquals("Hello  World Title", koNode.findValue("title").textValue());
  }
  @Test
  public void testGetMetaData(){
    ObjectNode koNode = koStore.getMetadata("hello-world-v0.1.0");
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
    List<String> shelfIds = koStore.getChildren("").stream().map(name -> {
      try {
        return StringUtils.substringAfterLast(name, FileSystems.getDefault().getSeparator());
      } catch (IllegalArgumentException e) {
        e.printStackTrace(); return null;
      }
    }).filter(
        Objects::nonNull).collect(Collectors.toList());
    assertTrue(shelfIds.size() == 4);
  }

}
