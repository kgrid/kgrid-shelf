package org.kgrid.shelf.repository;

import static org.junit.Assert.assertNotNull;

import org.kgrid.shelf.domain.ArkId;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

  @Before
  public void setUp() {
    factory.setShelfClass("filesystemCDOStore");
    repository = new KnowledgeObjectRepository(factory);
  }

  @After
  public void clearShelf() throws Exception {
    repository.delete(new ArkId("ark:/99999/fk45m6gq9t"));
  }

  @Test
  public void getKnowledgeObject() throws Exception {
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/99999-fk45m6gq9t.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", "99999-fk45m6gq9t.zip", "application/zip", zippedKO);
    repository.save(koZip);
    assertNotNull(repository.findByArkIdAndVersion(new ArkId("ark:/99999/fk45m6gq9t"), "v0.0.1"));
  }

  @Test
  public void getAllKOVersions() throws Exception {
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/99999-fk45m6gq9t.zip");
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", "99999-fk45m6gq9t.zip", "application/zip", zippedKO);
    repository.save(koZip);
    assertNotNull(repository.findByArkId(new ArkId("ark:/99999/fk45m6gq9t")));
  }



}