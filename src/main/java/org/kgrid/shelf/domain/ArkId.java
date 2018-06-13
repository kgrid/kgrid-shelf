package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ArkId {

  private String arkId;
  private String naan;
  private String name;

  public ArkId() {
  }

  public ArkId(String path) {
    setArkId(path);
  }

  public ArkId(String naan, String name) {
    this.naan = naan;
    this.name = name;
    this.arkId = String.format("ark:/%s", naan + "/" + name);
  }

  @JsonIgnore
  public String getFedoraPath() {

    if (naan == null) {
      return name;
    } else {
      return naan + "-" + name;
    }
  }

  @JsonIgnore
  public String getNaanName() {
    return naan + "/" + name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((naan == null) ? 0 : naan.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArkId other = (ArkId) obj;
    if (naan == null) {
      if (other.naan != null) {
        return false;
      }
    } else if (!naan.equals(other.naan)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return getArkId();
  }

  String getNaan() {
    return naan;
  }

  String getName() {
    return name;
  }

  public void setArkId(String path) {
    if (path.contains("ark:/")) {
      String[] parts = path.substring("ark:/".length()).split("/");
      this.naan = parts[0];
      this.name = parts[1];
      this.arkId = String.format("ark:/%s", naan + "/" + name);
    } else if (path.contains("-")) {
      String[] parts = path.split("-");
      this.naan = parts[0];
      this.name = parts[1];
      this.arkId = String.format("ark:/%s", naan + "/" + name);
    } else {
      throw new IllegalArgumentException("Cannot create ark id from " + path);
    }
  }

  public String getArkId() {
    return arkId;
  }

}
