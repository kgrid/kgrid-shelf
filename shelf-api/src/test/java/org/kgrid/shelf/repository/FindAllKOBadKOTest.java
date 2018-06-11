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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {KnowledgeObjectRepository.class, CompoundDigitalObjectStoreFactory.class, FilesystemCDOStore.class})
public class FindAllKOBadKOTest {

  @Autowired
  KnowledgeObjectRepository repository;

  @Autowired
  CompoundDigitalObjectStoreFactory factory;

  @Test
  public void findAllKOs() {
    Map<ArkId, Map<String, ObjectNode>>  objects = repository.findAll();

    assertEquals(3, objects.size());

    }





}