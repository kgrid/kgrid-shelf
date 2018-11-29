package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.kgrid.shelf.domain.KnowledgeObject;
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

  private ArkId arkId = new ArkId("ark:/hello/world");

  @Before
  public void setUp() throws Exception {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);
    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipImportService, zipExportService);
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/hello-world-jsonld.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));

    MockMultipartFile koZip = new MockMultipartFile("ko", "hello-world-jsonld.zip", "application/zip", zippedKO);
    repository.importZip(new ArkId("hello-world"), koZip);
    assertNotNull(repository.findByArkIdAndVersion(new ArkId("hello-world"), "v0.0.1"));
  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("ark:/hello/world"));
    try {
      Map<String, ObjectNode> map = repository.findByArkId(new ArkId("hello-world"));
      assertTrue("Should have deleted hell0-world", false);
    }catch (IllegalArgumentException e){
      assertTrue(true);
    }
  }

  @Test
  public void getKnowledgeObject() throws Exception {
    assertNotNull(repository.findByArkIdAndVersion(arkId, "v0.0.1"));
  }

  @Test
  public void getAllKOVersions() throws Exception {

    Map<String, ObjectNode> map = repository.findByArkId(arkId);
    assertNotNull(map);
  }



  @Test
  public void getCorrectMetadata() throws Exception {
    KnowledgeObject ko = repository.findByArkIdAndVersion(arkId, "v0.0.1");
    assertTrue(ko.getMetadata().findValue("identifier").asText().equals("v0.0.1"));
    String resource = ko.getMetadata().findValue("hasPayload").asText();
    assertEquals("v0.0.1/welcome.js", resource);
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, Map<String, ObjectNode>>  objects = repository.findAll();
    assertEquals("v0.0.1", objects.get(arkId).get("v0.0.1").findValue("@id").asText());
  }

  @Test
  public void testEditMainMetadata() {
    String testdata = "{\"test\":\"data\"}";
    ObjectNode metadata = repository.editMetadata(arkId, "v0.0.1", null, testdata);
    assertEquals("data", metadata.get("test").asText());
  }


}