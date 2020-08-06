package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipEntryCallback;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ZipImportService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipImportService.class);

    /**
     * Create KO object, must add Knowledge Object files, Knowledge Object properties and Knowledge
     * Object version properties
     *
     * @param zipFileStream zip in the form of a stream
     * @param cdoStore      persistence layer
     * @return arkId imported arkId
     */
    public ArkId importKO(InputStream zipFileStream, CompoundDigitalObjectStore cdoStore) {

        Map<String, JsonNode> containerResources = new HashMap<>();
        Map<String, byte[]> binaryResources = new HashMap<>();
        captureZipEntries(zipFileStream, containerResources, binaryResources);
        JsonNode koMetadata = findKOMetadata(containerResources);

        if (koMetadata.has(KoFields.IDENTIFIER.asStr())
                && koMetadata.has(KoFields.VERSION.asStr())) {

            ArkId arkId =
                    new ArkId(koMetadata.get(KoFields.IDENTIFIER.asStr()).asText());
            String version = koMetadata.get(KoFields.VERSION.asStr()).asText();

            importObject(arkId, version, cdoStore, containerResources, binaryResources);
            return arkId;

        } else {
            throw new ShelfException(
                    "Can't import identifier and/or version are not found in the metadata");
        }
    }

    private void captureZipEntries(
            InputStream zipFileStream,
            Map<String, JsonNode> containerResources,
            Map<String, byte[]> binaryResources) {

        log.info("processing zipEntries");
        Map<String, JsonNode> metadataQueue = Collections.synchronizedMap(new LinkedHashMap<>());
        Map<String, byte[]> binaryQueue = Collections.synchronizedMap(new LinkedHashMap<>());

        ZipUtil.iterate(
                zipFileStream,
                zipIterator(metadataQueue, binaryQueue));

        metadataQueue.forEach(containerResources::put);

        binaryQueue.forEach(
                (filename, bytes) -> binaryResources.put(FilenameUtils.normalize(filename), bytes));
    }

    private ZipEntryCallback zipIterator(Map<String, JsonNode> metadataQueue, Map<String, byte[]> binaryQueue) {
        return (inputStream, zipEntry) -> {
            if (!zipEntry.getName().contains("__MACOSX")) {

                if (zipEntry.getName().endsWith(KoFields.METADATA_FILENAME.asStr())) {

                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);

                    JsonNode metadata = new ObjectMapper().readTree(writer.toString());

                    if (metadataIsValid(zipEntry.getName(), metadata)) {
                        metadataQueue.put(metadata.get("@id").asText(), metadata);
                    }

                } else if (!zipEntry.isDirectory()
                        && !zipEntry.getName().endsWith(KoFields.METADATA_FILENAME.asStr())) {

                    binaryQueue.put(zipEntry.getName(), IOUtils.toByteArray(inputStream));
                }
            }
        };
    }

    private JsonNode findKOMetadata(Map<String, JsonNode> containerResources) {

        Optional<JsonNode> koMetadata =
                containerResources.values().stream()
                        .filter(jsonNode -> jsonNode.has("@type"))
                        .filter(jsonNode -> jsonNode.get("@type").asText().equals("koio:KnowledgeObject"))
                        .findFirst();

        if (koMetadata.isPresent()) {
            return koMetadata.get();
        } else {
            throw new ShelfException(
                    "The imported zip is not a valid knowledge object, no valid metadata found");
        }
    }

    private void importObject(
            ArkId arkId,
            String version,
            CompoundDigitalObjectStore cdoStore,
            Map<String, JsonNode> containerResources,
            Map<String, byte[]> binaryResources) {

        log.info("loading zip file for " + arkId.getDashArk());
        String trxId = cdoStore.createTransaction();

        try {

            ObjectNode koMetaData = (ObjectNode) findKOMetadata(containerResources);

            cdoStore.createContainer(trxId, arkId.getDashArk() + "-" + version);

            binaryResources.forEach(
                    (binaryPath, bytes) ->
                            cdoStore.saveBinary(
                                    bytes,
                                    trxId,
                                    arkId.getDashArk() + "-" + version,
                                    StringUtils.substringAfter(binaryPath, File.separator)));

            cdoStore.saveMetadata(
                    koMetaData,
                    trxId,
                    arkId.getDashArk() + "-" + version,
                    KoFields.METADATA_FILENAME.asStr());

            cdoStore.commitTransaction(trxId);

        } catch (Exception e) {
            cdoStore.rollbackTransaction(trxId);
            log.warn(e.getMessage());
            throw new ShelfException("Could not import " + arkId, e);
        }
    }

    private boolean metadataIsValid(String filename, JsonNode metadata) {
        String typeLabel = "@type", idLabel = "@id";
        String ko = "koio:KnowledgeObject";

        if (!metadata.has(idLabel)) {
            return false;
        }
        if (!metadata.has(typeLabel)) {
            return false;
        }
        if (!ko.equals(metadata.get(typeLabel).asText())) {
            return false;
        }
        return true;
    }
}
