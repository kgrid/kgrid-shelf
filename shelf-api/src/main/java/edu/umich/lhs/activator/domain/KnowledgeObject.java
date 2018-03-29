package edu.umich.lhs.activator.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface KnowledgeObject {

  URI baseMetadataLocation();

  URI modelMetadataLocation();

  ArkId getArkId();

  String version();

  String adapterType();

  URI resourceLocation();

  URI serviceLocation();

  ObjectNode getMetadata();

  ObjectNode getModelMetadata();

  void setMetadata(ObjectNode metadata);

  void setModelMetadata(ObjectNode modelMetadata);

}
