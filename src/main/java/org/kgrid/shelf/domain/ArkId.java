package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ArkId {

  private static final String ARK_FORMAT = "ark:/%s/%s";
  private String arkId;
  private String naan;
  private String name;
  private String implementation;

  public ArkId(String path) {
    setArkId(path);
  }

  public ArkId(String naan, String name) {
    this.naan = naan;
    this.name = name;
    this.arkId = String.format("ark:/%s", naan + "/" + name);
  }

  public ArkId(String naan, String name, String implementation) {
    this.naan = naan;
    this.name = name;
    this.implementation = implementation;
    this.arkId = String.format("ark:/%s", naan + "/" + name);
  }

  public ArkId() {

  }

  public String getFullArk() {
    return arkId;
  }

  @JsonIgnore
  public String getDashArk() {
     return naan + "-" + name;
  }

  @JsonIgnore
  public String getDashArkImplementation() {
    return naan + "-" + name + "/" + implementation;
  }

  @JsonIgnore
  public String getSlashArk() {
    return naan + "/" + name;
  }

  @JsonIgnore
  public String getSlashArkImplementation() {
    return StringUtils.join(naan, "/", name, "/", implementation);
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
    return getFullArk();
  }

  public String getNaan() {
    return naan;
  }

  public String getName() {
    return name;
  }

  public String getImplementation() {
    return implementation;
  }

  /*
   * Can create an ark id with optional implementation from the following formats:
   * ark:/naan/name
   * ark:/naan-name
   * ark:/naan/name/implementation
   * ark:/naan-name/implementation
   */
  public void setArkId(String path) {
    String arkIdRegex = "ark:/(\\w+)/(\\w+)";
    Matcher arkIdMatcher = Pattern.compile(arkIdRegex).matcher(path);
    String arkDirectoryRegex = "(\\w+)-(\\w+)";
    Matcher arkDirectoryMatcher = Pattern.compile(arkDirectoryRegex).matcher(path);
    String arkIdImplementationRegex = "ark:/(\\w+)/(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkIdImplementationMatcher = Pattern.compile(arkIdImplementationRegex).matcher(path);
    String arkDirectoryImplementationRegex = "(\\w+)-(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkDirectoryImplementationMatcher = Pattern.compile(arkDirectoryImplementationRegex).matcher(path);
    if (arkIdMatcher.matches()) {
      this.naan = arkIdMatcher.group(1);
      this.name = arkIdMatcher.group(2);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if (arkDirectoryMatcher.matches()) {
      this.naan = arkDirectoryMatcher.group(1);
      this.name = arkDirectoryMatcher.group(2);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if (arkIdImplementationMatcher.matches()) {
      this.naan = arkIdImplementationMatcher.group(1);
      this.name = arkIdImplementationMatcher.group(2);
      this.implementation = arkIdImplementationMatcher.group(3);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if(arkDirectoryImplementationMatcher.matches()) {
      this.naan = arkIdImplementationMatcher.group(1);
      this.name = arkIdImplementationMatcher.group(2);
      this.implementation = arkIdImplementationMatcher.group(3);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else {
      throw new IllegalArgumentException("Cannot create ark id from " + path);
    }
  }

}
