package org.kgrid.shelf.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.validation.constraints.NotNull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArkId implements Comparable {

  private static final String ARK_FORMAT = "ark:/%s/%s/%s";
  private final String naan;
  private final String name;
  private final String version;

  public ArkId(String path) {
    String arkIdRegex = "ark:/(\\w+)/(\\w+)";
    Matcher arkIdMatcher = Pattern.compile(arkIdRegex).matcher(path);
    // Use [a-zA-Z0-9._\-]+ instead of just \w+ for the version because want to allow periods,
    // dashes and underscores in versions
    // Can't use \S for everything because it will capture the / between naan name and version
    String arkIdVersionRegex = "ark:/(\\w+)/(\\w+)/([a-zA-Z0-9._\\-]+)";
    Matcher arkIdVersionMatcher = Pattern.compile(arkIdVersionRegex).matcher(path);
    if (arkIdMatcher.matches()) {
      naan = arkIdMatcher.group(1);
      name = arkIdMatcher.group(2);
      version = null;
    } else if (arkIdVersionMatcher.matches()) {
      naan = arkIdVersionMatcher.group(1);
      name = arkIdVersionMatcher.group(2);
      version = arkIdVersionMatcher.group(3);
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

  public String getFullArk() {
    return String.format(ARK_FORMAT, naan, name, version);
  }

  @JsonIgnore
  @Deprecated
  public String getDashArk() {
    return naan + "-" + name;
  }

  @JsonIgnore
  @Deprecated
  public String getDashArkVersion() {
    return naan + "-" + name + "/" + version;
  }

  @JsonIgnore
  public String getFullDashArk() {
    if (version != null) {
      return naan + "-" + name + "-" + version;
    } else {
      return naan + "-" + name;
    }
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
