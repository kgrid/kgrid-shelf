package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.service.ImportService;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.*;

@Category(FedoraFusekiTest.class)
public class FusekiClientTest {

  static FusekiClient fusekiClient;
  static CompoundDigitalObjectStore compoundDigitalObjectStore;

  @BeforeClass
  public static void setup() {

    try {
      fusekiClient = new FusekiClient("http://localhost:8080/fuseki/test/query");
      compoundDigitalObjectStore = new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");
      ImportService importService = new ImportService();

      // Load Hello-World example object

      URI helloWorldLoc =
          URI.create("file:src/test/resources/fixtures/import-export/hello-world.zip");

      importService.importZip(helloWorldLoc);

      // Load ri-bmicalc example object
      URI bmiLoc = URI.create("file:src/test/resources/fixtures/import-export/ri-bmicalc.zip");
      importService.importZip(bmiLoc);

      /*
       * TODO: Add a delay here after object creation to allow fuseki to detect that these
       *   objects have been created
       */

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }
  }

  @Test
  public void getAllKnowledgeObjects() throws Exception {
    JsonNode node = fusekiClient.getAllKnowledgeObjects();
    assertTrue("json-ld has @graph", node.has("@graph"));
    List<LinkedHashMap> list = JsonPath.parse(node.toString()).read("$.@graph[*]", List.class);
    assertEquals("ko list has two objects", 2, list.size());
  }

  @Test
  public void getAllKnowledgeObjectImpls() throws Exception {
    JsonNode node = fusekiClient.getAllKnowledgeObjectImpls();
    assertTrue("json-ld has @graph", node.has("@graph"));
    List<LinkedHashMap> list = JsonPath.parse(node.toString()).read("$.@graph[*]", List.class);
    assertEquals("overall version list has three objects", 3, list.size());
  }

  @Test
  public void getImplObjectsOfKO() throws Exception {
    ArkId arkId = new ArkId("hello", "world");
    JsonNode impls = fusekiClient.getImplGraphOfKO(arkId);
    assertTrue("hello world version list has @graph", impls.has("@graph"));
    List<LinkedHashMap> list = JsonPath.parse(impls.toString()).read("$.@graph[*]", List.class);
    assertEquals("hello world version list has two objects", 2, list.size());
  }

  // This test uses all the memory in the test vm and crashes it D:
  //  @Test
  //  public void getImplOfKoList() throws Exception {
  //    ArkId arkId = new ArkId("hello", "world");
  //    List list = fusekiClient.getImplListOfKO(arkId);
  //    assertTrue("List has v0.0.1 impl",
  //        list.contains("http://localhost:8080/fcrepo/rest/hello-world/v0.0.1"));
  //    assertTrue("List has v0.0.2 impl",
  //        list.contains("http://localhost:8080/fcrepo/rest/hello-world/v0.0.2"));
  //  }

  @AfterClass
  public static void teardown() throws Exception {

    compoundDigitalObjectStore.delete("hello-world");
    compoundDigitalObjectStore.delete("ri-bmicalc");
  }
}
