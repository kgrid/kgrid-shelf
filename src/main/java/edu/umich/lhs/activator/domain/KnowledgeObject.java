package edu.umich.lhs.activator.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface KnowledgeObject {

  Path getBaseMetadataLocation();

  Path getModelMetadataLocation();

  ArkId getArkId();

  String getVersion();

  String getAdapterType();

  Path getResourceLocation();

  Path getServiceLocation();

  ObjectNode getMetadata();

  ObjectNode getModelMetadata();

  void setMetadata(ObjectNode metadata);

  void setModelMetadata(ObjectNode modelMetadata);

}
