package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipImportService {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipImportService.class);

  /**
   * Create KO object, must add Knowledge Object files, Knowledge Object properties and Knowledge
   * Object Implementation properties
   *
   * @param zipFileStream zip in the form of a stream
   * @param cdoStore persistence layer
   * @return arkId imported arkId
   */
  public ArkId importKO(InputStream zipFileStream, CompoundDigitalObjectStore cdoStore) {

    Map<String, JsonNode> containerResources = new HashMap<>();
    Map<String, byte[]> binaryResources = new HashMap<>();

    captureZipEntries(zipFileStream, containerResources, binaryResources);
    if (containerResources.isEmpty()) {
      throw new ShelfException(
          "The imported zip is not a valid knowledge object, no valid metadata found");
    }

    ArkId arkId = new ArkId(findKOMetadata(containerResources).get("identifier").asText());

    importObject(arkId, cdoStore, containerResources, binaryResources);

    return arkId;
  }

  /**
   * Captures the Zip Entries loading a collection of metadata and collection of binaries
   *
   * @param zipFileStream zip file in a stream
   * @param containerResources collection of metadata files
   * @param binaryResources collection of binary files
   */
  private void captureZipEntries(InputStream zipFileStream,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources) {

    log.info("processing zipEntries");
    Map<String, JsonNode> metadataQueue = Collections.synchronizedMap(new LinkedHashMap<>());
    Map<String, byte[]> binaryQueue = Collections.synchronizedMap(new LinkedHashMap<>());

    ZipUtil.iterate(zipFileStream, (inputStream, zipEntry) -> {

      if (!zipEntry.getName().contains("__MACOSX")) {

        if (zipEntry.getName().endsWith(KnowledgeObject.METADATA_FILENAME)) {

          StringWriter writer = new StringWriter();
          IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);

          JsonNode metadata = new ObjectMapper().readTree(writer.toString());

          try {
            validateMetadata(zipEntry.getName(), metadata);
          } catch (ShelfException e) {
            log.warn(e.getMessage());
            return;
          }

          metadataQueue.put(metadata.get("@id").asText(), metadata);

        } else if (!zipEntry.isDirectory() &&
            !zipEntry.getName().endsWith(KnowledgeObject.METADATA_FILENAME)) {

          binaryQueue.put(zipEntry.getName(), IOUtils.toByteArray(inputStream));
        }
      }
    });

    metadataQueue.forEach((filename, metadata) ->
        containerResources.put(filename, metadata));

    binaryQueue.forEach((filename, bytes) ->
        binaryResources.put(FilenameUtils.normalize(filename), bytes));
  }

  /**
   * Finds the KO metadata in the zip resources based on @type
   *
   * @param containerResources collection of metadata
   * @return imported ko metadata
   */
  public JsonNode findKOMetadata(Map<String, JsonNode> containerResources) {

    Optional<JsonNode> koMetadata =
        containerResources.entrySet()
            .stream()
            .filter(jsonNode -> jsonNode.getValue().has("@type"))
            .filter(jsonNode -> jsonNode.getValue().get("@type").asText()
                .equals("koio:KnowledgeObject"))
            .map(value -> value.getValue()).findFirst();

    if (koMetadata.isPresent()) {
      return koMetadata.get();
    } else {
      throw new ShelfException(
          "The imported zip is not a valid knowledge object, no valid metadata found");
    }

  }

  /**
   * Process the KO import
   *
   * @param arkId the objects ark
   * @param cdoStore ko store
   * @param containerResources collection of metadata
   * @param binaryResources collection of binaries
   */
  public void importObject(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources) {

    log.info("loading zip file for " + arkId.getDashArk());
    String trxId = cdoStore.createTransaction();
    try {

      ObjectNode koMetaData = (ObjectNode) findKOMetadata(containerResources);

      if (ObjectUtils.isEmpty(koMetaData)) {
        throw new ShelfException("No KO metadata found, can not import zip file");
      }
      cdoStore.createContainer(trxId, arkId.getDashArk());

      findImplemtationMetadata(containerResources).forEach(jsonNode -> {
        importImplementation(arkId, trxId, cdoStore, containerResources, binaryResources, jsonNode);
      });

      KnowledgeObjectRepository knowledgeObjectRepository =
          new KnowledgeObjectRepository(cdoStore, null, null);

      mergeImplementations(arkId, cdoStore, koMetaData);

      cdoStore.saveMetadata(koMetaData, trxId, arkId.getDashArk(),
          KnowledgeObject.METADATA_FILENAME);

      findImplemtationMetadata(containerResources).forEach(jsonNode -> {
        knowledgeObjectRepository.deleteImpl(new ArkId(jsonNode.get("identifier").asText()));
      });

      cdoStore.commitTransaction(trxId);

    } catch (Exception e) {
      cdoStore.rollbackTransaction(trxId);
      log.warn(e.getMessage());
      throw new ShelfException("Could not import " +arkId, e);
    }
  }

  /**
   * Merge the imported and existing implementations together if needed.  This allows import of a
   * single implementation with altering existing implementations
   *
   * @param arkId imported ark id
   * @param cdoStore data store instance
   * @param koMetaData imported ko metadata
   * @throws IOException mapper read error
   */
  protected void mergeImplementations(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      ObjectNode koMetaData) throws IOException {

    try {

      ObjectNode existingKoMetadata = cdoStore.getMetadata(arkId.getDashArk());
      List<String> existingImplementations = new ArrayList<>();
      List<String>  importedImplementations = new ArrayList<>();
      ObjectMapper mapper = new ObjectMapper();

      //convert everything to lists
      if(existingKoMetadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM).isArray()){
        existingImplementations = mapper.readValue( existingKoMetadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM).toString(), List.class);
      } else {
        existingImplementations.add( existingKoMetadata.get(KnowledgeObject.IMPLEMENTATIONS_TERM).asText() );
      }

      if(koMetaData.get(KnowledgeObject.IMPLEMENTATIONS_TERM).isArray()){
        importedImplementations = mapper.readValue( koMetaData.get(KnowledgeObject.IMPLEMENTATIONS_TERM).toString(), List.class);
      } else {
        importedImplementations.add( koMetaData.get(KnowledgeObject.IMPLEMENTATIONS_TERM).asText() );
      }

      //remove and add to makes sure no dups in list
      importedImplementations.removeAll(existingImplementations); // remove elements that would be duplicated
      importedImplementations.addAll(existingImplementations);

      //update ko metatdata with imported and existing implementations
      koMetaData.set(KnowledgeObject.IMPLEMENTATIONS_TERM,mapper.valueToTree(importedImplementations));

    } catch (ShelfResourceNotFound notFound) {
      log.info("No existing ko, nothing to merge ", arkId.getDashArk());
    }
  }

  /**
   * Finds the Implementation metadata in the zip resources based on @type
   *
   * @param containerResources collection of resources being imported
   * @return collection of implementation metadata for the imported ko
   */
  public List<JsonNode> findImplemtationMetadata(Map<String, JsonNode> containerResources) {

    List<JsonNode> implemtationMetadata = containerResources.entrySet()
        .stream()
        .filter(jsonNode -> jsonNode.getValue().has("@type"))
        .filter(jsonNode -> jsonNode.getValue().get("@type").asText().equals("koio:Implementation"))
        .map(value -> value.getValue()).collect(Collectors.toList());

    return implemtationMetadata;

  }


  /**
   * Validate the metadata
   * @param filename  metadata filename
   * @param metadata jsonnode of metatdata
   */
  protected void validateMetadata(String filename, JsonNode metadata) {
    String typeLabel = "@type", idLabel = "@id";
    String ko = "koio:KnowledgeObject", impl = "koio:Implementation";

    if (!metadata.has(idLabel) || !metadata.has(typeLabel)) {
      throw new ShelfException("Cannot import, Missing @id in file " + filename);
    }
    if (!metadata.has(typeLabel)) {
      throw new ShelfException("Cannot import, Missing @type label in file " + filename);
    }
    if (!ko.equals(metadata.get(typeLabel).asText()) && !impl
        .equals(metadata.get(typeLabel).asText())) {
      throw new ShelfException(
          "Cannot import,  Missing knowledge object or implementation @type in file "
              + filename);
    }
  }

  /**
   * Imports the KO Implementations loading the metadata and binaries
   *
   * @param arkId Ark Id of the object
   * @param cdoStore persistence layer
   * @param containerResources metadata load from the zip
   * @param binaryResources binaries load based on the metadata in the zip
   * @param metadata implementation meatadata
   */
  private void importImplementation(ArkId arkId, String trxId, CompoundDigitalObjectStore cdoStore,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources,
      JsonNode metadata) {

    try {

      String path = Paths.get(arkId.getDashArk(), metadata.get("@id").asText()).toString();
      cdoStore.createContainer(trxId, path);
      findImplentationBinaries(binaryResources, metadata.get("@id").asText())
          .forEach((binaryPath, bytes) -> {

            cdoStore.saveBinary(bytes, trxId, Paths.get(arkId.getDashArk(),
                binaryPath.substring(binaryPath.indexOf(metadata.get("@id").asText()))).toString());

          });

      cdoStore.saveMetadata(metadata, trxId, path, KnowledgeObject.METADATA_FILENAME);

    } catch (Exception e) {
      throw new ShelfException("Issue importing implementation ", e);
    }

  }

  /**
   * Find any binaries under the implementation
   *
   * @param binaryResources collection of binaries
   * @param implementation implementation id
   * @return binaries collection of binaries and paths
   */
  public Map<String, byte[]> findImplentationBinaries(Map<String, byte[]> binaryResources,
      String implementation) {

    Map<String, byte[]> binaries = binaryResources.entrySet()
        .stream()
        .filter(map -> map.getKey().contains(File.separator + implementation + File.separator))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return binaries;

  }
}
