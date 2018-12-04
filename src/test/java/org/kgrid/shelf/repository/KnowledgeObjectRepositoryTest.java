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
import org.kgrid.shelf.domain.ArkId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {KnowledgeObjectRepository.class, CompoundDigitalObjectStoreFactory.class, FilesystemCDOStore.class, ZipImportService.class, ZipExportService.class})
public class KnowledgeObjectRepositoryTest {


  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  KnowledgeObjectRepository repository;
  CompoundDigitalObjectStore compoundDigitalObjectStore;

  @Autowired
  ZipImportService zipImportService;

  @Autowired
  ZipExportService zipExportService;

  private ArkId arkId = new ArkId("hello", "world", "v0.0.1");

  @Before
  public void setUp() throws Exception {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);
    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipImportService, zipExportService);
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world-jsonld.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-jsonld.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);
    assertNotNull(repository.findImplementationMetadata(arkId));

    zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-usa-jsonld.zip");
    zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    koZip = new MockMultipartFile("ko", "hello-world-jsonld.zip", "application/zip", zippedKO);;
    repository.importZip(new ArkId("hello", "usa"), koZip);

  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("ark:/hello/world"));
    try {
      JsonNode metadata = repository.findKnowledgeObjectMetadata(new ArkId("hello-world"));
      assertTrue("Should have deleted hell0-world", false);
    }catch (IllegalArgumentException e){
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
    assertTrue(koMeatadata.findValue("identifier").asText().equals("v0.0.1"));
    String resource = koMeatadata.findValue("hasPayload").asText();
    assertEquals("v0.0.1/welcome.js", resource);
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, JsonNode>  objects = repository.findAll();
    assertEquals(2,objects.size());
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

    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);

    ArkId arkId = new ArkId("hello-world/koio.v1");
    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);
    assertEquals( "Hello, World", serviceSpecNode.path("info").path("title").asText());
    assertEquals( "/welcome", serviceSpecNode.findValue("paths").fieldNames().next());

  }

  @Test(expected = IllegalArgumentException.class)
  public void findServiceSpecificationNotFound() throws IOException, URISyntaxException {

    ArkId arkId = new ArkId("hello-world/xxxx");
    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);

  }
  @Test
  public void findDeploymentSpecification() throws IOException, URISyntaxException {

    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);

    ArkId arkId = new ArkId("hello-world/koio.v1");
    JsonNode serviceSpecNode = repository.findDeploymentSpecification(arkId);
    assertNotNull( serviceSpecNode );


  }

  @Test
  public void findPayload() throws IOException, URISyntaxException {

    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);

    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload = repository.findPayload(arkId, "koio.v1/welcome.js");
    assertNotNull( payload );

  }

  @Test
  public void findPayloadNotFound() throws IOException, URISyntaxException {

    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-.zip", "application/zip", zippedKO);;
    repository.importZip(arkId, koZip);

    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload  = repository.findPayload(arkId, "one/two/three/welcome.js");
    assertNull( payload );
  }
}