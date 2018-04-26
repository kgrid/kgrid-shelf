package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umich.lhs.activator.domain.ArkId;
import edu.umich.lhs.activator.domain.CompoundKnowledgeObject;
import edu.umich.lhs.activator.domain.KnowledgeObject;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
  private final org.slf4j.Logger log = LoggerFactory.getLogger(KnowledgeObjectRepository.class);

  @Autowired
  KnowledgeObjectRepository(CompoundDigitalObjectStoreFactory factory) {
    this.factory = factory;
  }

  public KnowledgeObject findByArkIdAndVersion(ArkId arkId, String version) {
    CompoundDigitalObjectStore dataStore = factory.create(arkId.getFedoraPath());

    KnowledgeObject ko = new CompoundKnowledgeObject(arkId, version);
    ObjectNode metadataNode = dataStore.getMetadata(ko.baseMetadataLocation());
    JsonNode modelMetadataNode = dataStore.getMetadata(ko.modelMetadataLocation());
    metadataNode.set("models", modelMetadataNode);
    ko.setMetadata(metadataNode);
    return ko;
  }

  public Map<String, ObjectNode> findByArkId(ArkId arkId) {
    CompoundDigitalObjectStore dataStore = factory.create(arkId.getFedoraPath());
    Map<String, ObjectNode> versionMap = new HashMap<>();

    List<String> versions = dataStore.getChildren(Paths.get(arkId.getFedoraPath()));
    for (String version : versions) {
      versionMap.put(version, findByArkIdAndVersion(arkId, version).getMetadata());
    }

    return versionMap;
  }

  public Map<String, Map<String, ObjectNode>> findAll(){
    CompoundDigitalObjectStore dataStore = factory.create();
    Map<String, Map<String, ObjectNode>> knowledgeObjects = new HashMap<>();

    List<ArkId> arkIds = dataStore.getChildren(null).stream()
        .map(name -> {
            try {return new ArkId(name);
          } catch (IllegalArgumentException | NullPointerException e) {
            log.error(e.getMessage());return null;
          }
        })
        .filter(Objects::nonNull)
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
    CompoundDigitalObjectStore dataStore = factory.create();
    dataStore.getCompoundObjectFromShelf(arkId, version, outputStream);
  }

  public ObjectNode editMetadata(ArkId arkId, String version, String path, String metadata) {
    CompoundDigitalObjectStore dataStore = factory.create();
    Path metadataPath;
    if (path != null && !"".equals(path)) {
      metadataPath = Paths.get(arkId.getFedoraPath(), version, path, "metadata.json");
    } else {
      metadataPath = Paths.get(arkId.getFedoraPath(), version, "metadata.json");
    }

    List<String> immutableFields = new ArrayList<>();
    immutableFields.add("arkId");
    immutableFields.add("version");
    try {
      JsonNode jsonMetadata = new ObjectMapper().readTree(metadata);

      dataStore.saveMetadata(metadataPath, jsonMetadata);

    } catch (IOException e) {
      log.error("Cannot edit metadata at " + metadataPath + " " + e);
    }
    return dataStore.getMetadata(metadataPath);
  }

  public void delete(ArkId arkId) throws IOException {
    CompoundDigitalObjectStore dataStore = factory.create();
    dataStore.removeFile(Paths.get(arkId.getFedoraPath()));

  }

}
