package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipImportService {
  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";
  public static final String PAYLOAD_TERM = "hasPayload";
  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipImportService.class);

  /**
   * Create KO object, must add Knowledge Object files, Knowledge Object properties and
   * Knowledge Object Implementation properties
   *
   * @param zipFileStream
   * @param cdoStore
   */
  public void importCompoundDigitalObject(ArkId arkId, InputStream zipFileStream,CompoundDigitalObjectStore cdoStore) {

    Map<String, JsonNode> containerResources = new HashMap<>();
    Map<String, byte[]> binaryResources = new HashMap<>();

    log.info("loading zip file for " + arkId.getAsSimpleArk());
    captureZipEntries(zipFileStream, containerResources, binaryResources);

    cdoStore.createContainer( arkId.getAsSimpleArk() );

    JsonNode koMetaData = containerResources.get(
        arkId.getAsSimpleArk());

    ArrayNode arrayNode = (ArrayNode) getImplementationIDs( koMetaData );

    importImplementations(arkId, cdoStore, containerResources, binaryResources, arrayNode);

    cdoStore.saveMetadata(koMetaData, arkId.getAsSimpleArk(),
        KnowledgeObject.METADATA_FILENAME);

  }

  /**
   * Captures the Zip Entries loading a collection of metadata and collection of
   * binaries
   *
   * @param zipFileStream
   * @param containerResources
   * @param binaryResources
   */
  protected void captureZipEntries(InputStream zipFileStream,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources) {

    log.info("processing zipEntries");

    ZipUtil.iterate(zipFileStream, (inputStream, zipEntry ) -> {

      if( !zipEntry.getName().contains("__MACOSX")) {

        if (zipEntry.getName().endsWith(KnowledgeObject.METADATA_FILENAME)) {

          StringWriter writer = new StringWriter();
          IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);

          containerResources.put( FilenameUtils.normalize(
              zipEntry.getName().substring(0,
                  zipEntry.getName().indexOf(KnowledgeObject.METADATA_FILENAME) - 1)),
              new ObjectMapper().readTree(writer.toString()));

        } else if (!zipEntry.isDirectory() &&
            !zipEntry.getName().endsWith(KnowledgeObject.METADATA_FILENAME)) {

          binaryResources.put(FilenameUtils.normalize(zipEntry.getName()),
              IOUtils.toByteArray(inputStream));
        }

      }

    });
  }

  /**
   * Imports the KO Implementations loading the metadata and binaries
   *
   * @param arkId
   * @param cdoStore
   * @param containerResources
   * @param binaryResources
   * @param arrayNode
   */

  protected void importImplementations(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources,
      ArrayNode arrayNode) {

    arrayNode.forEach( jsonNode ->{

      String path = jsonNode.asText();
      JsonNode metadata = containerResources.get(Paths.get(path).toString());

      cdoStore.createContainer( path);

      List<String> binaryPaths = getImplementationBinaryPaths(metadata);

      binaryPaths.forEach( (binaryPath) -> {
        cdoStore.saveBinary(binaryResources.get( Paths.get(arkId.getAsSimpleArk(), binaryPath).toString()),
            arkId.getAsSimpleArk(), binaryPath);
      });

      cdoStore.saveMetadata(metadata, path,  KnowledgeObject.METADATA_FILENAME);

    });

  }

  /**
   * Give a JsonNode this will look for the KOIO defined
   * Implementation binaries for deployment, service and payload
   *
   * @param node
   * @return
   */
  public List<String> getImplementationBinaryPaths(JsonNode node){
    List<String> binaryNodes = new ArrayList<>();
    if (node.has(DEPLOYMENT_SPEC_TERM)) {
      binaryNodes.add(node.findValue(DEPLOYMENT_SPEC_TERM).asText());
    }
    if (node.has(PAYLOAD_TERM)) {
      binaryNodes.add(node.findValue(PAYLOAD_TERM).asText());
    }
    if (node.has(SERVICE_SPEC_TERM)) {
      binaryNodes.add(node.findValue(SERVICE_SPEC_TERM).asText());
    }
    return binaryNodes;
  }

  public  JsonNode getImplementationIDs(JsonNode node){
    return node.findValue(IMPLEMENTATIONS_TERM);
  }

}
