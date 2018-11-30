package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

@Service
public class ZipExportService {


  public ByteArrayOutputStream exportCompoundDigitalObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(arkId.getDashArk());
    entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(
        arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME).toString(), true),
        koMetaData.toString().getBytes()));

    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(KnowledgeObject.IMPLEMENTATIONS_TERM);

    implementations.forEach( jsonNode ->{

      //Get and add KO Implementation metadat export zip entries
      JsonNode implementationNode = cdoStore.getMetadata(
          Paths.get(jsonNode.asText()).toString());
      entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(jsonNode.asText(), KnowledgeObject.METADATA_FILENAME).toString(),true),implementationNode.toString().getBytes()));

      //Add Implementation binary files to export zip entries
      List<String> binaryNodes = new ArrayList<>();
      if (implementationNode.has(KnowledgeObject.DEPLOYMENT_SPEC_TERM)) {
        binaryNodes.add(implementationNode.findValue(KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText());
      }
      if (implementationNode.has(KnowledgeObject.PAYLOAD_TERM)) {
        binaryNodes.add(implementationNode.findValue(KnowledgeObject.PAYLOAD_TERM).asText());
      }
      if (implementationNode.has(KnowledgeObject.SERVICE_SPEC_TERM)) {
          binaryNodes.add(implementationNode.findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText());
      }
      binaryNodes.forEach( (binaryPath) -> {
        byte[] bytes = cdoStore.getBinary(
            Paths.get( arkId.getDashArk(), binaryPath).toString());
        entries.add(new ByteSource(FilenameUtils.normalize(Paths.get(arkId.getDashArk(), binaryPath).toString(),true), bytes));
      });
    });

    //Package it all up
    ZipUtil.pack(entries.toArray(new ZipEntrySource[entries.size()]), outputStream);

    return outputStream;
  }


}
