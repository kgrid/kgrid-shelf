package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
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
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.mock.web.MockMultipartFile;
import org.zeroturnaround.zip.ZipUtil;

@RunWith(JUnit4.class)
public class KnowledgeObjectRepositoryTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  KnowledgeObjectRepository repository;
  CompoundDigitalObjectStore compoundDigitalObjectStore;

  ZipImportService zipImportService = new ZipImportService();
  ZipExportService zipExportService = new ZipExportService();

  private ArkId helloWorldArkId = new ArkId("hello", "world", "v0.1.0");
  private ArkId helloFolderArkId = new ArkId("hello", "folder", "v0.1.0");

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

    ZipUtil.unpack(
        KnowledgeObjectRepositoryTest.class.getResourceAsStream("/fixtures/mycoolko.zip"),
        folder.getRoot() );

    repository.findAll();
  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("hello-world"));
    repository.delete(new ArkId("ri-bmicalc"));

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
  public void getMetadataFromFolder() throws Exception {
    JsonNode metadata = repository.findKnowledgeObjectMetadata(helloFolderArkId);
    assertTrue(metadata.has("@id"));
    assertEquals(helloFolderArkId.getDashArk(), metadata.get("@id").asText());
  }

  @Test
  public void getImplementationMetadataFromFolder() throws Exception {
    JsonNode metadata = repository.findImplementationMetadata(helloFolderArkId);
    assertTrue(metadata.has("@id"));
    assertEquals(helloFolderArkId.getImplementation(), metadata.get("@id").asText());
  }


  @Test
  public void getCorrectMetadata() throws Exception {
    JsonNode koMeatadata = repository.findImplementationMetadata(helloWorldArkId);
    assertTrue(koMeatadata.findValue("identifier").asText().equals("ark:/hello/world/v0.1.0"));
  }

  @Test
  public void deleteImplentation() throws Exception {

    ArkId arkId = new ArkId("hello","world","v0.2.0");
    repository.deleteImpl(arkId);
    JsonNode metadata = repository.findKnowledgeObjectMetadata(new ArkId("hello","world"));
    assertEquals(2, metadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM).size());

    try{
      metadata = repository.findImplementationMetadata(arkId);
      assertTrue("should not find "+ arkId.getDashArkImplementation(), false);
    } catch ( ShelfResourceNotFound e){
      assertTrue(true);
    }


  }

  @Test
  public void listAllObjects() {
    Map<ArkId, JsonNode>  objects = repository.findAll();
    assertEquals(3,objects.size());
    assertEquals("hello-world", objects.get(new ArkId("hello", "world")).get("@id").asText());
    assertEquals("ri-bmicalc", objects.get(new ArkId("ri", "bmicalc")).get("@id").asText());
    assertEquals("hello-folder", objects.get(new ArkId("hello", "folder")).get("@id").asText());
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

    ArkId arkId = new ArkId("hello-world/koio.v2");
    JsonNode serviceSpecNode = repository.findServiceSpecification(arkId);

  }

  @Test(expected = ShelfException.class)
  public void findPayloadNotFound() throws IOException, URISyntaxException {


    ArkId arkId = new ArkId("hello-world/koio.v1");
    byte[] payload  = repository.findPayload(arkId, "koio.v1/one/two/three/welcome.js");

  }

  @Test
  public void exportKnowledgeObjectWackyFolderName() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportObject(
        new ArkId("hello", "folder"),  "mycoolko", compoundDigitalObjectStore);


  }
}