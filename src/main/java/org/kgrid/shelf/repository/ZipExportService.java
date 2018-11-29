package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipExportService {

  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";
  public static final String PAYLOAD_TERM = "hasPayload";
  public static final String METADATA_FILENAME = "metadata.json";

  public ByteArrayOutputStream exportCompoundDigitalObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(arkId.getAsSimpleArk());
    entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(
        arkId.getAsSimpleArk(), METADATA_FILENAME).toString(), true),
        koMetaData.toString().getBytes()));

    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(IMPLEMENTATIONS_TERM);

    implementations.forEach( jsonNode ->{

      //Get and add KO Implementation metadat export zip entries
      JsonNode implementationNode = cdoStore.getMetadata(
          Paths.get(jsonNode.asText()).toString());
      entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(jsonNode.asText(), METADATA_FILENAME).toString(),true),implementationNode.toString().getBytes()));

      //Add Implementation binary files to export zip entries
      List<String> binaryNodes = new ArrayList<>();
      if (implementationNode.has(DEPLOYMENT_SPEC_TERM)) {
        binaryNodes.add(implementationNode.findValue(DEPLOYMENT_SPEC_TERM).asText());
      }
      if (implementationNode.has(PAYLOAD_TERM)) {
        binaryNodes.add(implementationNode.findValue(PAYLOAD_TERM).asText());
      }
      if (implementationNode.has(SERVICE_SPEC_TERM)) {
          binaryNodes.add(implementationNode.findValue(SERVICE_SPEC_TERM).asText());
      }
      binaryNodes.forEach( (binaryPath) -> {
        byte[] bytes = cdoStore.getBinary(
            Paths.get( arkId.getAsSimpleArk(), binaryPath).toString());
        entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(arkId.getAsSimpleArk(), binaryPath).toString(),true), bytes));
      });
    });

    //Package it all up
    ZipUtil.pack(entries.toArray(new ZipEntrySource[entries.size()]), outputStream);

    return outputStream;
  }


}
