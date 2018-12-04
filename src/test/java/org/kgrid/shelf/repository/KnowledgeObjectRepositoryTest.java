package org.kgrid.shelf.repository;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.mock.web.MockMultipartFile;

@RunWith(JUnit4.class)
public class KnowledgeObjectRepositoryTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  KnowledgeObjectRepository repository;
  CompoundDigitalObjectStore compoundDigitalObjectStore;

  ZipImportService zipImportService = new ZipImportService();
  ZipExportService zipExportService = new ZipExportService();

  private ArkId arkId = new ArkId("hello", "world", "koio.v1");

  @Before
  public void setUp() throws Exception {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);
    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipImportService, zipExportService);

    //Load Hello-World example object
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);
    assertNotNull(repository.findImplementationMetadata(arkId));

  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("ark:/hello/world"));
    try {
      JsonNode metadata = repository.findKnowledgeObjectMetadata(new ArkId("hello-world"));
      assertTrue("Should have deleted helo-world", false);
    }catch (ShelfException e){
      assertTrue(true);
    }
  }

  @Test
  public void getKnowledgeObject() throws Exception {
    assertNotNull(repository.findImplementationMetadata(arkId));
  }

  @Test
  public void getTopLevelMetadata() throws Exception {

    JsonNode map = repository.findKnowledgeObjectMetadata(arkId);
    assertNotNull(map);
  }



  @Test
  public void getCorrectMetadata() throws Exception {
    JsonNode koMeatadata = repository.findImplementationMetadata(arkId);
    assertTrue(koMeatadata.findValue("identifier").asText().equals("koio.v1"));
    String resource = koMeatadata.findValue("hasPayload").asText();
    assertEquals("koio.v1/welcome.js", resource);
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, JsonNode>  objects = repository.findAll();
    assertEquals(1,objects.size());
    assertEquals("hello-world", objects.get(arkId).get("@id").asText());
  }

  @Test
  public void testEditMainMetadata() {
    String testdata = "{\"test\":\"data\"}";
    ObjectNode metadata = repository.editMetadata(arkId, null, testdata);
    assertEquals("data", metadata.get("test").asText());
  }


  @Test
  public void findServiceSpecification() throws IOException, URISyntaxException {

    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);
    assertEquals( "Hello, World", serviceSpecNode.path("info").path("title").asText());
    assertEquals( "/welcome", serviceSpecNode.findValue("paths").fieldNames().next());

  }

  @Test(expected = Exception.class)
  public void findServiceSpecificationNotFound()  {

    ArkId arkId = new ArkId("hello-world/koio.v2");
    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);

  }
  @Test
  public void findDeploymentSpecification() throws IOException, URISyntaxException {

    ArkId arkId = new ArkId("hello-world/koio.v1");
    JsonNode serviceSpecNode = repository.findDeploymentSpecification(arkId);
    assertNotNull( serviceSpecNode );

  }

  @Test
  public void findPayload() throws IOException, URISyntaxException {

    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload = repository.findPayload(arkId, "koio.v1/welcome.js");
    assertNotNull( payload );

  }

  @Test(expected = ShelfException.class)
  public void findPayloadNotFound() throws IOException, URISyntaxException {


    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload  = repository.findPayload(arkId, "koio.v1/one/two/three/welcome.js");

  }
}