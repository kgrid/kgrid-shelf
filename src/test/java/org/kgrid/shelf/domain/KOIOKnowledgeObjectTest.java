package org.kgrid.shelf.domain;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Test;

public class KOIOKnowledgeObjectTest {


  @Test
  public void createKOIOKnowledgeObjectTest() throws Exception{
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream inputStream = new FileInputStream("src/test/resources/metadata.jsonld");
    JsonNode jsonNode = objectMapper.readTree(inputStream);
    KOIOKnowledgeObject ko = new KOIOKnowledgeObject(jsonNode);


    assertEquals("Hello World Title",ko.getKnowledgeObject().get("title").asText());
    assertEquals(2, ko.getImplementations().size());

  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void isImplementationTest() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree("{\"@type\": [\n"
        + "        \"koio:Implementation\",\n"
        + "        \"fedora:Resource\",\n"
        + "        \"fedora:Container\"\n"
        + "      ]}");

    KOIOKnowledgeObject ko = new KOIOKnowledgeObject();

    assertTrue("Should have found Implementation in " + node, ko.isImplementation(node.get("@type")));

    node = mapper.readTree("{\"@type\": \"Implementation\"}");

    assertTrue("Should have found Implementation in " + node,
        ko.isImplementation(node.get("@type")));

  }


  @Test
  public void isKnowledgeObjectTest() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree("{\"@type\": [\n"
        + "        \"fedora:Resource\",\n"
        + "        \"ldp:Container\",\n"
        + "        \"fedora:Container\",\n"
        + "        \"ldp:RDFSource\",\n"
        + "        \"koio:KnowledgeObject\"\n"
        + "      ]}");

    KOIOKnowledgeObject ko = new KOIOKnowledgeObject();

    assertTrue("Should have found KnowledgeObject in " + node, ko.isKnowledgeObject(node.get("@type")));

    node = mapper.readTree("{\"@type\": \"KnowledgeObject\"}");

    assertTrue("Should have found KnowledgeObject in " + node,
        ko.isKnowledgeObject(node.get("@type")));

    node = mapper.readTree("{}");

    assertFalse("Should not find found KnowledgeObject in " + node,
        ko.isKnowledgeObject(node));


  }
}