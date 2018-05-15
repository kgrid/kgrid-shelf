package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.mock.web.MockMultipartFile;

public class FilesystemCDOStoreTest {

  private CompoundDigitalObjectStore koStore;
  private ArkId arkId;

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();


  @Before
  public void setUp() throws Exception {
    koStore = new FilesystemCDOStore(folder.getRoot().getAbsolutePath());
    Path shelf = koStore.getAbsoluteLocation(null);
    if(Files.isDirectory(shelf)) {
      nukeTestShelf(shelf);
    }
    Files.createDirectory(shelf);

    // Add zip file to our test shelf:
    this.arkId = addFixtureToShelf("99999-fk45m6gq9t.zip");

  }

  @After
  public void deleteKO() throws Exception {
    (koStore).removeFile(Paths.get(this.arkId.getFedoraPath()));
    Path shelf = koStore.getAbsoluteLocation(null);
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

  private ArkId addFixtureToShelf(String filename) throws Exception {
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/" + filename);
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
    ObjectNode json = koStore.addCompoundObjectToShelf(koZip);
    return new ArkId(json.get("metadata").get("arkId").get("arkId").asText());
  }

  @Test
  public void getObjectsOnShelf() throws Exception {
    List<ArkId> shelfIds = koStore.getChildren(null).stream().map(name -> {try {return new ArkId(name.getFileName().toString());} catch (IllegalArgumentException e) {e.printStackTrace(); return null;}}).filter(
        Objects::nonNull).collect(Collectors.toList());
    List<ArkId> expectedIds = Collections.singletonList(arkId);

    assertEquals(expectedIds, shelfIds);
  }

  @Test
  public void getVersions() throws Exception {
    List<String> expectedVersions = new ArrayList<>();
    expectedVersions.add("default");
    expectedVersions.add("v0.0.1");
    List<String> versions = koStore.getChildren(Paths.get(arkId.getFedoraPath())).stream().map(child -> child.getFileName().toString()).collect(Collectors.toList());
    versions.sort(Comparator.naturalOrder());
    assertEquals(expectedVersions, versions);
  }

  @Test
  public void getBaseMetadata() throws Exception {
    ArkId arkId = koStore.getChildren(null).stream().map(name -> {try {return new ArkId(name.getFileName().toString());} catch (IllegalArgumentException e) {e.printStackTrace(); return null;}}).filter(
        Objects::nonNull).collect(Collectors.toList()).get(0);
    String version = koStore.getChildren(Paths.get(arkId.getFedoraPath())).get(0).getFileName().toString();
    KnowledgeObject ko = new KnowledgeObject(arkId, version);
    ObjectNode metadata = (ObjectNode)koStore.getMetadata(ko.baseMetadataLocation()).get("metadata");
    assertEquals("Stent Thrombosis Risk Calculator", metadata.get("title").asText());
    metadata.replace("title", new TextNode("TEST"));
    koStore.saveMetadata(ko.baseMetadataLocation(), metadata);
    metadata = koStore.getMetadata(ko.baseMetadataLocation());
    assertEquals("TEST", metadata.get("title").asText());
  }

  @Test
  public void getResource() throws Exception {
    String version = koStore.getChildren(Paths.get(arkId.getFedoraPath())).get(0).getFileName().toString();
    KnowledgeObject ko = new KnowledgeObject(arkId, version);
    JsonNode metadata = koStore.getMetadata(ko.baseMetadataLocation());
    JsonNode modelMetadata = koStore.getMetadata(ko.modelMetadataLocation());
    ko.setMetadata((ObjectNode)metadata);
    ko.setModelMetadata((ObjectNode)modelMetadata);
    Path resourceLocation = ko.resourceLocation();
    byte[] resource = koStore.getBinary(resourceLocation);
    assertEquals("function content(riskValues) {", new String(resource, Charset.defaultCharset()).substring(0, 30));
    String data =  "test data for broken payload";
    byte[] dataArray = data.getBytes();
    koStore.saveBinary(resourceLocation, dataArray);

    resource = koStore.getBinary(resourceLocation);

    assertEquals(data, new String(resource, Charset.defaultCharset()));
  }



}