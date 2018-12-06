package org.kgrid.shelf.repository;

import static org.zeroturnaround.zip.ZipUtil.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

@Service
public class ZipExportService {


  public ByteArrayOutputStream exportCompoundDigitalObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(arkId.getDashArk());
    entries.add(new ByteSource(
        FilenameUtils.normalize(
            Paths.get(arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME).toString(), true),
        koMetaData.toString().getBytes()));

    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(KnowledgeObject.IMPLEMENTATIONS_TERM);

    if(implementations.isTextual()){

      extractImplementation(arkId, cdoStore, entries, implementations);

    } else {

      implementations.forEach(jsonNode -> {
        extractImplementation(arkId, cdoStore, entries, jsonNode);

      });

    }
    //Package it all up
    pack(entries.toArray(new ZipEntrySource[entries.size()]), outputStream);

    return outputStream;
  }

  /**
   * Extract a single Implementation JsonNode
   *
   * @param arkId Ark ID
   * @param cdoStore CDO Store
   * @param entries List of all of the zip entries
   * @param jsonNode the implementation node
   */
  protected void extractImplementation(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      List<ZipEntrySource> entries, JsonNode jsonNode) {
    String path = ResourceUtils.isUrl(jsonNode.asText())?
        jsonNode.asText(): Paths.get(jsonNode.asText()).toString();

    //Get and add KO Implementation metadat export zip entries
    JsonNode implementationNode = cdoStore.getMetadata(path);

    try {
      //handle absolute URIs with relative
      String fileName = ResourceUtils.isUrl(path) ?
          Paths.get(ResourceUtils.toURI(path).getPath().substring(
              ResourceUtils.toURI(path).getPath().indexOf(arkId.getDashArk())),
              KnowledgeObject.METADATA_FILENAME).toString() :
          FilenameUtils.normalize(
              Paths.get(jsonNode.asText(), KnowledgeObject.METADATA_FILENAME).toString(), true);

      entries.add(new ByteSource(fileName, implementationNode.toString().getBytes()));

    } catch (URISyntaxException ex){
      throw new ShelfException("Issue creating file name for extract " + jsonNode.asText(), ex);
    }

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

      try {

        String uriPath = ResourceUtils.isUrl(binaryPath)?
            binaryPath:Paths.get( arkId.getDashArk(), binaryPath).toString();

        byte[] bytes = cdoStore.getBinary(uriPath);

        String fileName = ResourceUtils.isUrl(binaryPath)?
          Paths.get(ResourceUtils.toURI(binaryPath).getPath().substring(
              ResourceUtils.toURI(binaryPath).getPath().indexOf(arkId.getDashArk()))).toString():
          FilenameUtils.normalize(Paths.get(arkId.getDashArk(), binaryPath).toString(), true);

         entries.add(new ByteSource(fileName, bytes));

      } catch (URISyntaxException ex){
        throw new ShelfException("Issue creating file name for extract " + binaryPath, ex);
      }

    });
  }


}
