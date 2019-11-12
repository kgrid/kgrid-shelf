package org.kgrid.shelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Test;

public class KnowledgeObjectTest {

  @Test
  public void getImplementationIDs() throws IOException {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(  "{ \"hasImplementation\": ["
        + "    \"hello-world/v0.0.1\","
        + "    \"hello-world/v0.0.2\""
        + "  ] }");

    assertTrue( KnowledgeObject.getVersionIDs(node).isArray() );
    assertEquals(2, node.findValue(KnowledgeObject.IMPLEMENTATIONS_TERM).size());


     node = mapper.readTree(  "{ \"hasImplementation\":\"hello-world/v0.0.1\"}");
     assertFalse( KnowledgeObject.getVersionIDs(node).isArray() );
     assertEquals("hello-world/v0.0.1", node.findValue(KnowledgeObject.IMPLEMENTATIONS_TERM).asText());
  }
}
