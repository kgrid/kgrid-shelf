package org.kgrid.shelf.repository;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kgrid.shelf.domain.ArkId;

@Category(FedoraFusekiTest.class)
public class FusekiClientTest {

  static FusekiClient fusekiClient;
  static CompoundDigitalObjectStore compoundDigitalObjectStore;

  @BeforeClass
  public static void setup() {

    try {
      fusekiClient = new FusekiClient("http://localhost:8080/fuseki/test/query");
      compoundDigitalObjectStore = new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");
      ZipImportService zipImportService = new ZipImportService();

      //Load Hello-World example object
      InputStream zipStream = FedoraCDOStoreTest.class.getResourceAsStream("/fixtures/hello-world.zip");
      zipImportService.importKO(zipStream, compoundDigitalObjectStore);

      //Load ri-bmicalc example object
      zipStream = FedoraCDOStoreTest.class.getResourceAsStream("/fixtures/ri-bmicalc.zip");
      zipImportService.importKO(zipStream, compoundDigitalObjectStore);

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
    assertEquals("overall implementation list has three objects", 3, list.size());
  }

  @Test
  public void getImplObjectsOfKO() throws Exception {
    ArkId arkId = new ArkId("hello", "world");
    JsonNode impls = fusekiClient.getImplGraphOfKO(arkId);
    assertTrue("hello world implementation list has @graph", impls.has("@graph"));
    List<LinkedHashMap> list = JsonPath.parse(impls.toString()).read("$.@graph[*]", List.class);
    assertEquals("hello world implementation list has two objects", 2, list.size());
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