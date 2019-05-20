package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
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
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipImportService extends ZipService {

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

    ArkId arkId = new ArkId( findKOMetadata(containerResources).get("@id").asText());

    importObject(arkId, cdoStore, containerResources, binaryResources);

    return arkId;
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
   * Finds the binaries for a particular implementation
   *
   * @param binaryResources
   * @param binaryPath
   * @return
   */
  public byte[] findBinaries(Map<String, byte[]> binaryResources, String binaryPath){

    Optional<byte[]> binary = binaryResources.entrySet()
        .stream()
        .filter(map -> map.getKey().endsWith(binaryPath))
        .map( map -> map.getValue())
        .findFirst();

    if(binary.isPresent()){
      return binary.get();
    } else {
      throw new ShelfException("Can't find binary at" + binaryPath);
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

  // Metadata must have an @type field that contains either koio:KnowledgeObject or koio:Implementation,
  // an @id field and an @context field, ignores top-level @graph structures
  void validateMetadata(String filename, JsonNode metadata) {
    String typeLabel = "@type", idLabel = "@id", contextLabel = "@context";
    String ko = "koio:KnowledgeObject", impl = "koio:Implementation";

    if (metadata.has("@graph")) {
      metadata = metadata.get("@graph").get(0);
    }
    if (!metadata.has(idLabel)) {
      throw new ShelfException("Cannot import ko: Missing id label in file " + filename);
    }

    if (metadata.has(typeLabel)) {
      if (metadata.get(typeLabel).isArray()) {
        boolean valid = false;
        Iterator<JsonNode> iter = metadata.get(typeLabel).elements();
        while (iter.hasNext()) {
          JsonNode typeNode = iter.next();
          if (ko.equals(typeNode.asText()) || impl.equals(typeNode.asText())) {
            valid = true;
          }
        }
        if (!valid) {
          throw new ShelfException(
              "Cannot import ko: Missing knowledge object or implementation type in file "
                  + filename);
        }
      } else if (!ko.equals(metadata.get(typeLabel).asText()) &&
          !impl.equals(metadata.get(typeLabel).asText())) {
        throw new ShelfException(
            "Cannot import ko: Missing knowledge object or implementation type in file "
                + filename);
      }
    } else {
      throw new ShelfException("Cannot import ko: Missing type field in file " + filename);
    }
    if (!metadata.has(contextLabel)) {
      throw new ShelfException("Cannot import ko: Missing context in file " + filename);
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

      List<String> binaryPaths = listBinaryNodes(metadata);

      binaryPaths.forEach((binaryPath) -> {
        try {

          byte[] binaryBytes = findBinaries( binaryResources, Paths.get(binaryPath).toString());

          Objects.requireNonNull(binaryBytes,
              "Issue importing implementation binary can not find " + Paths.get(arkId.getDashArk(), binaryPath).toString());

          cdoStore.saveBinary(binaryBytes, trxId, Paths.get(arkId.getDashArk(), binaryPath).toString());

        } catch (Exception e) {
          throw new ShelfException("Issue importing implementation binary ", e);
        }

      });

      cdoStore.saveMetadata(metadata, trxId, path, KnowledgeObject.METADATA_FILENAME);

    } catch (Exception e) {
      throw new ShelfException("Issue importing implementation ", e);
    }

  }
}
