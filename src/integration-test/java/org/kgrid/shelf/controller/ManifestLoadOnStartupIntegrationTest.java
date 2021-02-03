package org.kgrid.shelf.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kgrid.shelf.ShelfGateway;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.CompoundDigitalObjectStoreFactory;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.kgrid.shelf.service.ManifestReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


@SpringBootTest(
    classes = {
      ShelfGateway.class,
      ManifestReader.class,
      ManifestLoadOnStartupIntegrationTest.TestConfig.class
    },
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ManifestLoadOnStartupIntegrationTest {

  @TempDir static File folder;

  @Autowired KnowledgeObjectRepository shelf;

  @Autowired ConfigurableApplicationContext ctx;

  @Autowired ObjectMapper mapper;

  @LocalServerPort private String port;

  @BeforeAll
  public static void config() {
    System.setProperty(
        "kgrid.shelf.manifest", "classpath:/static/manifest-with-classpath-resource.json");
  }

  @Test
  public void testDefaultLoadWithAfterPropertiesSet() {

    // Default ImportExportController loads classpath manifest set in @BeforeClass config method

    Map<ArkId, JsonNode> kos = shelf.findAll();
    assertTrue(
        kos.containsKey(new ArkId("ark:/hello/world/v1.3")));
    assertTrue(kos.containsKey(new ArkId("ark:/score/calc/v0.2.0")));
  }

  @TestConfiguration
  static class TestConfig {

    @ConditionalOnMissingBean
    @Bean
    @Primary
    CompoundDigitalObjectStore getCdoStore() {
      return CompoundDigitalObjectStoreFactory.create("filesystem:" + folder.toURI());
    }
  }
}
