package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
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
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipImportService  {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipImportService.class);

  /**
   * Create KO object, must add Knowledge Object files, Knowledge Object properties and Knowledge
   * Object Implementation properties
   *
   * @param zipFileStream zip in the form of a stream
   * @param cdoStore persistence layer
   */
  public ArkId importKO(InputStream zipFileStream, CompoundDigitalObjectStore cdoStore) {

    Map<String, JsonNode> containerResources = new HashMap<>();
    Map<String, byte[]> binaryResources = new HashMap<>();

    captureZipEntries(zipFileStream, containerResources, binaryResources);
    if(containerResources.isEmpty()) {
      throw new ShelfException("The imported zip is not a valid knowledge object, no valid metadata found");
    }

    ArkId arkId = new ArkId( findKOMetadata(containerResources).get("identifier").asText());

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
   * @param containerResources
   * @return ko metadata as jsonnode
   */
  public JsonNode findKOMetadata(  Map<String, JsonNode> containerResources){

    Optional<JsonNode> koMetadata =
        containerResources.entrySet()
            .stream()
            .filter(jsonNode -> jsonNode.getValue().has("@type"))
            .filter(jsonNode -> jsonNode.getValue().get("@type").asText().equals("koio:KnowledgeObject"))
            .map(value -> value.getValue()).findFirst();

    if(koMetadata.isPresent()){
      return koMetadata.get();
    } else {
      throw new ShelfException("The imported zip is not a valid knowledge object, no valid metadata found");
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

      JsonNode koMetaData = findKOMetadata(containerResources);

      if (ObjectUtils.isEmpty(koMetaData)){
        throw new ShelfException("No KO metadata found, can not import zip file");
      }
      cdoStore.createContainer(trxId, arkId.getDashArk());

      findImplemtationMetadata(containerResources).forEach(jsonNode -> {
        importImplementation(arkId, trxId, cdoStore, containerResources, binaryResources, jsonNode);
      });

      cdoStore.saveMetadata(koMetaData, trxId, arkId.getDashArk(),
          KnowledgeObject.METADATA_FILENAME);

      // Remove the object if it exists before committing the transaction and copying the new one to its location
      cdoStore.delete(arkId.getDashArk());

      cdoStore.commitTransaction(trxId);
    } catch (Exception e) {
      cdoStore.rollbackTransaction(trxId);
      log.warn(e.getMessage());
      throw e;
    }
  }

  /**
   * Finds the Implementation metadata in the zip resources based on @type
   * @param containerResources
   * @return list of implementation metatdata as jsonnodes
   */
  public List<JsonNode> findImplemtationMetadata(  Map<String, JsonNode> containerResources){

    List<JsonNode> implemtationMetadata = containerResources.entrySet()
        .stream()
        .filter(jsonNode -> jsonNode.getValue().has("@type"))
        .filter(jsonNode -> jsonNode.getValue().get("@type").asText().equals("koio:Implementation"))
        .map(value -> value.getValue()).collect(Collectors.toList());

    return implemtationMetadata;

  }


  /**
   * Checks to make sure metadata follows koio
   *
   * @param filename
   * @param metadata
   */
  protected  void validateMetadata(String filename, JsonNode metadata) {
    String typeLabel = "@type", idLabel = "@id";
    String ko = "koio:KnowledgeObject", impl = "koio:Implementation";

    if (!metadata.has(idLabel) || !metadata.has(typeLabel) ) {
      throw new ShelfException("Cannot import, Missing @id in file " + filename);
    }
    if (!metadata.has(typeLabel) ) {
      throw new ShelfException("Cannot import, Missing @type label in file " + filename);
    }
    if (!ko.equals(metadata.get(typeLabel).asText()) && !impl.equals(metadata.get(typeLabel).asText())) {
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
      findImplentationBinaries( binaryResources, metadata.get("@id").asText()).forEach((binaryPath, bytes) -> {

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
   * @return
   */
  public Map<String, byte[]> findImplentationBinaries(Map<String, byte[]> binaryResources, String implementation){

    Map<String, byte[]> binaries = binaryResources.entrySet()
        .stream()
        .filter(map -> map.getKey().contains("/"+implementation+"/"))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return binaries;

  }
}
