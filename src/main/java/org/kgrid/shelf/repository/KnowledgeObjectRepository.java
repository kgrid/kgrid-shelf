package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeObjectRepository {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);
  private CompoundDigitalObjectStore dataStore;
  private ZipImportService zipImportService;
  private ZipExportService zipExportService;
  private final static Map<String, String> objectLocations = new HashMap<>();

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStore compoundDigitalObjectStore,
      ZipImportService zis,
      ZipExportService zes) {
    this.dataStore = compoundDigitalObjectStore;
    this.zipImportService = zis;
    this.zipExportService = zes;
    // Initialize the map of folder names -> ark ids
    findAll();
  }

  public void delete(ArkId arkId) {

    dataStore.delete(objectLocations.get(arkId.getDashArk()));
    log.info("Deleted ko with ark id " + arkId);
  }

  public void deleteImpl(ArkId arkId) {
    dataStore.delete(objectLocations.get(arkId.getDashArk()), arkId.getImplementation());
    JsonNode objectMetadata = dataStore.getMetadata(objectLocations.get(arkId.getDashArk()));
    if(objectMetadata.has(KnowledgeObject.IMPLEMENTATIONS_TERM) && objectMetadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM).isArray()) {
      ArrayNode impls = (ArrayNode)objectMetadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM);
      for (int i = 0; i < impls.size(); i++) {
        if(impls.get(i).asText().equals(arkId.getDashArkImplementation())) {
          impls.remove(i);
        }
      }
      ((ObjectNode) objectMetadata).set(KnowledgeObject.IMPLEMENTATIONS_TERM, impls);
      dataStore.saveMetadata(objectMetadata, objectLocations.get(arkId.getDashArk()));
    }

  }

  public ObjectNode editMetadata(ArkId arkId, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths.get(objectLocations.get(arkId.getDashArk()), arkId.getImplementation(), path,
          KnowledgeObject.METADATA_FILENAME);
    } else {
      metadataPath = Paths
          .get(objectLocations.get(arkId.getDashArk()), arkId.getImplementation(), KnowledgeObject.METADATA_FILENAME);
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
    outputStream
        .write(zipExportService.exportObject(arkId, dataStore).toByteArray());
  }

  public Map<ArkId, JsonNode> findAll() {
    Map<ArkId, JsonNode> knowledgeObjects = new HashMap<>();
    objectLocations.clear();

    //Load KO objects and skip any KOs with exceptions like missing metadata
    for (String path : dataStore.getChildren("")) {
      try {
        ArkId arkId;
        String folderName;
        if (path.contains("/")) {
          folderName = StringUtils.substringAfterLast(path, "/");
        } else if (path.contains("\\")) {
          folderName = StringUtils.substringAfterLast(path, "\\");
        } else {
          folderName = path;
        }
        JsonNode metadata = dataStore.getMetadata(folderName);
        if(!metadata.has("@id")) {
          log.warn("Folder with metadata " + folderName + " is missing an @id field, cannot load.");
          continue;
        }
        arkId = new ArkId(metadata.get("@id").asText());
        if(objectLocations.get(arkId.getDashArk()) != null) {
          log.warn("Two objects on the shelf have the same ark id: " + arkId + " Check folders " + folderName + " and " + objectLocations.get(arkId.getDashArk()));
        }
        objectLocations.put(arkId.getDashArk(), folderName);
        knowledgeObjects.put(arkId, metadata);
      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
    return knowledgeObjects;
  }

  /**
   * Find the deployment specification based on implementation ark id
   *
   * @param arkId implementation ark id
   * @return JsonNode deployment specification
   */
  public JsonNode findDeploymentSpecification(ArkId arkId) {

    log.info("find deployment specification for  " + arkId.getDashArkImplementation());

    return findDeploymentSpecification(arkId, findImplementationMetadata(arkId));

  }

  /**
   * Find the Deployment Specification for the implementation, if not found throw
   * shelf exception
   *
   * @param arkId Ark ID for the implementation
   * @param implementationNode implementation
   * @return JsonNode deployment specification
   */
  public JsonNode findDeploymentSpecification(ArkId arkId, JsonNode implementationNode) {

    if (implementationNode.has(KnowledgeObject.DEPLOYMENT_SPEC_TERM)) {

      String deploymentSpecPath = implementationNode.findValue(
          KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText();

    String uriPath = ResourceUtils.isUrl(deploymentSpecPath) ?
        deploymentSpecPath : Paths.get(objectLocations.get(arkId.getDashArk()), deploymentSpecPath).toString();

      return loadSpecificationNode(arkId, uriPath);


    } else {

      throw new ShelfException("deployment specification not found in metadata");

    }

  }

  public JsonNode findImplementationMetadata(ArkId arkId) {
    ObjectNode metadataNode = dataStore.getMetadata(objectLocations.get(arkId.getDashArk()), arkId.getImplementation(),
        KnowledgeObject.METADATA_FILENAME);
    if (!metadataNode.has("title")) {
      log.warn("Metadata for ko " + arkId.getSlashArkImplementation() + " is missing a title");
    }
    return metadataNode;
  }

  public JsonNode findKnowledgeObjectMetadata(ArkId arkId) {
    return dataStore.getMetadata(objectLocations.get(arkId.getDashArk()));
  }


  public byte[] findPayload(ArkId arkId, String implementationPath) {

    String payloadPath = Paths.get(objectLocations.get(arkId.getDashArk()),
        implementationPath).toString();

    log.info("find payload for  " + payloadPath);

    return dataStore.getBinary(payloadPath);

  }

  /**
   * Find the Service Specification for the implementation
   *
   * @param arkId Ark ID for the implementation
   * @param implementationNode implementation
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId, JsonNode implementationNode) {

    String serviceSpecPath = implementationNode.findValue(
        KnowledgeObject.SERVICE_SPEC_TERM).asText();

    log.info("find service specification at " + serviceSpecPath);

    String uriPath = ResourceUtils.isUrl(serviceSpecPath) ?
        serviceSpecPath : Paths.get(objectLocations.get(arkId.getDashArk()), serviceSpecPath).toString();

    return loadSpecificationNode(arkId, uriPath);

  }

  /**
   * Find the Service Specification for the implementation
   *
   * @param arkId Ark ID for the implementation
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId) {

    log.info("find service specification for  " + arkId.getDashArkImplementation());

    return findServiceSpecification(arkId, findImplementationMetadata(arkId));

  }

  public byte[] getBinaryOrMetadata(ArkId arkId, String childPath) {
    String filepath = Paths.get(objectLocations.get(arkId.getDashArk()), arkId.getImplementation(), childPath)
        .toString();
    if (this.dataStore.isMetadata(filepath)) {

      return this.dataStore.getMetadata(filepath).toString().getBytes();

    }
    return this.dataStore.getBinary(filepath);
  }

  public String getConnection() {

    return this.dataStore.getAbsoluteLocation("");
  }

  public JsonNode getMetadataAtPath(ArkId arkId, String path) {
    return dataStore.getMetadata(objectLocations.get(arkId.getDashArk()), arkId.getImplementation(), path);
  }

  /**
   * Import ZIP file of a KO into self
   *
   * @param arkId ark id of object
   * @param zippedKO zip file
   * @return ark id of the import object
   */
  public ArkId importZip(ArkId arkId, MultipartFile zippedKO) {
    try {
      String folderName = zipImportService.findArkIdImportKO(zippedKO.getInputStream(), dataStore);
      JsonNode metadata = dataStore.getMetadata(folderName);
      arkId = new ArkId(metadata.get("@id").asText());
      objectLocations.put(arkId.getDashArk(), folderName);
    } catch (IOException e) {
      log.warn("Cannot load full zip file for ark id " + arkId);
    }
    return arkId;
  }

  public ArkId importZip(MultipartFile zippedKO) {
    try {
      String folderName = zipImportService.findArkIdImportKO(zippedKO.getInputStream(), dataStore);
      JsonNode metadata = dataStore.getMetadata(folderName);
      ArkId arkId = new ArkId(metadata.get("@id").asText());
      objectLocations.put(arkId.getDashArk(), folderName);
      return arkId;
    } catch (IOException e) {
      log.warn("Cannot load zip file with filename " + zippedKO.getName());
    }
    return null;
  }

  public ArkId importZip(InputStream zipStream) {

    String folderName = zipImportService.findArkIdImportKO(zipStream, dataStore);
    JsonNode metadata = dataStore.getMetadata(folderName);
    ArkId arkId = new ArkId(metadata.get("@id").asText());
    objectLocations.put(arkId.getDashArk(), folderName);
    return arkId;
  }

  public String getObjectLocation(ArkId arkId) {
    // Reload for activation use cases
    if(objectLocations.get(arkId.getDashArk()) == null) {
      findAll();
    }
    return objectLocations.get(arkId.getDashArk());
  }

  /**
   * Loads a YMAL specification file (service or deployment) and maps to a JSON node
   *
   * @param arkId implementation ark id
   * @param uriPath path to specification file
   * @return JsonNode representing YMAL specification file
   */
  protected JsonNode loadSpecificationNode(ArkId arkId, String uriPath) {
    try {

      YAMLMapper yamlMapper = new YAMLMapper();
      JsonNode serviceSpecNode = yamlMapper.readTree(dataStore.getBinary(uriPath));

      return serviceSpecNode;

    } catch (IOException exception) {
      throw new ShelfException("Could not parse service specification for " +
          arkId.getDashArkImplementation(), exception);
    }
  }
}
