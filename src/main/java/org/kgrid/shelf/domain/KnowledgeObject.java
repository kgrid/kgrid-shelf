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

  public static final String METADATA_FILENAME = "metadata.json";
  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";
  public static final String PAYLOAD_TERM = "hasPayload";


  public KnowledgeObject(ArkId arkId) {
    this.arkId = arkId;
  }

  @JsonIgnore
  public ArkId getArkId() {
    return arkId;
  }

  public void setMetadata(ObjectNode metadata) {
    this.metadata = metadata;
  }

  public ObjectNode getMetadata() {
    return metadata;
  }

}
