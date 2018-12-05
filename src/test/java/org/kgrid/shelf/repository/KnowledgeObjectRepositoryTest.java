package org.kgrid.shelf.repository;

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

  private ArkId helloWorldArkId = new ArkId("hello", "world", "koio.v1");

  @Before
  public void setUp() throws Exception {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);
    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipImportService, zipExportService);

    //Load Hello-World example object
    URL zipStream = KnowledgeObjectRepositoryTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world.zip", "application/zip", zippedKO);;
    repository.importZip(helloWorldArkId, koZip);
    assertNotNull(repository.findImplementationMetadata(helloWorldArkId));

    ArkId bmiArkId = new ArkId("ri", "bmicalc");
    zipStream = KnowledgeObjectRepositoryTest.class.getResource("/fixtures/ri-bmicalc.zip");
    zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    koZip = new MockMultipartFile("ko", "ri-bmicalc.zip", "application/zip", zippedKO);;
    repository.importZip(bmiArkId, koZip);


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
    assertNotNull(repository.findImplementationMetadata(helloWorldArkId));
  }

  @Test
  public void getTopLevelMetadata() throws Exception {

    JsonNode map = repository.findKnowledgeObjectMetadata(helloWorldArkId);
    assertNotNull(map);
  }



  @Test
  public void getCorrectMetadata() throws Exception {
    JsonNode koMeatadata = repository.findImplementationMetadata(helloWorldArkId);
    assertTrue(koMeatadata.findValue("identifier").asText().equals("koio.v1"));
    String resource = koMeatadata.findValue("hasPayload").asText();
    assertEquals("koio.v1/welcome.js", resource);
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, JsonNode>  objects = repository.findAll();
    assertEquals(2,objects.size());
    assertEquals("hello-world", objects.get(helloWorldArkId).get("@id").asText());
    assertEquals("ri-bmicalc", objects.get(new ArkId("ri", "bmicalc")).get("@id").asText());
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