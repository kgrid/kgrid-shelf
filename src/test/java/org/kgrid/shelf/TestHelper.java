package org.kgrid.shelf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.zeroturnaround.zip.ZipUtil.pack;

public class TestHelper {
  public static final String SERVICE_YAML_PATH = "service.yaml";
  public static final String DEPLOYMENT_YAML_PATH = "deployment.yaml";
  public static final String PAYLOAD_PATH = "src/index.js";
  public static final String NAAN = "naan";
  public static final String NAME = "name";
  public static final String VERSION_1 = "1";
  public static final String KO_PATH_V1 = NAAN + "-" + NAME + "-" + VERSION_1;
  public static final ArkId ARK_ID_V1 = new ArkId(NAAN, NAME, VERSION_1);
  public static final String VERSION_2 = "2";
  public static final String KO_PATH_V2 = NAAN + "-" + NAME + "-" + VERSION_2;
  public static final ArkId ARK_ID_V2 = new ArkId(NAAN, NAME, VERSION_2);
  public static final String GOOD_MANIFEST_PATH = "http://example.com/folder/manifest.json";
  public static final String BAD_MANIFEST_PATH = "asdfkujnhdsfa";
  public static final String RELATIVE_RESOURCE_URI = "resource_1_uri.zip";
  public static final String ABSOLUTE_RESOURCE_URI = "http://example.com/folder/resource_2_uri.zip";
  public static final String RESOLVED_RELATIVE_RESOURCE_URI =
      "http://example.com/folder/resource_1_uri.zip";
  public static final byte[] DEPLOYMENT_BYTES =
      ("/welcome:\n    artifact: " + PAYLOAD_PATH + "\n    function: welcome\n")
          .getBytes();
  public static final byte[] SERVICE_BYTES =
      ("/welcome:\n    post:\n      x-kgrid-activation:\n        artifact:\n            - "
              + PAYLOAD_PATH
              + "\n")
          .getBytes();

  public static ByteArrayInputStream packZipForImport(
      byte[] metadata, byte[] deploymentSpec, byte[] serviceSpec, byte[] payload) {
    return new ByteArrayInputStream(
        packZipForExport(metadata, deploymentSpec, serviceSpec, payload).toByteArray());
  }

  public static ByteArrayOutputStream packZipForExport(
      byte[] metadata, byte[] deploymentSpec, byte[] serviceSpec, byte[] payload) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> filesToBeZipped = new ArrayList<>();
    if (deploymentSpec != null && deploymentSpec.length != 0) {
      filesToBeZipped.add(
          new ByteSource(KO_PATH_V1 + File.separator + DEPLOYMENT_YAML_PATH, deploymentSpec));
    }
    if (serviceSpec != null && serviceSpec.length != 0) {
      filesToBeZipped.add(
          new ByteSource(KO_PATH_V1 + File.separator + SERVICE_YAML_PATH, serviceSpec));
    }
    if (payload != null && payload.length != 0) {
      filesToBeZipped.add(new ByteSource(KO_PATH_V1 + File.separator + PAYLOAD_PATH, payload));
    }
    if (metadata != null && metadata.length != 0) {
      filesToBeZipped.add(
          new ByteSource(KO_PATH_V1 + File.separator + KoFields.METADATA_FILENAME.asStr(), metadata));
    }
    pack(filesToBeZipped.toArray(new ZipEntrySource[0]), outputStream);
    return outputStream;
  }

  public static JsonNode generateMetadata(
      String serviceYamlPath,
      String deploymentYamlPath,
      boolean hasAtId,
      boolean hasIdentifier,
      boolean hasVersion,
      boolean hasType) {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    if (hasAtId) {
      metadata.put("@id", KO_PATH_V1);
    }
    if (hasType) {
      metadata.put("@type", "koio:KnowledgeObject");
    }
    if (hasIdentifier) {
      metadata.put("identifier", ARK_ID_V1.toString());
    }
    if (hasVersion) {
      metadata.put(KoFields.VERSION.asStr(), VERSION_1);
    }
    if (deploymentYamlPath != null) {
      metadata.put(KoFields.DEPLOYMENT_SPEC_TERM.asStr(), deploymentYamlPath);
    }
    if (serviceYamlPath != null) {
      metadata.put(KoFields.SERVICE_SPEC_TERM.asStr(), serviceYamlPath);
    }
    return metadata;
  }
  public static JsonNode generateMetadata(String id, String identifier, String version) {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
      metadata.put("@id", id);
      metadata.put("@type", "koio:KnowledgeObject");
      metadata.put("identifier", identifier);
      metadata.put(KoFields.VERSION.asStr(), version);
      metadata.put(KoFields.DEPLOYMENT_SPEC_TERM.asStr(), DEPLOYMENT_YAML_PATH);
      metadata.put(KoFields.SERVICE_SPEC_TERM.asStr(), SERVICE_YAML_PATH);
    return metadata;
  }
  public static JsonNode generateMetadata() {
    return generateMetadata(SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
  }

  public static ObjectNode getManifestNode() {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    ArrayNode uris = node.putArray("manifest");
    uris.add(RELATIVE_RESOURCE_URI);
    uris.add(ABSOLUTE_RESOURCE_URI);
    return node;
  }

  public static ArrayNode getManifestListNode() {
    ArrayNode node = JsonNodeFactory.instance.arrayNode();
    node.add(GOOD_MANIFEST_PATH);
    node.add(BAD_MANIFEST_PATH);
    return node;
  }

  public static ObjectNode getManifestNodeWithBadUri() {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    ArrayNode uris = node.putArray("manifest");
    uris.add(RELATIVE_RESOURCE_URI);
    uris.add("bad uri to ko.zip");
    uris.add(ABSOLUTE_RESOURCE_URI);
    return node;
  }
}
