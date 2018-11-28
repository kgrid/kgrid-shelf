package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

  public KnowledgeObject findByArkIdAndVersion(ArkId arkId, String version) {
    KnowledgeObject ko = new KnowledgeObject(arkId, version);
    ObjectNode metadataNode = dataStore.getMetadata(ko.baseMetadataLocation().toString());
    try {
      JsonNode modelMetadataNode = dataStore.getMetadata(ko.modelMetadataLocation().toString());
      metadataNode.set(KnowledgeObject.MODEL_DIR_NAME, modelMetadataNode);
    } catch (IllegalArgumentException | NullPointerException ex) {
      log.warn("Cannot find model metadata for ko " + arkId + "/" + version);
    }
    ko.setMetadata(metadataNode);
    if(!ko.hasTitle()) {
      log.warn("Metadata for ko " + arkId + "/" + version + " is missing a title");
    }
    return ko;
  }

  public ObjectNode getMetadataAtPath(ArkId arkId, String version, String path) {
    return dataStore.getMetadata(arkId.getAsSimpleArk(), version, path);
  }

  public Map<String, ObjectNode> findByPath(Path koPath) {
    Map<String, ObjectNode> versions = new HashMap<>();
    ArkId arkId = new ArkId(koPath.getParent().getFileName().toString());
    String version = koPath.getFileName().toString();
    versions.put(version, findByArkIdAndVersion(arkId, version).getMetadata());
    return versions;
  }

  public Map<String, ObjectNode> findByArkId(ArkId arkId) {
    Map<String, ObjectNode> versionMap = new HashMap<>();

    List<String> versions = dataStore.getChildren(arkId.getAsSimpleArk());

    for (String versionPath : versions) {
      String version = null;
      try {
        if(versionPath.contains("/")) {
          version = StringUtils.substringAfterLast(versionPath, "/");
        } else if (versionPath.contains("\\")) {
          version = StringUtils.substringAfterLast(versionPath, "\\");
        } else {
          log.warn("Version path is invalid: " + versionPath);
        }
        versionMap.put(version, findByArkIdAndVersion(arkId, version).getMetadata());
      } catch (Exception exception){
       log.warn( "Can't load KO " + arkId + "/" + version + " " + exception.getMessage());
      }
    }
    if(versionMap.isEmpty()) {
      throw new IllegalArgumentException("Knowledge object with ark id " + arkId + " has no valid versions");
    }
    return versionMap;
  }

  public Map<ArkId, Map<String, ObjectNode>> findAll() {
    Map<ArkId, Map<String, ObjectNode>> knowledgeObjects = new HashMap<>();

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
        knowledgeObjects.put(arkId, findByArkId(arkId));
      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
    return knowledgeObjects;
  }

  public ArkId save(ArkId arkId, MultipartFile zippedKO) {
    try {
      zipImportService.importCompoundDigitalObject(arkId, zippedKO.getInputStream(), dataStore);
    } catch (IOException e) {
      log.warn("Cannot load full zip file for ark id " + arkId);
    }
    return arkId;
  }

  public void putZipFileIntoOutputStream(ArkId arkId, OutputStream outputStream) throws IOException {
    outputStream.write(zipExportService.exportCompoundDigitalObject(arkId, dataStore).toByteArray());
  }

  public ObjectNode editMetadata(ArkId arkId, String version, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths
          .get(arkId.getAsSimpleArk(), version, path, KnowledgeObject.METADATA_FILENAME);
    } else {
      metadataPath = Paths.get(arkId.getAsSimpleArk(), version, KnowledgeObject.METADATA_FILENAME);
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
    dataStore.removeFile(Paths.get(arkId.getAsSimpleArk()).toString());
    log.info("Deleted ko with ark id " + arkId);
  }

  public void delete(ArkId arkId, String version) throws IOException {
    dataStore.removeFile(Paths.get(arkId.getAsSimpleArk(), version).toString());
    log.info("Deleted ko with ark id " + arkId + " and version " + version);
  }

  public String getConnection() {

    return this.dataStore.getAbsoluteLocation("");
  }

  public byte[] getBinaryOrMetadata(ArkId arkId, String version, String childPath) {
    String filepath = Paths.get(arkId.getAsSimpleArk(), version, childPath).toString();
    if(this.dataStore.isMetadata(filepath)) {

      return this.dataStore.getMetadata(filepath).toString().getBytes();

    }
    return this.dataStore.getBinary(filepath);
  }
}
