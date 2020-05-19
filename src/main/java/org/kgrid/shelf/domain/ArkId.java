package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.validation.constraints.NotNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArkId implements Comparable {

  private static final String ARK_FORMAT = "ark:/%s/%s";
  private final String naan;
  private final String name;
  private final String version;

  /* TODO:
   *    - add static 'create(String path)' factory method for creation from existing path
   *    - remove all '..Slash..' methods (consider using only naan-name, even in urls)
   *    - make getId() return 'ark:/naan/name'
   *    - make getArk return 'naan-name'
   *    - make getVersion return 'naan-name/version'
   *    - make getEndpoint return 'naan-name/version/endpoint'
   *  TODO (consider):
   *    - add a builder
   *    - add a 'toURI()' method
   *    - add a 'with(...)' method to extend the path
   *    - add a 'server' parameter, 'withServer(...)' method, or static
   *      'withServer(ArkId, server)' method to support full urls
   *   .
   *   Note: all the above stay in URL/URI style forward slash style
   */

  /*
   * Can create an ark id with optional version from the following formats:
   * ark:/naan/name
   * ark:/naan-name
   * ark:/naan/name/version
   * ark:/naan-name/version
   */
  public ArkId(String path) {
    String arkIdRegex = "ark:/(\\w+)/(\\w+)";
    Matcher arkIdMatcher = Pattern.compile(arkIdRegex).matcher(path);
    String arkDirectoryRegex = "(\\w+)-(\\w+)";
    Matcher arkDirectoryMatcher = Pattern.compile(arkDirectoryRegex).matcher(path);
    String arkIdVersionRegex = "ark:/(\\w+)/(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkIdVersionMatcher = Pattern.compile(arkIdVersionRegex).matcher(path);
    String arkDirectoryVersionRegex = "(\\w+)-(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkDirectoryVersionMatcher = Pattern.compile(arkDirectoryVersionRegex).matcher(path);
    String arkHyphenVersionRegex = "(\\w+)-(\\w+)-([a-zA-Z0-9._\\-]+)";
    Matcher arkHyphenVersionMatcher = Pattern.compile(arkHyphenVersionRegex).matcher(path);
    if (arkIdMatcher.matches()) {
      naan = arkIdMatcher.group(1);
      name = arkIdMatcher.group(2);
      version = null;
    } else if (arkDirectoryMatcher.matches()) {
      naan = arkDirectoryMatcher.group(1);
      name = arkDirectoryMatcher.group(2);
      version = null;
    } else if (arkIdVersionMatcher.matches()) {
      naan = arkIdVersionMatcher.group(1);
      name = arkIdVersionMatcher.group(2);
      version = arkIdVersionMatcher.group(3);
    } else if (arkDirectoryVersionMatcher.matches()) {
      naan = arkDirectoryVersionMatcher.group(1);
      name = arkDirectoryVersionMatcher.group(2);
      version = arkDirectoryVersionMatcher.group(3);
    } else if (arkHyphenVersionMatcher.matches()) {
      naan = arkHyphenVersionMatcher.group(1);
      name = arkHyphenVersionMatcher.group(2);
      version = arkHyphenVersionMatcher.group(3);
    } else {
      throw new IllegalArgumentException("Cannot create ark id from " + path);
    }
  }

  public ArkId(String naan, String name) {
    this.naan = naan;
    this.name = name;
    version = null;
  }

  public ArkId(String naan, String name, String version) {
    this.naan = naan;
    this.name = name;
    this.version = version;
  }

  public static boolean isValid(String id) {
    try {
      new ArkId(id);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String getFullArk() {
    return String.format(ARK_FORMAT, naan, name);
  }

  @JsonIgnore
  public String getDashArk() {
    return naan + "-" + name;
  }

  @JsonIgnore
  public String getDashArkVersion() {
    return naan + "-" + name + "/" + version;
  }

  @JsonIgnore
  public String getSlashArk() {
    return naan + "/" + name;
  }

  @JsonIgnore
  public String getSlashArkVersion() {
    return StringUtils.join(new String[] {naan, name, version}, "/");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ArkId arkId = (ArkId) o;

    return new EqualsBuilder()
        .append(naan, arkId.naan)
        .append(name, arkId.name)
        .append(version, arkId.version)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(naan).append(name).append(version).toHashCode();
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

  public String getVersion() {
    return StringUtils.isEmpty(version) ? "" : version;
  }

  public boolean hasVersion() {
    return version != null;
  }

  @Override
  public int compareTo(@NotNull Object o) {
    if (this == o) {
      return 0;
    }
    ArkId that = (ArkId) o;

    if (getNaan().compareTo(that.getNaan()) != 0) {
      return getNaan().compareTo(that.getNaan());
    }

    if (getName().compareTo(that.getName()) != 0) {
      return getName().compareTo(that.getName());
    }

    if (getVersion() != null && that.getVersion() != null) {
      if (getVersion().compareTo(that.getVersion()) != 0) {
        return getVersion().compareTo(that.getVersion());
      }
    }
    return 0;
  }
}
