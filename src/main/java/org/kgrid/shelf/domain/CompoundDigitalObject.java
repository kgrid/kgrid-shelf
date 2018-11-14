package org.kgrid.shelf.domain;

import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CompoundDigitalObject {

  private String identifier;
  private Map<String, JsonNode > containers = new HashMap<>();
  private Map<String, byte[]> binaryResources = new HashMap<>();

  public CompoundDigitalObject(String identifier) {
    this.identifier = identifier;
  }



  public Map<String, JsonNode> getContainers() {

    containers = sortContainers(containers);

    return containers;
  }

  /**
   * Sort the container based on path, helps with import/export processing of containers
   * @param containers
   * @return
   */
  protected Map<String, JsonNode> sortContainers(Map<String, JsonNode> containers) {

    return containers
          .entrySet()
          .stream()
          .sorted(comparingByKey())
          .collect( toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2, LinkedHashMap::new));

  }

  public Map<String, byte[]> getBinaryResources() {
    return binaryResources;
  }

  public String getIdentifier() {
    return identifier;
  }


}
