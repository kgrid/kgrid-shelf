package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class KnowledgeObject {

  private ObjectNode metadata;
  private ArkId arkId;

  private final Path basePath;
  private final Path versionPath;
  private final Path modelPath;
  private final Path resourcePath;
  private final Path servicePath;


  private static final String MODELS_DIR_NAME = "models";
  private static final String RESOURCE_DIR_NAME = "resource";
  private static final String SERVICE_DIR_NAME = "service";
  public static final String METADATA_FILENAME = "metadata.json";
  private static final String ARK_ID_LABEL = "arkId";
  private static final String VERSION_LABEL = "version";
  private static final String TITLE_LABEL = "title";
  private static final String ADAPTER_LABEL = "adapterType";
  private static final String FUNCTION_LABEL = "functionName";
  private static final String RESOURCE_LABEL = "resource";
  private static final String SERVICE_LABEL = "service";

  public KnowledgeObject(ArkId arkId, String version) {
    this.arkId = arkId;
    basePath = Paths.get(arkId.getFedoraPath());
    versionPath = basePath.resolve(version);
    modelPath = versionPath.resolve(MODELS_DIR_NAME);
    resourcePath = modelPath.resolve(RESOURCE_DIR_NAME);
    servicePath = modelPath.resolve(SERVICE_DIR_NAME);
  }

  @JsonIgnore
  public Path getBaseDir() {
    return basePath;
  }

  @JsonIgnore
  public Path getVersionDir() {
    return versionPath;
  }

  @JsonIgnore
  public Path getModelDir() {
    return modelPath;
  }

  @JsonIgnore
  public Path getResourceDir() {
    return resourcePath;
  }

  @JsonIgnore
  public Path getServiceDir() {
    return servicePath;
  }

  public Path baseMetadataLocation() {
    return versionPath.resolve(METADATA_FILENAME);
  }

  public Path modelMetadataLocation() {
    return modelPath.resolve(METADATA_FILENAME);
  }

  public Path resourceLocation() {
    return modelPath.resolve(getModelMetadata().get(RESOURCE_LABEL).asText());
  }

  public Path serviceLocation() {
    return modelPath.resolve(SERVICE_DIR_NAME);
  }

  @JsonIgnore
  public ArkId getArkId() {
    return arkId;
  }

  public String version() {
    return versionPath.getFileName().toString();
  }

  public String adapterType() {
    return getModelMetadata().get(ADAPTER_LABEL).asText();
  }

  public void setMetadata(ObjectNode metadata) {
    this.metadata = metadata;
  }

  public ObjectNode getMetadata() {
    return metadata;
  }
  @JsonIgnore
  public ObjectNode getModelMetadata() {
    return (ObjectNode) metadata.get(MODELS_DIR_NAME);
  }

  public void setModelMetadata(ObjectNode metadataNode) {
    this.metadata.set(MODELS_DIR_NAME, metadataNode);
  }



}
