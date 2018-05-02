package kgrid.org.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;

@JsonIgnoreProperties(ignoreUnknown = true)
public interface KnowledgeObject {

  Path baseMetadataLocation();

  Path modelMetadataLocation();

  ArkId getArkId();

  String version();

  String adapterType();

  Path resourceLocation();

  Path serviceLocation();

  ObjectNode getMetadata();

  ObjectNode getModelMetadata();

  void setMetadata(ObjectNode metadata);

  void setModelMetadata(ObjectNode modelMetadata);

}
