package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfGateway;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.CompoundDigitalObjectStoreFactory;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = {ShelfGateway.class, ImportExportControllerIntegrationTest.TestConfig.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ImportExportControllerIntegrationTest {

  @Rule public final TemporaryFolder shelfFolder = new TemporaryFolder();

  @Autowired KnowledgeObjectRepository shelf;

  @Autowired ConfigurableApplicationContext ctx;

  @Autowired ObjectMapper mapper;

  @LocalServerPort private String port;

  @BeforeClass
  public static void config() {
    System.setProperty(
        "kgrid.shelf.manifest", "classpath:/static/manifest-with-classpath-resource.json");
  }

  @Test
  public void testDefaultLoadWithAfterPropertiesSet() {

    // Default ImportExportController loads classpath manifest set in @BeforeClass config method

    Map<ArkId, JsonNode> kos = shelf.findAll();
    assertTrue(
        "Shelf contains hello-world-v1.3", kos.containsKey(new ArkId("ark:/hello/world/v1.3")));
    assertTrue(
        "Shelf contains score-calc-v0.2.0", kos.containsKey(new ArkId("ark:/score/calc/v0.2.0")));
  }

  @Test
  public void resourceLoaderTestUrl() throws IOException {
    Resource manifestResource =
        ctx.getResource("http://localhost:" + port + "/manifest-with-http-resource.json");

    assertTrue(manifestResource.exists());

    JsonNode manifest = mapper.readTree(readAndPortFilter(manifestResource, port));

    String firstUri = manifest.get("manifest").get(0).asText();
    assertTrue(firstUri.contains(port + "/hello-world-v1.3"));
  }

  @Test
  public void resourceLoaderTestsFile() throws IOException {

    Resource manifestResource =
        ctx.getResource("file:src/test/resources/static/manifest-with-classpath-resource.json");

    assertTrue(manifestResource.exists());
    JsonNode manifest = mapper.readTree(manifestResource.getInputStream());
    assertTrue((manifest.toString()).contains("hello-world-v1.3"));

    // Convert to absolute path
    manifestResource = ctx.getResource(manifestResource.getFile().toURI().toString());

    assertTrue(manifestResource.exists());
    manifest = mapper.readTree(manifestResource.getInputStream());
    assertTrue((manifest.toString()).contains("hello-world-v1.3"));
  }

  @Test
  public void resourceLoaderTestClasspath() throws IOException {

    Resource manifestResource =
        ctx.getResource("classpath:/static/manifest-with-classpath-resource.json");

    assertTrue(manifestResource.exists());
    JsonNode manifest = mapper.readTree(manifestResource.getInputStream());
    assertTrue((manifest.toString()).contains("hello-world-v1.3"));
  }

  private String readAndPortFilter(Resource manifestResource, String port) throws IOException {
    String s;
    try (InputStream stream = manifestResource.getInputStream()) {
      s = IOUtils.toString(stream, "UTF-8");
    }
    return StringUtils.replace(s, "8080", port);
  }

  @TestConfiguration
  static class TestConfig {

    @ConditionalOnMissingBean
    @Bean
    @Primary
    CompoundDigitalObjectStore getCdoStore() throws IOException {

      TemporaryFolder folder = new TemporaryFolder();
      folder.create();

      folder.getRoot().deleteOnExit();
      return CompoundDigitalObjectStoreFactory.create("filesystem:" + folder.getRoot().toURI());
    }
  }
}
