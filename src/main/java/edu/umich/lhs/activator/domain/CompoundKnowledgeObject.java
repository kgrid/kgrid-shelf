package edu.umich.lhs.activator.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompoundKnowledgeObject implements KnowledgeObject {

  private ObjectNode koMetadata;
  private ArkId arkId;

  private final Path basePath;
  private final Path versionPath;
  private final Path modelPath;
  private final Path resourcePath;
  private final Path servicePath;

  private static final String MODELS_DIR_NAME = "models";
  private static final String RESOURCE_DIR_NAME = "resource";
  private static final String SERVICE_DIR_NAME = "service";
  private static final String METADATA_FILENAME = "metadata.json";
  private static final String ARK_ID_LABEL = "arkId";
  private static final String VERSION_LABEL = "version";
  private static final String TITLE_LABEL = "title";
  private static final String ADAPTER_LABEL = "adapterType";
  private static final String FUNCTION_LABEL = "functionName";
  private static final String RESOURCE_LABEL = "resource";
  private static final String SERVICE_LABEL = "service";

  public CompoundKnowledgeObject(ArkId arkId, String version) {
    this.arkId = arkId;
    basePath = Paths.get(arkId.getFedoraPath());
    versionPath = basePath.resolve(version);
    modelPath = versionPath.resolve(MODELS_DIR_NAME);
    resourcePath = modelPath.resolve(RESOURCE_DIR_NAME);
    servicePath = modelPath.resolve(SERVICE_DIR_NAME);
  }

  public Path getBaseDir() {
    return basePath;
  }

  public Path getVersionDir() {
    return versionPath;
  }

  public Path getModelDir() {
    return modelPath;
  }

  public Path getResourceDir() {
    return resourcePath;
  }

  public Path getServiceDir() {
    return servicePath;
  }

  @Override
  public Path getBaseMetadataLocation() {
    return versionPath.resolve(METADATA_FILENAME);
  }

  @Override
  public Path getModelMetadataLocation() {
    return modelPath.resolve(METADATA_FILENAME);
  }

  @Override
  public Path getResourceLocation() {
    return modelPath.resolve(getModelMetadata().get(RESOURCE_LABEL).asText());
  }

  @Override
  public Path getServiceLocation() {
    return modelPath.resolve(SERVICE_DIR_NAME);
  }

  @Override
  public ArkId getArkId() {
    return arkId;
  }

  @Override
  public String getVersion() {
    return koMetadata.get(VERSION_LABEL).asText();
  }

  @Override
  public String getAdapterType() {
    return getModelMetadata().get(ADAPTER_LABEL).asText();
  }

  @Override
  public void setMetadata(ObjectNode metadata) {
    this.koMetadata = metadata;
  }

  @Override
  public ObjectNode getMetadata() {
    return koMetadata;
  }

  @Override
  public ObjectNode getModelMetadata() {
    return (ObjectNode)koMetadata.get(MODELS_DIR_NAME);
  }

  @Override
  public void setModelMetadata(ObjectNode metadataNode) {
    this.koMetadata.set(MODELS_DIR_NAME, metadataNode);
  }

}
