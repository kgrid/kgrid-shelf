package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ArkId {

  public static final String ARK_FORMAT = "ark:/%s/%s";
  private String arkId;
  private String naan;
  private String name;
  private String version;

  public ArkId(String path) {
    setArkId(path);
  }

  public ArkId(String naan, String name) {
    this.naan = naan;
    this.name = name;
    this.arkId = String.format("ark:/%s", naan + "/" + name);
  }

  public ArkId(String naan, String name, String version) {
    this.naan = naan;
    this.name = name;
    this.version = version;
    this.arkId = String.format("ark:/%s", naan + "/" + name);
  }

  public ArkId() {

  }

  @JsonIgnore
  public String getAsSimpleArk() {
     return naan + "-" + name;
  }

  public String getAsFullArk() {
    return arkId;
  }

  @JsonIgnore
  public String getAsSimpleArkAndVersion() {
    return naan + "-" + name + "/" + version;
  }

  @JsonIgnore
  public String getNaanName() {
    return naan + "/" + name;
  }

  @JsonIgnore
  public String getNaanNameVersion() {
    return StringUtils.join(naan, "/", name, "/", version);
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
    return getAsFullArk();
  }

  String getNaan() {
    return naan;
  }

  String getName() {
    return name;
  }

  String getVersion() {
    return version;
  }

  /*
   * Can create an ark id with optional version from the following formats:
   * ark:/naan/name
   * ark:/naan-name
   * ark:/naan/name/version
   * ark:/naan-name/version
   */
  public void setArkId(String path) {
    String arkIdRegex = "ark:/(\\w+)/(\\w+)";
    Matcher arkIdMatcher = Pattern.compile(arkIdRegex).matcher(path);
    String arkDirectoryRegex = "(\\w+)-(\\w+)";
    Matcher arkDirectoryMatcher = Pattern.compile(arkDirectoryRegex).matcher(path);
    String arkIdVersionRegex = "ark:/(\\w+)/(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkIdVersionMatcher = Pattern.compile(arkIdVersionRegex).matcher(path);
    String arkDirectoryVersionRegex = "(\\w+)-(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkDirectoryVersionMatcher = Pattern.compile(arkDirectoryVersionRegex).matcher(path);
    if (arkIdMatcher.matches()) {
      this.naan = arkIdMatcher.group(1);
      this.name = arkIdMatcher.group(2);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if (arkDirectoryMatcher.matches()) {
      this.naan = arkDirectoryMatcher.group(1);
      this.name = arkDirectoryMatcher.group(2);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if (arkIdVersionMatcher.matches()) {
      this.naan = arkIdVersionMatcher.group(1);
      this.name = arkIdVersionMatcher.group(2);
      this.version = arkIdVersionMatcher.group(3);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else if(arkDirectoryVersionMatcher.matches()) {
      this.naan = arkIdVersionMatcher.group(1);
      this.name = arkIdVersionMatcher.group(2);
      this.version = arkIdVersionMatcher.group(3);
      this.arkId = String.format(ARK_FORMAT, naan, name);
    } else {
      throw new IllegalArgumentException("Cannot create ark id from " + path);
    }
  }

}
