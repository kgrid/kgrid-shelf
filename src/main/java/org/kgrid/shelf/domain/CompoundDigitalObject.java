package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CompoundDigitalObject {

  private String identifier;
  private JsonNode rootMetadatNode;
  private JsonNode metadata;
  private Map<String, byte[]> binaryResources = new HashMap<>();

  public CompoundDigitalObject(String identifier) {
    this.identifier = identifier;
  }

  public void setMetadata(JsonNode metadata) {
    this.metadata = metadata;


  }

  public Map<String, byte[]> getBinaryResources() {
    return binaryResources;
  }

  /**
   * Returns CDO items/containers with the path to the container.
   * Root node is first
   * @return
   */
  public Map<String, JsonNode> getContainers(){

    List<JsonNode> containers = new ArrayList<>();
    Map<String, JsonNode> containerMap = new LinkedHashMap<>();


    JsonNode jsonContext=metadata.get("@context");
    metadata.get("@graph").deepCopy().elements().forEachRemaining( element ->
    {
      ((ObjectNode) element).set("@context", jsonContext);
      if (element.get("@id").asText().equals(getIdentifier())) {
        containers.add(0,element);
      } else {
        containers.add(element);
      }
    });

    containers.forEach( container -> {

      String urlPath="";
      if ( getIdentifier().equals(container.get("@id").asText())){
        urlPath = container.get("@id").asText();
      } else {
        urlPath = getIdentifier()+
            "/"+container.get("@id").asText();
      }
      containerMap.put(urlPath, container);

    });

    return containerMap;

  }

  public String getIdentifier() {
    return identifier;
  }

  public JsonNode getMetadata() {
    return this.metadata;
  }
}
