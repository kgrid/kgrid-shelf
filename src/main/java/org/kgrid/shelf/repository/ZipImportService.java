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

    ZipUtil.iterate(zipFileStream, (inputStream, zipEntry ) -> {

      if( !zipEntry.getName().contains("__MACOSX")) {

        if (zipEntry.getName().endsWith("metadata.json")) {

          StringWriter writer = new StringWriter();
          IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);

          containerResources.put(zipEntry.getName().substring(0,
              zipEntry.getName().indexOf("metadata.json") - 1),
              new ObjectMapper().readTree(writer.toString()));

        } else if (!zipEntry.isDirectory() &&
            !zipEntry.getName().endsWith("metadata.json")) {

          binaryResources.put(zipEntry.getName(), IOUtils.toByteArray(inputStream));
        }

      }

    });

    cdoStore.createContainer( arkId.getAsSimpleArk() );

    JsonNode koMetaData = containerResources.get(
        arkId.getAsSimpleArk());

    ArrayNode arrayNode = (ArrayNode) getImplementationIDs( koMetaData );

    arrayNode.forEach( jsonNode ->{

      String path = jsonNode.asText();
      JsonNode metadata = containerResources.get(path);

      cdoStore.createContainer( path);

      List<String> binaryPaths = getImplementationBinaryPaths(metadata);

      binaryPaths.forEach( (binaryPath) -> {
        cdoStore.saveBinary( Paths.get( arkId.getAsSimpleArk(), binaryPath).toString(),
            binaryResources.get(Paths.get( arkId.getAsSimpleArk(), binaryPath).toString()));
      });

      cdoStore.saveMetadata(Paths.get(path,
          KnowledgeObject.METADATA_FILENAME).toString(), metadata);

    });

    cdoStore.saveMetadata(Paths.get(arkId.getAsSimpleArk(),
        KnowledgeObject.METADATA_FILENAME).toString(), koMetaData);
  }

  public List<String> getImplementationBinaryPaths(JsonNode node){
    List<String> binaryNodes = new ArrayList<>();
    binaryNodes.add(node.findValue(DEPLOYMENT_SPEC_TERM).asText());
    binaryNodes.add(node.findValue(PAYLOAD_TERM).asText());
    binaryNodes.add(node.findValue(SERVICE_SPEC_TERM).asText());
    return binaryNodes;
  }

  public  JsonNode getImplementationIDs(JsonNode node){
    return node.findValue(IMPLEMENTATIONS_TERM);
  }

}
