package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class KnowledgeObjectRepository {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);
  private final CompoundDigitalObjectStore cdoStore;
  private static final Map<String, Map<String, URI>> objectLocations = new HashMap<>();
  private static final Map<ArkId, JsonNode> knowledgeObjects = new HashMap<>();

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStore compoundDigitalObjectStore) {
    cdoStore = compoundDigitalObjectStore;
    refreshObjectMap();
  }

  public void delete(ArkId arkId) {
    cdoStore.delete(resolveArkIdToLocation(arkId));
    knowledgeObjects.remove(arkId);
    Map<String, URI> versionMap = objectLocations.get(arkId.getSlashArk());
    if(versionMap.size()>1){
      versionMap.remove(arkId.getVersion());
    } else {
      objectLocations.remove(arkId.getSlashArk());
    }
    log.info("Deleted ko with ark id " + arkId);
  }

  /**
   * Edit the KO
   *
   * @param arkId arkId
   * @param metadata metadata
   * @return metadata metadata
   */
  public ObjectNode editMetadata(ArkId arkId, String metadata) {
    URI metadataLocation =
        resolveArkIdToLocation(arkId).resolve(KoFields.METADATA_FILENAME.asStr());
    JsonNode jsonMetadata;
    try {
      jsonMetadata = new ObjectMapper().readTree(metadata);
    } catch (JsonProcessingException e) {
      throw new ShelfException("Cannot parse new metadata", e);
    }

    cdoStore.saveMetadata(jsonMetadata, metadataLocation);

    return cdoStore.getMetadata(metadataLocation);
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

      URI uriPath = resolveArkIdToLocation(arkId).resolve(deploymentSpecPath);

      return loadSpecificationNode(arkId, uriPath);

    } else {
      throw new ShelfException(
          "Deployment specification not found in metadata for object " + arkId.getFullArk());
    }
  }

  public JsonNode findKnowledgeObjectMetadata(ArkId arkId) {

    if (arkId == null) {
      throw new ShelfResourceNotFound("Cannot find metadata for null ark id");
    }
    Map<String, URI> versionMap = objectLocations.get(arkId.getSlashArk());
    if (versionMap == null) {
      throw new ShelfResourceNotFound("Object location not found for ark id " + arkId.getFullArk());
    }

    if (!arkId.hasVersion()) {
      ArrayNode node = new ObjectMapper().createArrayNode();
      versionMap.forEach((version, location) -> node.add(cdoStore.getMetadata(location)));
      return node;
    }
    URI koLocation = versionMap.get(arkId.getVersion());
    if (koLocation == null) {
      throw new ShelfResourceNotFound(
              "Object location not found for ark id " + arkId.getFullArk());
    }
    return cdoStore.getMetadata(koLocation);
  }

  public KnowledgeObjectWrapper getKow(ArkId arkId) {
    JsonNode metadata = findKnowledgeObjectMetadata(arkId);
    KnowledgeObjectWrapper kow = new KnowledgeObjectWrapper(metadata);
    kow.addService(findServiceSpecification(arkId, metadata));
    kow.addDeployment(findDeploymentSpecification(arkId, metadata));
    return kow;
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

    URI path;
    if (arkId.hasVersion()) {
      path = resolveArkIdToLocation(arkId).resolve(serviceSpecPath);

    } else {
      path =
          ((URI) objectLocations.get(arkId.getSlashArk()).values().toArray()[0])
              .resolve(serviceSpecPath);
    }
    return loadSpecificationNode(arkId, path);
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
    return cdoStore.getBinary(resolveArkIdToLocation(arkId).resolve(childPath));
  }

  public InputStream getBinaryStream(ArkId arkId, String childPath) {
    return cdoStore.getBinaryStream(resolveArkIdToLocation(arkId).resolve(childPath));
  }

  public long getBinarySize(ArkId arkId, String childPath) {
    return cdoStore.getBinarySize(resolveArkIdToLocation(arkId).resolve(childPath));
  }

  private URI resolveArkIdToLocation(ArkId arkId) {
    if (isKoMissingFromMap(arkId)) {
      throw new ShelfResourceNotFound(
              "Object location not found for ark id " + arkId.getFullArk());
    }
    return objectLocations.get(arkId.getSlashArk()).get(arkId.getVersion());
  }

  private boolean isKoMissingFromMap(ArkId arkId) {
    Map<String, URI> versionMapForArk = objectLocations.get(arkId.getSlashArk());
    return versionMapForArk == null || versionMapForArk.get(arkId.getVersion()) == null;
  }

  public URI getKoRepoLocation() {
    return cdoStore.getAbsoluteLocation(null);
  }

  // Used by activator
  public URI getObjectLocation(ArkId arkId) {
    if (isKoMissingFromMap(arkId)) {
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
  protected JsonNode loadSpecificationNode(ArkId arkId, URI uriPath) {
    try {

      YAMLMapper yamlMapper = new YAMLMapper();
      return yamlMapper.readTree(cdoStore.getBinary(uriPath));

    } catch (IOException exception) {
      throw new ShelfException(
          "Could not parse service specification for " + arkId.getFullArk(), exception);
    }
  }

  public void refreshObjectMap() {
    objectLocations.clear();
    knowledgeObjects.clear();

    // Load KO objects and skip any KOs with exceptions like missing metadata
    for (URI path : cdoStore.getChildren()) {
      try {
        ArkId arkId;
        JsonNode metadata = cdoStore.getMetadata(path);
        if (!metadata.has(KoFields.IDENTIFIER.asStr())) {
          log.warn(
              "Folder with metadata " + path + " is missing an identifier field, cannot load.");
          continue;
        }

        if (!metadata.has(KoFields.VERSION.asStr())) {
          log.warn("Folder with metadata " + path + " is missing a version field.");
          arkId = new ArkId(metadata.get(KoFields.IDENTIFIER.asStr()).asText());
        } else {
          if (metadata.get(KoFields.IDENTIFIER.asStr()).asText().matches(ArkId.arkIdRegex())) {
            arkId =
                new ArkId(
                    metadata.get(KoFields.IDENTIFIER.asStr()).asText()
                        + "/"
                        + metadata.get(KoFields.VERSION.asStr()).asText());
          } else {
            arkId = new ArkId(metadata.get(KoFields.IDENTIFIER.asStr()).asText());
          }
        }

        if (objectLocations.get(arkId.getSlashArk()) != null
            && objectLocations.get(arkId.getSlashArk()).get(arkId.getVersion()) != null) {
          log.warn(
              "Two objects on the shelf have the same ark id: "
                  + arkId
                  + " Check folders "
                  + path
                  + " and "
                  + resolveArkIdToLocation(arkId));
        }

        if (objectLocations.get(arkId.getSlashArk()) == null) {
          Map<String, URI> versionMap = new TreeMap<>(Collections.reverseOrder());
          versionMap.put(arkId.getVersion(), path);
          objectLocations.put(arkId.getSlashArk(), versionMap);
        } else {
          objectLocations.get(arkId.getSlashArk()).put(arkId.getVersion(), path);
        }

        knowledgeObjects.put(arkId, metadata);

      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
  }

  public void addKnowledgeObjectToLocatioMap(URI id, JsonNode metadata) {
    String[] arkParts = id.toString().split("/");
    ArkId arkId = new ArkId(arkParts[0], arkParts[1], arkParts[2]);
    if (objectLocations.get(arkId.getSlashArk()) != null) {
      objectLocations.get(arkId.getSlashArk()).put(arkId.getVersion(), id);
    } else {
      Map<String, URI> versionMap = new HashMap<>();
      versionMap.put(arkId.getVersion(), id);
      objectLocations.put(arkId.getSlashArk(), versionMap);
    }
    knowledgeObjects.put(arkId, metadata);
  }
}
