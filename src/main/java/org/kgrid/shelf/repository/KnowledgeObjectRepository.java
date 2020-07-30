package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObjectFields;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class KnowledgeObjectRepository {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);
  private CompoundDigitalObjectStore dataStore;
  private ZipImportService zipImportService;
  private ZipExportService zipExportService;
  // Map of Ark -> Version -> File Location for rapid object lookup
  private static final Map<String, Map<String, String>> objectLocations = new HashMap<>();
  // Map of Ark -> Metadata location for displaying to end user
  private static final Map<ArkId, JsonNode> knowledgeObjects = new HashMap<>();

  @Autowired
  KnowledgeObjectRepository(
      CompoundDigitalObjectStore compoundDigitalObjectStore,
      ZipImportService zis,
      ZipExportService zes) {
    this.dataStore = compoundDigitalObjectStore;
    this.zipImportService = zis;
    this.zipExportService = zes;
    // Initialize the map of folder names -> ark ids
    refreshObjectMap();
  }

  public void delete(ArkId arkId) {

    dataStore.delete(objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()));
    log.info("Deleted ko with ark id " + arkId);
  }

  /**
   * Edit the KO
   *
   * @param arkId arkId
   * @param path path
   * @param metadata metadata
   * @return metadata
   */
  public ObjectNode editMetadata(ArkId arkId, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath =
          Paths.get(
              objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()),
              path,
              KnowledgeObjectFields.METADATA_FILENAME.asStr());
    } else {
      metadataPath =
          Paths.get(
              objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()),
              KnowledgeObjectFields.METADATA_FILENAME.asStr());
    }
    try {
      JsonNode jsonMetadata = new ObjectMapper().readTree(metadata);

      dataStore.saveMetadata(jsonMetadata, metadataPath.toString());

    } catch (IOException e) {
      log.error("Cannot edit metadata at " + metadataPath + " " + e);
    }
    return dataStore.getMetadata(metadataPath.toString());
  }

  /**
   * Extract ZIP file of the KO
   *
   * @param arkId ark id of the object
   * @param outputStream zipped file in outputstream
   * @throws IOException if the system can't extract the zip file to the filesystem
   */
  public void extractZip(ArkId arkId, OutputStream outputStream) throws IOException {

    String koPath = objectLocations.get(arkId.getDashArk()).get(arkId.getVersion());
    outputStream.write(zipExportService.exportObject(arkId, koPath, dataStore).toByteArray());
  }

  private void refreshObjectMap() {
    objectLocations.clear();
    knowledgeObjects.clear();

    // Load KO objects and skip any KOs with exceptions like missing metadata
    for (String path : dataStore.getChildren("")) {
      try {
        ArkId arkId;
        String repoLocation;

        if (path.contains(File.separator)) {
          repoLocation = StringUtils.substringAfterLast(path, File.separator);
        } else {
          repoLocation = path;
        }

        JsonNode metadata = dataStore.getMetadata(repoLocation);
        if (!metadata.has("identifier")) {
          log.warn(
              "Folder with metadata " + repoLocation + " is missing an @id field, cannot load.");
          continue;
        }

        if (!metadata.has("version")) {
          log.warn(
              "Folder with metadata "
                  + repoLocation
                  + " is missing a version field, will default to reverse alphabetical lookup");
          arkId = new ArkId(metadata.get("identifier").asText());
        } else {
          arkId =
              new ArkId(
                  metadata.get("identifier").asText() + "/" + metadata.get("version").asText());
        }

        if (objectLocations.get(arkId.getDashArk()) != null
            && objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()) != null) {
          log.warn(
              "Two objects on the shelf have the same ark id: "
                  + arkId
                  + " Check folders "
                  + repoLocation
                  + " and "
                  + objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()));
        }

        if (objectLocations.get(arkId.getDashArk()) == null) {
          Map<String, String> versionMap = new TreeMap<>(Collections.reverseOrder());
          versionMap.put(arkId.getVersion(), repoLocation);
          objectLocations.put(arkId.getDashArk(), versionMap);
        } else {
          objectLocations.get(arkId.getDashArk()).put(arkId.getVersion(), repoLocation);
        }

        knowledgeObjects.put(arkId, metadata);

      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
  }

  public Map<ArkId, JsonNode> findAll() {
    refreshObjectMap();
    return knowledgeObjects;
  }

  /**
   * Find the deployment specification based on version ark id
   *
   * @param arkId version ark id
   * @return JsonNode deployment specification
   */
  public JsonNode findDeploymentSpecification(ArkId arkId) {

    log.info("find deployment specification for  " + arkId.getDashArkVersion());

    JsonNode node = findKnowledgeObjectMetadata(arkId);
    if (node.isArray()) {
      node = node.get(0);
      log.warn(
          "Finding deployment spec for array of objects, looking up first object with version "
              + node.get("version").asText());
    }

    return findDeploymentSpecification(arkId, node);
  }

  /**
   * Find the Deployment Specification for the version, if not found throw shelf exception
   *
   * @param arkId Ark ID for the version
   * @param versionNode version
   * @return JsonNode deployment specification
   */
  public JsonNode findDeploymentSpecification(ArkId arkId, JsonNode versionNode) {

    if (versionNode.has(KnowledgeObjectFields.DEPLOYMENT_SPEC_TERM.asStr())) {

      String deploymentSpecPath =
          versionNode.findValue(KnowledgeObjectFields.DEPLOYMENT_SPEC_TERM.asStr()).asText();

      String uriPath =
          ResourceUtils.isUrl(deploymentSpecPath)
              ? deploymentSpecPath
              : Paths.get(
                      objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()),
                      deploymentSpecPath)
                  .toString();

      if (uriPath.startsWith("$.")) {
        log.warn("Cannot load deployment spec starting with $ yet");
        return null;
      }
      return loadSpecificationNode(arkId, uriPath);

    } else {

      throw new ShelfException("deployment specification not found in metadata");
    }
  }

  public JsonNode findKnowledgeObjectMetadata(ArkId arkId) {

    if (!arkId.hasVersion()) {
      ArrayNode node = new ObjectMapper().createArrayNode();
      objectLocations
          .get(arkId.getDashArk())
          .forEach(
              (version, location) -> {
                node.add(dataStore.getMetadata(location));
              });
      return node;
    }
    String nodeLoc = objectLocations.get(arkId.getDashArk()).get(arkId.getVersion());
    if (nodeLoc == null) {
      throw new ShelfResourceNotFound(
          "Cannot load metadata, " + arkId.getDashArkVersion() + " not found on shelf");
    }
    return dataStore.getMetadata(nodeLoc);
  }

  public byte[] findPayload(ArkId arkId, String versionPath) {

    String payloadPath =
        Paths.get(objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()), versionPath)
            .toString();

    log.info("find payload for  " + payloadPath);

    return dataStore.getBinary(payloadPath);
  }

  /**
   * Find the Service Specification for the version
   *
   * @param arkId Ark ID for the version
   * @param versionNode version
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId, JsonNode versionNode) {

    String serviceSpecPath =
        versionNode.findValue(KnowledgeObjectFields.SERVICE_SPEC_TERM.asStr()).asText();

    log.info("find service specification at " + serviceSpecPath);

    if (ResourceUtils.isUrl(serviceSpecPath)) {
      return loadSpecificationNode(arkId, serviceSpecPath);
    }

    String uriPath;
    if (arkId.hasVersion()) {
      uriPath =
          Paths.get(
                  objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()), serviceSpecPath)
              .toString();

    } else {
      uriPath =
          Paths.get(
                  objectLocations.get(arkId.getDashArk()).values().toArray()[0].toString(),
                  serviceSpecPath)
              .toString();
    }
    return loadSpecificationNode(arkId, uriPath);
  }

  /**
   * Find the Service Specification for the version
   *
   * @param arkId Ark ID for the version
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId) {

    log.info("find service specification for " + arkId.getDashArkVersion());

    JsonNode node = findKnowledgeObjectMetadata(arkId);
    if (node.isArray()) {
      node = node.get(0);
      log.warn(
          "Finding deployment spec for array of objects, looking up first object with version "
              + node.get("version").asText());
    }

    return findServiceSpecification(arkId, node);
  }

  public byte[] getBinary(ArkId arkId, String childPath) {
    String filepath =
        Paths.get(objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()), childPath)
            .toString();
    return this.dataStore.getBinary(filepath);
  }

  public String getConnection() {

    return this.dataStore.getAbsoluteLocation("");
  }

  public JsonNode getMetadataAtPath(ArkId arkId, String path) {
    return dataStore.getMetadata(
        objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()), path);
  }

  public ArkId importZip(MultipartFile zippedKO) {
    try {

      ArkId arkId = zipImportService.importKO(zippedKO.getInputStream(), dataStore);
      refreshObjectMap();
      return arkId;
    } catch (IOException e) {
      log.warn("Cannot load zip file with filename " + zippedKO.getName());
    }
    return null;
  }

  public ArkId importZip(InputStream zipStream) {

    ArkId arkId = zipImportService.importKO(zipStream, dataStore);
    if (objectLocations.get(arkId.getDashArk()) != null) {
      objectLocations
          .get(arkId.getDashArk())
          .put(arkId.getVersion(), arkId.getDashArk() + "-" + arkId.getVersion());
    } else {
      Map<String, String> version = new HashMap<>();
      version.put(arkId.getVersion(), arkId.getDashArk() + "-" + arkId.getVersion());
      objectLocations.put(arkId.getDashArk(), version);
    }
    refreshObjectMap();
    return arkId;
  }

  public String getObjectLocation(ArkId arkId) {
    // Reload for activation use cases
    if (objectLocations.get(arkId.getDashArk()) == null) {
      refreshObjectMap();
    }
    return objectLocations.get(arkId.getDashArk()).get(arkId.getVersion());
  }

  /**
   * Loads a YMAL specification file (service or deployment) and maps to a JSON node
   *
   * @param arkId version ark id
   * @param uriPath path to specification file
   * @return JsonNode representing YMAL specification file
   */
  protected JsonNode loadSpecificationNode(ArkId arkId, String uriPath) {
    try {

      YAMLMapper yamlMapper = new YAMLMapper();
      JsonNode serviceSpecNode = yamlMapper.readTree(dataStore.getBinary(uriPath));

      return serviceSpecNode;

    } catch (IOException exception) {
      throw new ShelfException(
          "Could not parse service specification for " + arkId.getDashArkVersion(), exception);
    }
  }
}
