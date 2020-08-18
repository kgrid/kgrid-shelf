package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.kgrid.shelf.domain.KoFields.*;

public class KnowledgeObjectWrapper {
  private JsonNode metadata;
  private JsonNode deployment;
  private JsonNode service;
  private Map<KoFields, URI> metadataLocations;
  private URI id;

  Logger log = LoggerFactory.getLogger(KnowledgeObjectWrapper.class);

  public KnowledgeObjectWrapper(JsonNode metadata) {
    this.metadata = metadata;
    this.metadataLocations = getKoParts();
    this.id = URI.create(metadata.get("@id").asText() + "/");
    this.deployment = new ObjectMapper().createObjectNode();
    this.service = new ObjectMapper().createObjectNode();
  }

  public void addDeployment(JsonNode spec) {
    if (spec == null) {
      log.warn("Loading blank deployment into object " + this.id);
    } else {
      this.deployment = spec;
    }
  }

  public void addService(JsonNode spec) {
    if (spec == null) {
      log.warn("Loading blank service into object " + this.id);
    } else {
      this.service = spec;
    }
  }

  public URI getDeploymentLocation() {
    return metadataLocations.get(DEPLOYMENT_SPEC_TERM);
  }

  public URI getServiceSpecLocation() {
    return metadataLocations.get(SERVICE_SPEC_TERM);
  }

  public URI getId() {
    return id;
  }

  public Map<KoFields, URI> getKoParts() {
    Map<KoFields, URI> koParts = new HashMap<>();
    koParts.put(METADATA_FILENAME, URI.create(METADATA_FILENAME.asStr()));
    koParts.put(
        DEPLOYMENT_SPEC_TERM, URI.create(metadata.at("/" + DEPLOYMENT_SPEC_TERM.asStr()).asText()));
    koParts.put(
        SERVICE_SPEC_TERM, URI.create(metadata.at("/" + SERVICE_SPEC_TERM.asStr()).asText()));
    return koParts;
  }

  public List<URI> getArtifactLocations() {
    List<URI> artifacts = new ArrayList<>(metadataLocations.values());
    this.deployment
        .at("/" + KoFields.ENDPOINTS.asStr())
        .forEach(
            endpoint -> {
              final JsonNode artifactNode = endpoint.get(KoFields.ARTIFACT.asStr());
              if (artifactNode.isArray()) {
                artifactNode.forEach(node -> artifacts.add(URI.create(node.asText())));
              } else {
                artifacts.add(URI.create(artifactNode.asText()));
              }
            });
    return artifacts;
  }
}
