package org.kgrid.shelf.repository;

import static org.zeroturnaround.zip.ZipUtil.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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


  public ByteArrayOutputStream exportObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(arkId.getDashArk());

    entries.add(new ByteSource(
        FilenameUtils.normalize(
            Paths.get(arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME).toString(), true),
        prettyPrintJsonString(koMetaData).getBytes()));


    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(KnowledgeObject.IMPLEMENTATIONS_TERM);

    if (arkId.isImplementation()){

      extractImplementation(arkId, cdoStore, entries, arkId.getDashArkImplementation());

    } else if (implementations.isTextual()){

      extractImplementation(arkId, cdoStore, entries, implementations.asText());

    } else {

      implementations.forEach(jsonNode -> {

        extractImplementation(arkId, cdoStore, entries, jsonNode.asText());

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
   * @param implementationPath the implementation path absolute and relative IR
   */
  private void extractImplementation(ArkId arkId, CompoundDigitalObjectStore cdoStore,
      List<ZipEntrySource> entries, String implementationPath) {

    String path = ResourceUtils.isUrl(implementationPath)?
        implementationPath: Paths.get(implementationPath).toString();

    //Get and add KO Implementation metadat export zip entries
    JsonNode implementationNode = cdoStore.getMetadata(path);

    try {

      //handle absolute and relative IRIs for metadata
      String fileName = ResourceUtils.isUrl(path) ?
          Paths.get(ResourceUtils.toURI(path).getPath().substring(
              ResourceUtils.toURI(path).getPath().indexOf(arkId.getDashArk())),
              KnowledgeObject.METADATA_FILENAME).toString() :
          FilenameUtils.normalize(
              Paths.get(implementationPath, KnowledgeObject.METADATA_FILENAME).toString(), true);

      entries.add(new ByteSource(fileName, prettyPrintJsonString(implementationNode).getBytes()));

    } catch (URISyntaxException ex){
      throw new ShelfException("Issue creating file name for extract " + implementationPath, ex);
    }

    //Add Implementation binary files to export zip entries
    List<String> binaryNodes = listBinaryNodes(implementationNode);

    binaryNodes.forEach( (binaryPath) -> {

      try {

        String uriPath = ResourceUtils.isUrl(binaryPath)?
            binaryPath:Paths.get( arkId.getDashArk(), binaryPath).toString();

        byte[] bytes = cdoStore.getBinary(uriPath);

        //handle absolute and relative IRIs for binary filesdoc
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

  /**
   * Get a list of implementation paths, service, deployment and payload
   *
   * @param implementationNode
   * @return
   */
  private List<String> listBinaryNodes(JsonNode implementationNode) {
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
    return binaryNodes;
  }

  /**
   * Format a JsonNode
   *
   * @param jsonNode
   * @return  formatted JsonNode as a string
   */
  private String prettyPrintJsonString(JsonNode jsonNode) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Object json = mapper.readValue(jsonNode.toString(), Object.class);
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    } catch (Exception e) {
      return "Sorry, pretty print didn't work";
    }
  }
}
