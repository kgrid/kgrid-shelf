package org.kgrid.shelf.domain;

public enum KnowledgeObjectFields {
  METADATA_FILENAME("metadata.json"),
  SERVICE_SPEC_TERM("hasServiceSpecification"),
  DEPLOYMENT_SPEC_TERM("hasDeploymentSpecification");

  private final String fieldName;

  KnowledgeObjectFields(String fieldName) {
    this.fieldName = fieldName;
  }

  public String asStr() {
    return fieldName;
  }
}
