package org.kgrid.shelf.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.kgrid.shelf.ShelfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
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
    String rawId = metadata.get("@id").asText() + "/";

    this.metadata = metadata;
    this.metadataLocations = getKoParts();
    try {
      this.id = URI.create(rawId.startsWith("/") ? rawId.substring(1) : rawId);
    } catch (Exception e) {
      throw new ShelfException(
          "Cannot create identifier from @id " + rawId + " " + e.getMessage(), e);
    }
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

  public JsonNode getMetadata() {
    return metadata;
  }

  public JsonNode getDeployment() {
    return deployment;
  }

  public JsonNode getService() {
    return service;
  }

  private Map<KoFields, URI> getKoParts() {
    Map<KoFields, URI> koParts = new HashMap<>();
    koParts.put(METADATA_FILENAME, URI.create(METADATA_FILENAME.asStr()));
    koParts.put(
        DEPLOYMENT_SPEC_TERM, URI.create(metadata.at("/" + DEPLOYMENT_SPEC_TERM.asStr()).asText()));
    koParts.put(
        SERVICE_SPEC_TERM, URI.create(metadata.at("/" + SERVICE_SPEC_TERM.asStr()).asText()));
    return koParts;
  }

  public HashSet<URI> getArtifactLocations() {
    HashSet<URI> artifacts = new HashSet<>(metadataLocations.values());
    this.deployment.forEach(
        endpoint ->
            endpoint.forEach(
                method -> {
                  final JsonNode artifactNode = method.get(KoFields.ARTIFACT.asStr());
                  if (artifactNode.isArray()) {
                    artifactNode.forEach(
                        node -> {
                          artifacts.add(URI.create(node.asText()));
                        });
                  } else {
                    artifacts.add(URI.create(artifactNode.asText()));
                  }
                }));
    return artifacts;
  }
}
