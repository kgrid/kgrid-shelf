package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeObjectRepository {

  private CompoundDigitalObjectStore dataStore;
  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStore compoundDigitalObjectStore) {
    this.dataStore = compoundDigitalObjectStore;
  }

  public KnowledgeObject findByArkIdAndVersion(ArkId arkId, String version) {
    KnowledgeObject ko = new KnowledgeObject(arkId, version);
    ObjectNode metadataNode = dataStore.getMetadata(ko.baseMetadataLocation());
    try {
      JsonNode modelMetadataNode = dataStore.getMetadata(ko.modelMetadataLocation());
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
    return dataStore.getMetadata(Paths.get(arkId.getFedoraPath(), version, path));
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

    List<Path> versions = dataStore.getChildren(Paths.get(arkId.getFedoraPath()));

    for (Path version : versions) {
      try {
       versionMap.put(version.getFileName().toString(),
           findByArkIdAndVersion(arkId, version.getFileName().toString()).getMetadata());
      } catch (Exception exception){
       log.warn( "Can't load KO " + arkId + "/" + version.getFileName().toString() + " " + exception.getMessage());
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
    for (Path path : dataStore.getChildren(null)) {
      try {
        knowledgeObjects.put(new ArkId(path.getFileName().toString()),
            findByArkId(new ArkId(path.getFileName().toString())));
      } catch (Exception illegalArgument) {
        log.warn("Unable to load KO " + illegalArgument.getMessage());
      }
    }
    return knowledgeObjects;
  }

  public ArkId save(MultipartFile zippedKO) {
    return dataStore.addCompoundObjectToShelf(zippedKO);
  }

  public void putZipFileIntoOutputStream(ArkId arkId, OutputStream outputStream)
      throws IOException {
    Path relativeDestination = Paths.get(arkId.getFedoraPath());
    dataStore.getCompoundObjectFromShelf(relativeDestination, false, outputStream);
  }

  public void findByArkIdAndVersion(ArkId arkId, String version, OutputStream outputStream)
      throws IOException {
    Path relativeDestination = Paths.get(arkId.getFedoraPath(), version);
    dataStore.getCompoundObjectFromShelf(relativeDestination, true, outputStream);
  }

  public ObjectNode editMetadata(ArkId arkId, String version, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths
          .get(arkId.getFedoraPath(), version, path, KnowledgeObject.METADATA_FILENAME);
    } else {
      metadataPath = Paths.get(arkId.getFedoraPath(), version, KnowledgeObject.METADATA_FILENAME);
    }
    try {
      JsonNode jsonMetadata = new ObjectMapper().readTree(metadata);

      dataStore.saveMetadata(metadataPath, jsonMetadata);

    } catch (IOException e) {
      log.error("Cannot edit metadata at " + metadataPath + " " + e);
    }
    return dataStore.getMetadata(metadataPath);
  }

  public void delete(ArkId arkId) throws IOException {
    dataStore.removeFile(Paths.get(arkId.getFedoraPath()));
    log.info("Deleted ko with ark id " + arkId);
  }

  public void delete(ArkId arkId, String version) throws IOException {
    dataStore.removeFile(Paths.get(arkId.getFedoraPath(), version));
    log.info("Deleted ko with ark id " + arkId + " and version " + version);
  }

  public String getConnection() {

    return this.dataStore.getAbsoluteLocation(Paths.get(""));
  }

  public byte[] getBinary(ArkId arkId, String version, String childPath) {
    Path filepath = Paths.get(arkId.getFedoraPath(), version, childPath);
    return this.dataStore.getBinary(filepath);
  }
}
