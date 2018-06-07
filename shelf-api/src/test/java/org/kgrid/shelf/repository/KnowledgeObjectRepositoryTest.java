package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.kgrid.shelf.domain.ArkId;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {KnowledgeObjectRepository.class, CompoundDigitalObjectStoreFactory.class, FilesystemCDOStore.class})
public class KnowledgeObjectRepositoryTest {

  KnowledgeObjectRepository repository;

  @Autowired
  CompoundDigitalObjectStoreFactory factory;

  private ArkId arkId = new ArkId("ark:/99999/fk45m6gq9t");

  @Before
  public void setUp() throws Exception {
    factory.setShelfClass("filesystemCDOStore");
    repository = new KnowledgeObjectRepository(factory);
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/99999-fk45m6gq9t.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", "99999-fk45m6gq9t.zip", "application/zip", zippedKO);
    repository.save(koZip);
  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("ark:/99999/fk45m6gq9t"));
  }

  @Test
  public void getKnowledgeObject() throws Exception {
    assertNotNull(repository.putVersionZipFileIntoOutputStream(arkId, "v0.0.1"));
  }

  @Test
  public void getAllKOVersions() throws Exception {
    assertNotNull(repository.findByArkId(arkId));
  }

  @Test
  public void getCorrectMetadata() throws Exception {
    KnowledgeObject ko = repository.putVersionZipFileIntoOutputStream(arkId, "v0.0.1");
    assertTrue(ko.getMetadata().get("metadata").has("arkId"));
    String resource = ko.getModelMetadata().get("resource").asText();
    assertEquals("resource/content.js", resource);
  }

  @Test
  public void getCorrectModelMetadata() throws Exception {
    ObjectNode modelMetadata = repository.getMetadataAtPath(arkId, "v0.0.1", KnowledgeObject.MODEL_DIR_NAME);
    assertTrue(modelMetadata.has("resource"));
  }

  @Test
  public void listAllObjects() {
    Map<ArkId, Map<String, ObjectNode>>  objects = repository.findAll();
    assertEquals(1, objects.size());
    assertEquals("v0.0.1", objects.get(arkId).get("v0.0.1").get("version").asText());
  }

  @Test
  public void testEditMainMetadata() {
    String testdata = "{\"test\":\"data\"}";
    ObjectNode metadata = repository.editMetadata(arkId, "v0.0.1", null, testdata);
    assertEquals("data", metadata.get("test").asText());
  }


}