package org.kgrid.shelf.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ShelfGateway.class, ImportExportControllerIntegrationTest.TestConfig.class},
    webEnvironment = WebEnvironment.RANDOM_PORT)
public class ImportExportControllerIntegrationTest {

  @Rule
  public final TemporaryFolder shelfFolder = new TemporaryFolder();

  @Autowired
  KnowledgeObjectRepository shelf;

  @Autowired
  ConfigurableApplicationContext ctx;

  @Autowired
  CompoundDigitalObjectStore cdoStore;

  @Autowired
  ObjectMapper mapper;

  @LocalServerPort
  private String port;

  @BeforeClass
  public static void config() {
    System.setProperty("kgrid.shelf.manifest", "classpath:/static/manifest-with-classpath-resource.json");
  }

  @Test
  public void testDefaultLoadWithAfterPropertiesSet() {

    // Default ImportExportController loads classpath manifest set in @BeforeClass config method

    final Map<ArkId, JsonNode> kos = shelf.findAll();
    assertTrue("Shelf contains hello-world-v1.3", kos.containsKey(new ArkId("ark:/hello/world/v1.3")));
    assertTrue("Shelf contains score-calc-v0.2.0", kos.containsKey(new ArkId("ark:/score/calc/v0.2.0")));
  }

  // Exploratory tests just to see how things work
  // TODO: delete

  /**
   * Test manifest load from a new {@link ImportExportController}
   * with a hand loaded JsonNode manifest.
   *
   * Just to show the moving pieces a little better
   *
   * @throws IOException the io exception
   */
  @Test
  public void testDepositObjectFromManifestWithHttpLoading() throws IOException {

//    Empty the existing shelf
    shelf.delete(new ArkId("hello","world", "v1.3"));
    shelf.delete(new ArkId("score","calc", "v0.2.0"));
    assertEquals("shelf shpuld be empty", 0, shelf.findAll().size());

    // so let's load-on-startup from a BAD manifest
    final String localResource = "http://localhost:" + port + "/manifest-with-bad-http-resource.json";

    // reset to url manifest location with url-based ko locations
    System.setProperty("kgrid.shelf.manifest", localResource);
    ImportExportController iec = ctx.getBeanFactory().createBean(ImportExportController.class);

    assertEquals(iec.getStartupManifestLocation(),
        "http://localhost:" + port + "/manifest-with-bad-http-resource.json");
    assertEquals("shelf load should fail when the KOs can't be found", 0, shelf.findAll().size());

    // so let's create a GOOD manifest manually
    Resource manifestResource = ctx.getResource("http://localhost:" + port + "/manifest-with-http-resource.json");
    JsonNode manifest = mapper.readTree(readAndPortFilter(manifestResource, port));

    // and test the deposit method
    iec.depositKnowledgeObject(manifest);

    final Map<ArkId, JsonNode> kos = shelf.findAll();

    assertEquals(2, kos.size());
    assertTrue("Shelf contains hello-world-v1.3", kos.containsKey(new ArkId("ark:/hello/world/v1.3")));
    assertTrue("Shelf contains score-calc-v0.2.0", kos.containsKey(new ArkId("ark:/score/calc/v0.2.0")));
  }

  // ResourceLoader tests just to show it works

  @Test
  public void resourceLoaderTestUrl() throws IOException {
    Resource manifestResource = ctx.getResource("http://localhost:" + port + "/manifest-with-http-resource.json");

    assertTrue(manifestResource.exists());

    JsonNode manifest = mapper.readTree(readAndPortFilter(manifestResource, port));

    assertThat(manifest.get("manifest").get(0).asText(), containsString(port + "/hello-world-v1.3"));
  }

  @Test
  public void resourceLoaderTestsFile() throws IOException {

    Resource manifestResource = ctx.getResource("file:src/test/resources/static/manifest-with-classpath-resource.json");

    assertTrue(manifestResource.exists());
    JsonNode manifest = mapper.readTree(manifestResource.getInputStream());
    assertThat((manifest.toString()), containsString("hello-world-v1.3"));

    // Convert to absolute path
   manifestResource = ctx.getResource(manifestResource.getFile().toURI().toString());

    assertTrue(manifestResource.exists());
    manifest = mapper.readTree(manifestResource.getInputStream());
    assertThat((manifest.toString()), containsString("hello-world-v1.3"));
  }

  @Test
  public void resourceLoaderTestClasspath() throws IOException {

    Resource manifestResource = ctx.getResource("classpath:/static/manifest-with-classpath-resource.json");

    assertTrue(manifestResource.exists());
    JsonNode manifest = mapper.readTree(manifestResource.getInputStream());
    assertThat((manifest.toString()), containsString("hello-world-v1.3"));

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

  private String readAndPortFilter(Resource manifestResource, String port) throws IOException {
    String s;
    try (InputStream stream = manifestResource.getInputStream()) {
       s = IOUtils.toString(stream, "UTF-8");
    }
    return StringUtils.replace(s, "8080", port);
  }

}
