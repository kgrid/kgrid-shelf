package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umich.lhs.activator.domain.ArkId;
import edu.umich.lhs.activator.domain.CompoundKnowledgeObject;
import edu.umich.lhs.activator.domain.KnowledgeObject;
import edu.umich.lhs.activator.domain.Payload;
import edu.umich.lhs.activator.domain.SimpleKnowledgeObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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

  public KnowledgeObject getCompoundKnowledgeObject(ArkId arkId, String version) {
    CompoundDigitalObjectStore dataStore = factory.create(arkId.getFedoraPath());

    KnowledgeObject ko = new CompoundKnowledgeObject(arkId, version);
    ObjectNode metadataNode = dataStore.getMetadata(ko.baseMetadataLocation());
    JsonNode modelMetadataNode = dataStore.getMetadata(ko.modelMetadataLocation());
    metadataNode.set("models", modelMetadataNode);
    ko.setMetadata(metadataNode);
    return ko;
  }

  public SimpleKnowledgeObject convertCompoundToSimpleKObject(ArkId arkId, String version) {
    CompoundDigitalObjectStore dataStore = factory.create(arkId.getFedoraPath());
    SimpleKnowledgeObject sko = new SimpleKnowledgeObject();
    KnowledgeObject ko = getCompoundKnowledgeObject(arkId, version);

    byte[] inputMessage = dataStore.getBinary(ko.serviceLocation().resolve("input.xml"));
    sko.setInputMessage(new String(inputMessage, Charset.defaultCharset()));
    byte[] outputMessage = dataStore.getBinary(ko.serviceLocation().resolve("output.xml"));
    sko.setOutputMessage(new String (outputMessage, Charset.defaultCharset()));
    sko.setMetadata(ko.getMetadata());

    Payload payload = new Payload();
    payload.setContent(new String(dataStore.getBinary(ko.resourceLocation()), Charset.defaultCharset()));
    payload.setEngineType(ko.adapterType());
    payload.setFunctionName(ko.getMetadata().get("models").get("functionName").asText());

    sko.setPayload(payload);
    return sko;
  }

  public SimpleKnowledgeObject getSimpleKnowledgeObject(ArkId arkId) {
    CompoundDigitalObjectStore dataStore = factory.create();
    Path koPath = Paths.get(arkId.getFedoraPath());
    ObjectNode koJson;
    if(Files.exists(koPath)) {
      koJson = dataStore.getMetadata(koPath);
    } else {
      koPath = Paths.get(arkId.getFedoraPath() + ".json");
      koJson = dataStore.getMetadata(koPath);
    }
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.treeToValue(koJson, SimpleKnowledgeObject.class);
    } catch (JsonProcessingException jpEx) {
      throw new IllegalArgumentException("Cannot convert file " + arkId + " to simple ko");
    }
  }

  public Map<String, ObjectNode> knowledgeObjectVersions(ArkId arkId) {
    CompoundDigitalObjectStore dataStore = factory.create(arkId.getFedoraPath());
    Map<String, ObjectNode> versionMap = new HashMap<>();

    List<String> versions = dataStore.getChildren(Paths.get(arkId.getFedoraPath()));
    for (String version : versions) {
      versionMap.put(version, getCompoundKnowledgeObject(arkId, version).getMetadata());
    }

    return versionMap;
  }

  public Map<String, Map<String, ObjectNode>> getAllObjects(){
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
      knowledgeObjects.put(arkId.getArkId(), knowledgeObjectVersions(arkId));
    }
    return knowledgeObjects;
  }

  public ArkId saveKnowledgeObject(MultipartFile zippedKO) {
    CompoundDigitalObjectStore dataStore = factory.create();
    ObjectNode jsonData = dataStore.addCompoundObjectToShelf(zippedKO);
    return new ArkId(jsonData.get("metadata").get("arkId").get("arkId").asText());
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

  public void removeKO(ArkId arkId) throws IOException {
    CompoundDigitalObjectStore dataStore = factory.create();
    dataStore.removeFile(Paths.get(arkId.getFedoraPath()));

  }

}
