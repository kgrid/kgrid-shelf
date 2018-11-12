package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class KOIOKnowledgeObject {

  private Collection<JsonNode> implementations = new ArrayList<JsonNode>();
  private JsonNode knowledgeObject;
  private JsonNode jsonContext;

  public KOIOKnowledgeObject() {
  }

  public KOIOKnowledgeObject(JsonNode jsonKO) {
    jsonContext=jsonKO.get("@context");
    jsonKO.get("@graph").elements().forEachRemaining(element ->
    {
      if( isImplementation(element.get("@type")) ){

        ((ObjectNode) element).set("@context", jsonContext);
        implementations.add(element);

      } else if ( isKnowledgeObject(element.get("@type")) ){

        knowledgeObject = ((ObjectNode) element).set("@context", jsonContext);

      }
    });
  }

  public JsonNode getKnowledgeObject() { return knowledgeObject; }

  public Collection<JsonNode> getImplementations() {
    return implementations;
  }

  public boolean isKnowledgeObject(JsonNode typeJsonNode){

    String implementation = "KnowledgeObject";

    return findType(typeJsonNode, implementation);

  }
    public boolean isImplementation(JsonNode typeJsonNode){

    String implementation = "Implementation";

    return findType(typeJsonNode, implementation);

  }

  protected boolean findType(JsonNode typeJsonNode, String implementation) {

    if (typeJsonNode == null) {
      return false;
    } else if (typeJsonNode.isArray()) {

      for (Iterator<JsonNode> it = typeJsonNode.elements(); it.hasNext(); ) {
        JsonNode element = it.next();
        if (element.asText().endsWith(implementation)) {
          return true;
        }
      }

    } else if (typeJsonNode.asText().endsWith(implementation)) {
      return true;
    }

    return false;
  }

  @Override
  public String toString() {
    return "KOIOKnowledgeObject{" +
        " knowledgeObject=" + knowledgeObject +
        ", implementations=" + implementations +
        ", jsonContext=" + jsonContext +
        '}';
  }
}
