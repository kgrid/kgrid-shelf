package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;

@RunWith(JUnit4.class)
public class KnowledgeObjectRepositoryTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  KnowledgeObjectRepository repository;
  CompoundDigitalObjectStore compoundDigitalObjectStore;

  ZipImportService zipImportService = new ZipImportService();
  ZipExportService zipExportService = new ZipExportService();

  private ArkId helloWorldArkId = new ArkId("hello", "world", "v0.1.0");
  private ArkId helloFolder = new ArkId("hello", "folder", "v0.1.0");

  @Before
  public void setUp() throws Exception {
    FileUtils.copyDirectory(
        new File("src/test/resources/shelf"), new File(folder.getRoot().getPath()));
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);

    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipImportService, zipExportService);
    assertTrue(repository.findAll().size() > 0);

  }

  @Test
  public void getKnowledgeObject() throws Exception {
    assertNotNull(repository.findKnowledgeObjectMetadata(helloWorldArkId));
  }

  @Test
  public void getCorrectMetadata() throws Exception {
    JsonNode koMeatadata = repository.findKnowledgeObjectMetadata(helloWorldArkId);
    assertTrue(koMeatadata.findValue("identifier").asText().equals("ark:/hello/world"));
    assertTrue(koMeatadata.findValue("version").asText().equals("v0.1.0"));
  }

  @Test
  public void getCorrectFolderMetadata() throws Exception {
    JsonNode koMeatadata = repository.findKnowledgeObjectMetadata(helloFolder);
    assertTrue(koMeatadata.findValue("identifier").asText().equals("ark:/hello/world"));
    assertTrue(koMeatadata.findValue("version").asText().equals("v0.1.0"));
  }

  @Test
  public void deleteImplentation() throws Exception {

    ArkId arkId = new ArkId("hello","world","v0.2.0");
    repository.delete(arkId);

    try{
      repository.findKnowledgeObjectMetadata(arkId);
      assertTrue("should not find "+ arkId.getDashArkVersion(), false);
    } catch ( ShelfResourceNotFound e){
      assertTrue(true);
    }
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, JsonNode>  objects = repository.findAll();
    assertEquals(4,objects.size());
    assertEquals("hello-world", objects.get(new ArkId("hello", "world", "v0.1.0")).get("@id").asText());
  }

  @Test
  public void testEditMainMetadata() {
    String testdata = "{\"test\":\"data\"}";
    ObjectNode metadata = repository.editMetadata(helloWorldArkId, null, testdata);
    assertEquals("data", metadata.get("test").asText());
  }


  @Test
  public void findServiceSpecification() throws IOException, URISyntaxException {

    JsonNode serviceSpecNode = repository.findServiceSpecification(helloWorldArkId);
    assertEquals( "Hello, World", serviceSpecNode.path("info").path("title").asText());
    assertEquals( "/welcome", serviceSpecNode.findValue("paths").fieldNames().next());

  }

  @Test(expected = ShelfResourceNotFound.class)
  public void findServiceSpecificationNotFound()  {

    ArkId arkId = new ArkId("hello-world/v0.4.0");
    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);

  }

  @Test(expected = ShelfException.class)
  public void findPayloadNotFound() throws IOException, URISyntaxException {


    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload  = repository.findPayload(arkId, "koio.v1/one/two/three/welcome.js");

  }

}
