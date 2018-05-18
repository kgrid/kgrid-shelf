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
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeObjectRepository {

  private CompoundDigitalObjectStoreFactory factory;
  private CompoundDigitalObjectStore dataStore;
  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStoreFactory factory) {
    this.factory = factory;
    this.dataStore = factory.create();
  }

  public KnowledgeObject findByArkIdAndVersion(ArkId arkId, String version) {
    KnowledgeObject ko = new KnowledgeObject(arkId, version);
    ObjectNode metadataNode = dataStore.getMetadata(ko.baseMetadataLocation());
    JsonNode modelMetadataNode = dataStore.getMetadata(ko.modelMetadataLocation());
    metadataNode.set("models", modelMetadataNode);
    ko.setMetadata(metadataNode);
    return ko;
  }

  public ObjectNode getMetadataAtPath(ArkId arkId, String version, String path) {
    return dataStore.getMetadata(Paths.get(arkId.getFedoraPath(), version, path));

  }

  public Map<String, ObjectNode> findByArkId(ArkId arkId) {
    Map<String, ObjectNode> versionMap = new HashMap<>();

    List<Path> versions = dataStore.getChildren(Paths.get(arkId.getFedoraPath()));
    for (Path version : versions) {
      versionMap.put(version.getFileName().toString(), findByArkIdAndVersion(arkId, version.getFileName().toString()).getMetadata());
    }
    return versionMap;
  }

  public Map<String, Map<String, ObjectNode>> findAll(){
    Map<String, Map<String, ObjectNode>> knowledgeObjects = new HashMap<>();

    List<ArkId> arkIds = dataStore.getChildren(null).stream()
        .map(name -> {
            try {return new ArkId(name.getFileName().toString());
          } catch (IllegalArgumentException | NullPointerException e) {
              log.warn("Found this directory on shelf " + name.getFileName() + ", name not in Ark id format (naan-name)");
              return null;
          }
        })
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    for (ArkId arkId : arkIds) {
      knowledgeObjects.put(arkId.getArkId(), findByArkId(arkId));
    }
    return knowledgeObjects;
  }

  public ArkId save(MultipartFile zippedKO) {
    CompoundDigitalObjectStore dataStore = factory.create();
    ObjectNode jsonData = dataStore.addCompoundObjectToShelf(zippedKO);
    return new ArkId(jsonData.get("metadata").get("arkId").get("arkId").asText());
  }

  public void findByArkIdAndVersion(ArkId arkId, String version, OutputStream outputStream) throws IOException {
    Path relativeDestination = Paths.get(arkId.getFedoraPath(), version);
    dataStore.getCompoundObjectFromShelf(relativeDestination, outputStream);
  }

  public ObjectNode editMetadata(ArkId arkId, String version, String path, String metadata) {
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths.get(arkId.getFedoraPath(), version, path, "metadata.json");
    } else {
      metadataPath = Paths.get(arkId.getFedoraPath(), version, "metadata.json");
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

  public Path getConnection() {

  return this.dataStore.getAbsoluteLocation(Paths.get(""));
  }
}
