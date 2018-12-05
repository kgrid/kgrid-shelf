package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipImportService {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipImportService.class);

  /**
   * Create KO object, must add Knowledge Object files, Knowledge Object properties and
   * Knowledge Object Implementation properties
   *
   * @param arkId ark id of the importing object
   * @param zipFileStream zip in the form of a stream
   * @param cdoStore persistence layer
   */
  public void importCompoundDigitalObject(ArkId arkId, InputStream zipFileStream,
      CompoundDigitalObjectStore cdoStore) {

    Map<String, JsonNode> containerResources = new HashMap<>();
    Map<String, byte[]> binaryResources = new HashMap<>();

    log.info("loading zip file for " + arkId.getDashArk());
    captureZipEntries(zipFileStream, containerResources, binaryResources);

    cdoStore.createContainer( arkId.getDashArk() );

    JsonNode koMetaData = containerResources.get(
        arkId.getDashArk());

    if(KnowledgeObject.getImplementationIDs( koMetaData ).isArray()){

      JsonNode implementationNodes = KnowledgeObject.getImplementationIDs( koMetaData );
      implementationNodes.forEach( jsonNode ->{

        importImplementation(arkId, cdoStore, containerResources, binaryResources, jsonNode);

      });

    } else {

      importImplementation(arkId, cdoStore, containerResources, binaryResources,
          KnowledgeObject.getImplementationIDs( koMetaData ));

    }

    cdoStore.saveMetadata(koMetaData, arkId.getDashArk(),
        KnowledgeObject.METADATA_FILENAME);
    cdoStore.saveMetadata(koMetaData, arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME);

  }

  /**
   * Captures the Zip Entries loading a collection of metadata and collection of
   * binaries
   *
   * @param zipFileStream zip file in a stream
   * @param containerResources collection of metadata files
   * @param binaryResources collection of binary files
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
   * @param arkId Ark Id of the object
   * @param cdoStore persistence layer
   * @param containerResources metadata load from the zip
   * @param binaryResources binaries load based on the metadata in the zip
   * @param jsonNode implementation node
   */
  protected void importImplementation(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      Map<String, JsonNode> containerResources, Map<String, byte[]> binaryResources,
      JsonNode jsonNode) {
    String path = jsonNode.asText();
    JsonNode metadata = containerResources.get(Paths.get(path).toString());

    cdoStore.createContainer( path);

    List<String> binaryPaths = getImplementationBinaryPaths(metadata);

    binaryPaths.forEach( (binaryPath) -> {

      byte[] binaryBytes =  binaryResources.get( Paths.get(arkId.getDashArk(), binaryPath).toString());

      Objects.requireNonNull(binaryBytes,
          "Can't find linked file " + Paths.get(arkId.getDashArk(), binaryPath).toString());

      cdoStore.saveBinary(binaryBytes, arkId.getDashArk(), binaryPath);

    });

    cdoStore.saveMetadata(metadata, path,  KnowledgeObject.METADATA_FILENAME);
  }

  /**
   * Give a JsonNode this will look for the KOIO defined
   * Implementation binaries for deployment, service and payload
   *
   * @param node implementation json node
   * @return paths to the binaries that define service, deployment and payload
   */
  public List<String> getImplementationBinaryPaths(JsonNode node){
    List<String> binaryNodes = new ArrayList<>();
    if (node.has(KnowledgeObject.DEPLOYMENT_SPEC_TERM)) {
      binaryNodes.add(node.findValue(KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText());
    }
    if (node.has(KnowledgeObject.PAYLOAD_TERM)) {
      binaryNodes.add(node.findValue(KnowledgeObject.PAYLOAD_TERM).asText());
    }
    if (node.has(KnowledgeObject.SERVICE_SPEC_TERM)) {
      binaryNodes.add(node.findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText());
    }
    return binaryNodes;
  }


}
