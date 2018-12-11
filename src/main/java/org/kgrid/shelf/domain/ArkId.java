package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class ArkId {

  private static final String ARK_FORMAT = "ark:/%s/%s";
  private final String naan;
  private final String name;
  private final String implementation;

  /* TODO:
  *    - add static 'create(String path)' factory method for creation from existing path
  *    - remove all '..Slash..' methods (consider using only naan-name, even in urls)
  *    - make getId() return 'ark:/naan/name'
  *    - make getArk return 'naan-name'
  *    - make getImplementation return 'naan-name/version'
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
   * Can create an ark id with optional implementation from the following formats:
   * ark:/naan/name
   * ark:/naan-name
   * ark:/naan/name/implementation
   * ark:/naan-name/implementation
   */
  public ArkId(String path) {
    String arkIdRegex = "ark:/(\\w+)/(\\w+)";
    Matcher arkIdMatcher = Pattern.compile(arkIdRegex).matcher(path);
    String arkDirectoryRegex = "(\\w+)-(\\w+)";
    Matcher arkDirectoryMatcher = Pattern.compile(arkDirectoryRegex).matcher(path);
    String arkIdImplementationRegex = "ark:/(\\w+)/(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkIdImplementationMatcher = Pattern.compile(arkIdImplementationRegex).matcher(path);
    String arkDirectoryImplementationRegex = "(\\w+)-(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkDirectoryImplementationMatcher = Pattern.compile(arkDirectoryImplementationRegex).matcher(
        path);
    if (arkIdMatcher.matches()) {
      this.naan = arkIdMatcher.group(1);
      this.name = arkIdMatcher.group(2);
      this.implementation = null;
    } else if (arkDirectoryMatcher.matches()) {
      this.naan = arkDirectoryMatcher.group(1);
      this.name = arkDirectoryMatcher.group(2);
      this.implementation = null;
    } else if (arkIdImplementationMatcher.matches()) {
      this.naan = arkIdImplementationMatcher.group(1);
      this.name = arkIdImplementationMatcher.group(2);
      this.implementation = arkIdImplementationMatcher.group(3);
    } else if(arkDirectoryImplementationMatcher.matches()) {
      this.naan = arkDirectoryImplementationMatcher.group(1);
      this.name = arkDirectoryImplementationMatcher.group(2);
      this.implementation = arkDirectoryImplementationMatcher.group(3);
    } else {
      throw new IllegalArgumentException("Cannot create ark id from " + path);
    }
  }

  public ArkId(String naan, String name) {
    this.naan = naan;
    this.name = name;
    this.implementation = null;
  }

  public ArkId(String naan, String name, String implementation) {
    this.naan = naan;
    this.name = name;
    this.implementation = implementation;
  }

  public String getFullArk() {
    return String.format(ARK_FORMAT, naan, name);
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
        .append(implementation, arkId.implementation)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(naan)
        .append(name)
        .append(implementation)
        .toHashCode();
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

  public boolean isImplementation(){
    return implementation!=null;
  }

}
