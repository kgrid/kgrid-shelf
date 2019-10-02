package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
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

    } else {

      if (findKOMetadata(containerResources).has("identifier") &&
          findKOMetadata(containerResources).has("version")) {

        ArkId arkId = new ArkId(findKOMetadata(containerResources).get("identifier").asText());
        String version = findKOMetadata(containerResources).get("version").asText();

        importObject(arkId, version, cdoStore, containerResources, binaryResources);
        return arkId;

      } else {
        throw new ShelfException(
            "Can't import identifier and/or version are not found in the metadata");
      }
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
   *  @param arkId the objects ark
   * @param version
   * @param cdoStore ko store
   * @param containerResources collection of metadata
   * @param binaryResources collection of binaries
   */
  public void importObject(ArkId arkId, String version, CompoundDigitalObjectStore cdoStore,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources) {

    log.info("loading zip file for " + arkId.getDashArk());
    String trxId = cdoStore.createTransaction();

    try {

      ObjectNode koMetaData = (ObjectNode) findKOMetadata(containerResources);

      if (ObjectUtils.isEmpty(koMetaData)) {
        throw new ShelfException("No KO metadata found, can not import zip file");
      }

      cdoStore.createContainer(trxId, arkId.getDashArk()+"-"+version);

      KnowledgeObjectRepository knowledgeObjectRepository =
          new KnowledgeObjectRepository(cdoStore, null, null);

      binaryResources.forEach((binaryPath, bytes) -> {
            cdoStore.saveBinary(bytes, trxId, binaryPath);
      });

      cdoStore.saveMetadata(koMetaData, trxId, arkId.getDashArk()+"-"+version,
          KnowledgeObject.METADATA_FILENAME);

      cdoStore.commitTransaction(trxId);

    } catch (Exception e) {
      cdoStore.rollbackTransaction(trxId);
      log.warn(e.getMessage());
      throw new ShelfException("Could not import " +arkId, e);
    }
  }



  /**
   * Validate the metadata
   * @param filename  metadata filename
   * @param metadata jsonnode of metatdata
   */
  protected void validateMetadata(String filename, JsonNode metadata) {
    String typeLabel = "@type", idLabel = "@id", identifier="identifier", vesrsion="identifier";
    String ko = "koio:KnowledgeObject";


    if (!metadata.has(idLabel) || !metadata.has(typeLabel)) {
      throw new ShelfException("Cannot import, Missing @id in file " + filename);
    }
    if (!metadata.has(typeLabel)) {
      throw new ShelfException("Cannot import, Missing @type label in file " + filename);
    }
    if (!ko.equals(metadata.get(typeLabel).asText())) {
      throw new ShelfException(
          "Cannot import,  Missing knowledge object @type in file "
              + filename);
    }
    if (ko.equals(metadata.get(typeLabel).asText()) &&
        !filename.startsWith(metadata.get("@id").asText())) {
      throw new ShelfException(
          "Cannot import, doesn't not follow packing specifications, base directory must match ark id structure "
              + filename);
    }

  }

}
