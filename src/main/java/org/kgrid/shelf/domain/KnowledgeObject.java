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

  public static final String METADATA_FILENAME = "metadata.json";

  public KnowledgeObject(ArkId arkId, String version) {
    this.arkId = arkId;
    basePath = Paths.get(arkId.getAsSimpleArk());
    versionPath = basePath.resolve(version);
  }

  public Path baseMetadataLocation() {
    return versionPath.resolve(METADATA_FILENAME);
  }

  @JsonIgnore
  public ArkId getArkId() {
    return arkId;
  }

  public String version() {
    return versionPath.getFileName().toString();
  }

  public void setMetadata(ObjectNode metadata) {
    this.metadata = metadata;
  }

  public ObjectNode getMetadata() {
    return metadata;
  }

}
