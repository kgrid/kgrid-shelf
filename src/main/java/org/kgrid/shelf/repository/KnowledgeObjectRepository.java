package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    dataStore.delete(resolveArkIdToLocation(arkId));
    log.info("Deleted ko with ark id " + arkId);
  }

  /**
   * Edit the KO
   *
   * @param arkId arkId
   * @param metadata metadata
   * @return metadata
   */
  public ObjectNode editMetadata(ArkId arkId, String metadata) {
    Path metadataPath;
    metadataPath = Paths.get(resolveArkIdToLocation(arkId), KoFields.METADATA_FILENAME.asStr());
    JsonNode jsonMetadata;
    try {
      jsonMetadata = new ObjectMapper().readTree(metadata);
    } catch (JsonProcessingException e) {
      throw new ShelfException("Cannot parse new metadata", e);
    }

    dataStore.saveMetadata(jsonMetadata, metadataPath.toString());

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

    String koPath = resolveArkIdToLocation(arkId);
    outputStream.write(zipExportService.exportObject(arkId, koPath, dataStore).toByteArray());
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

    JsonNode node = findKnowledgeObjectMetadata(arkId);

    return findDeploymentSpecification(arkId, node);
  }

  /**
   * Find the Deployment Specification for the version, if not found throw shelf exception
   *
   * @param arkId Ark ID for the version
   * @param metadata version
   * @return JsonNode deployment specification
   */
  public JsonNode findDeploymentSpecification(ArkId arkId, JsonNode metadata) {

    if (metadata.has(KoFields.DEPLOYMENT_SPEC_TERM.asStr())) {

      String deploymentSpecPath =
          metadata.findValue(KoFields.DEPLOYMENT_SPEC_TERM.asStr()).asText();

      String uriPath = Paths.get(resolveArkIdToLocation(arkId), deploymentSpecPath).toString();

      return loadSpecificationNode(arkId, uriPath);

    } else {
      throw new ShelfException(
          "Deployment specification not found in metadata for object " + arkId.getDashArkVersion());
    }
  }

  public JsonNode findKnowledgeObjectMetadata(ArkId arkId) {

    if (arkId == null) {
      throw new IllegalArgumentException("Cannot find metadata for null ark id");
    }
    Map<String, String> versionMap = objectLocations.get(arkId.getDashArk());
    if (versionMap == null) {
      throw new ShelfException("Object location not found for ark id " + arkId.getDashArkVersion());
    }

    if (!arkId.hasVersion()) {
      ArrayNode node = new ObjectMapper().createArrayNode();
      versionMap.forEach((version, location) -> node.add(dataStore.getMetadata(location)));
      return node;
    }
    String nodeLoc = versionMap.get(arkId.getVersion());
    if (nodeLoc == null) {
      throw new ShelfException(
          "Cannot load metadata, " + arkId.getDashArkVersion() + " not found on shelf");
    }
    return dataStore.getMetadata(nodeLoc);
  }

  /**
   * Find the Service Specification for the version
   *
   * @param arkId Ark ID for the version
   * @param versionNode version
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId, JsonNode versionNode) {

    JsonNode serviceSpecNode = versionNode.findValue(KoFields.SERVICE_SPEC_TERM.asStr());
    if (serviceSpecNode == null) {
      throw new ShelfException(
          "Metadata for "
              + arkId
              + " is missing a \""
              + KoFields.SERVICE_SPEC_TERM.asStr()
              + "\" field.");
    }
    String serviceSpecPath = serviceSpecNode.asText();

    String uriPath;
    if (arkId.hasVersion()) {
      uriPath = Paths.get(resolveArkIdToLocation(arkId), serviceSpecPath).toString();

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

    return findServiceSpecification(arkId, findKnowledgeObjectMetadata(arkId));
  }

  public byte[] getBinary(ArkId arkId, String childPath) {
    return this.dataStore.getBinary(resolveArkIdToLocation(arkId), childPath);
  }

  private String resolveArkIdToLocation(ArkId arkId) {
    if (objectLocations.get(arkId.getDashArk()) == null
        || objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()) == null) {
      throw new ShelfException("Cannot resolve " + arkId + " to a location in the KO repository");
    }
    return objectLocations.get(arkId.getDashArk()).get(arkId.getVersion());
  }

  public String getKoRepoLocation() {

    return this.dataStore.getAbsoluteLocation("");
  }

  public ArkId importZip(MultipartFile zippedKO) {
    try {
      ArkId arkId = zipImportService.importKO(zippedKO.getInputStream(), dataStore);
      refreshObjectMap();
      return arkId;
    } catch (IOException e) {
      throw new ShelfException("Cannot load zip file with filename " + zippedKO.getName(), e);
    }
  }

  public ArkId importZip(InputStream zipStream) {

    ArkId arkId = zipImportService.importKO(zipStream, dataStore);
    refreshObjectMap();
    return arkId;
  }

  // Used by activator
  public String getObjectLocation(ArkId arkId) {
    // Reload for activation use cases
    if (objectLocations.get(arkId.getDashArk()) == null
        || objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()) == null) {
      refreshObjectMap();
    }
    return resolveArkIdToLocation(arkId);
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
      return yamlMapper.readTree(dataStore.getBinary(uriPath));

    } catch (IOException exception) {
      throw new ShelfException(
          "Could not parse service specification for " + arkId.getDashArkVersion(), exception);
    }
  }

  public void refreshObjectMap() {
    objectLocations.clear();
    knowledgeObjects.clear();

    // Load KO objects and skip any KOs with exceptions like missing metadata
    for (String path : dataStore.getChildren("")) {
      try {
        ArkId arkId;
        String koLocation;

        if (path.contains(File.separator)) {
          koLocation = StringUtils.substringAfterLast(path, File.separator);
        } else {
          koLocation = path;
        }

        JsonNode metadata = dataStore.getMetadata(koLocation);
        if (!metadata.has(KoFields.IDENTIFIER.asStr())) {
          log.warn("Folder with metadata " + koLocation + " is missing an @id field, cannot load.");
          continue;
        }

        if (!metadata.has(KoFields.VERSION.asStr())) {
          log.warn(
              "Folder with metadata "
                  + koLocation
                  + " is missing a version field, will default to reverse alphabetical lookup");
          arkId = new ArkId(metadata.get(KoFields.IDENTIFIER.asStr()).asText());
        } else {
          arkId =
              new ArkId(
                  metadata.get(KoFields.IDENTIFIER.asStr()).asText()
                      + "/"
                      + metadata.get(KoFields.VERSION.asStr()).asText());
        }

        if (objectLocations.get(arkId.getDashArk()) != null
            && objectLocations.get(arkId.getDashArk()).get(arkId.getVersion()) != null) {
          log.warn(
              "Two objects on the shelf have the same ark id: "
                  + arkId
                  + " Check folders "
                  + koLocation
                  + " and "
                  + resolveArkIdToLocation(arkId));
        }

        if (objectLocations.get(arkId.getDashArk()) == null) {
          Map<String, String> versionMap = new TreeMap<>(Collections.reverseOrder());
          versionMap.put(arkId.getVersion(), koLocation);
          objectLocations.put(arkId.getDashArk(), versionMap);
        } else {
          objectLocations.get(arkId.getDashArk()).put(arkId.getVersion(), koLocation);
        }

        knowledgeObjects.put(arkId, metadata);

      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
  }
}
