package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
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

  private CompoundDigitalObjectStore dataStore;
  private ZipImportService zipImportService;
  private ZipExportService zipExportService;
  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStore compoundDigitalObjectStore, ZipImportService zis,
      ZipExportService zes) {
    this.dataStore = compoundDigitalObjectStore;
    this.zipImportService = zis;
    this.zipExportService = zes;
  }

  public JsonNode getMetadataAtPath(ArkId arkId, String path) {
    return dataStore.getMetadata(arkId.getDashArk(), arkId.getImplementation(), path);
  }

  public JsonNode findKnowledgeObjectMetadata(ArkId arkId) {
    return dataStore.getMetadata(arkId.getDashArk());
  }

  public JsonNode findImplementationMetadata(ArkId arkId) {
    ObjectNode metadataNode = dataStore.getMetadata(arkId.getDashArk(), arkId.getImplementation(), KnowledgeObject.METADATA_FILENAME);
    if(!metadataNode.has("title")) {
      log.warn("Metadata for ko " + arkId.getSlashArkImplementation() + " is missing a title");
    }
    return metadataNode;
  }

  public Map<ArkId, JsonNode> findAll() {
    Map<ArkId, JsonNode> knowledgeObjects = new HashMap<>();

    //Load KO objects and skip any KOs with exceptions like missing metadata
    for (String path : dataStore.getChildren("")) {
      try {
        ArkId arkId;
        if(path.contains("/")) {
          arkId = new ArkId(StringUtils.substringAfterLast(path, "/"));
        } else if (path.contains("\\")) {
          arkId = new ArkId(StringUtils.substringAfterLast(path, "\\"));
        }
        else {
          arkId = new ArkId(path);
        }
        knowledgeObjects.put(arkId, findKnowledgeObjectMetadata(arkId));
      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
    return knowledgeObjects;
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

    String uriPath = ResourceUtils.isUrl(serviceSpecPath)?
        serviceSpecPath:Paths.get( arkId.getDashArk(), serviceSpecPath).toString();

    try {
      YAMLMapper yamlMapper = new YAMLMapper();
      JsonNode serviceSpecNode =  yamlMapper.readTree(dataStore.getBinary(uriPath));

      return serviceSpecNode;

    } catch (IOException exception){
      throw new ShelfException("Could not parse service specification for " +
          arkId.getDashArkImplementation(), exception);
    }

  }

  /**
   * Find the Service Specification for the implementation
   *
   * @param arkId Ark ID for the implementation
   * @return JsonNode service specification
   */
  public JsonNode findServiceSpecification(ArkId arkId) {

     return findServiceSpecification(arkId, findImplementationMetadata(arkId));

  }

  public JsonNode findPayload(ArkId arkId) {

   throw new RuntimeException("not implemented yet");

  }

  public JsonNode findDeploymentSpecification(ArkId arkId) {

    throw new RuntimeException("not implemented yet");
  }

  /**
   * Import ZIP file of a KO into self
   * @param arkId ark id of object
   * @param zippedKO zip file
   * @return ark id of the import object
   */
  public ArkId importZip(ArkId arkId, MultipartFile zippedKO) {
    try {
      zipImportService.importCompoundDigitalObject(arkId, zippedKO.getInputStream(), dataStore);
    } catch (IOException e) {
      log.warn("Cannot load full zip file for ark id " + arkId);
    }
    return arkId;
  }

  /**
   * Extract ZIP file of the KO
   * @param arkId ark id of the object
   * @param outputStream zipped file in outputstream
   * @throws IOException if the system can't extract the zip file to the filesystem
   */
  public void extractZip(ArkId arkId, OutputStream outputStream) throws IOException {
    outputStream.write(zipExportService.exportCompoundDigitalObject(arkId, dataStore).toByteArray());
  }

  public ObjectNode editMetadata(ArkId arkId, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths.get(arkId.getDashArk(), arkId.getImplementation(), path, KnowledgeObject.METADATA_FILENAME);
    } else {
      metadataPath = Paths.get(arkId.getDashArk(), arkId.getImplementation(), KnowledgeObject.METADATA_FILENAME);
    }
    try {
      JsonNode jsonMetadata = new ObjectMapper().readTree(metadata);

      dataStore.saveMetadata(jsonMetadata, metadataPath.toString());

    } catch (IOException e) {
      log.error("Cannot edit metadata at " + metadataPath + " " + e);
    }
    return dataStore.getMetadata(metadataPath.toString());
  }

  public void delete(ArkId arkId) throws IOException {
    if(arkId.getImplementation() != null) {
      dataStore.removeFile(arkId.getDashArk(), arkId.getImplementation());
    } else {
      dataStore.removeFile(arkId.getDashArk());
    }
    log.info("Deleted ko with ark id " + arkId);
  }

  public String getConnection() {

    return this.dataStore.getAbsoluteLocation("");
  }

  public byte[] getBinaryOrMetadata(ArkId arkId, String childPath) {
    String filepath = Paths.get(arkId.getDashArk(), arkId.getImplementation(), childPath).toString();
    if(this.dataStore.isMetadata(filepath)) {

      return this.dataStore.getMetadata(filepath).toString().getBytes();

    }
    return this.dataStore.getBinary(filepath);
  }
}
