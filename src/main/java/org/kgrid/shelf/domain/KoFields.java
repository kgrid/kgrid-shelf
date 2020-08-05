package org.kgrid.shelf.domain;

public enum KoFields {
  METADATA_FILENAME("metadata.json"),
  SERVICE_SPEC_TERM("hasServiceSpecification"),
  DEPLOYMENT_SPEC_TERM("hasDeploymentSpecification"),
  ARTIFACT("artifact"),
  SERVICE_ACTIVATION_KEY("x-kgrid-activation"),
  VERSION("version"),
  IDENTIFIER("identifier");

  private final String fieldName;

  KoFields(String fieldName) {
    this.fieldName = fieldName;
  }

  public String asStr() {
    return fieldName;
  }
}
