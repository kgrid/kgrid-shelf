package org.kgrid.shelf.repository;

import static org.zeroturnaround.zip.ZipUtil.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
public class ZipExportService extends ZipService {

  private boolean stripBase = false;
  private String basePath="";

  /**
   *
   * @param arkId KO to expoprt
   * @param cdoStore
   * @param stripBase
   * @return
   * @throws ShelfException
   */
  public ByteArrayOutputStream exportObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore, boolean stripBase) throws ShelfException {

    this.stripBase=stripBase;
    this.basePath = cdoStore.getAbsoluteLocation("");

    return exportObject(arkId, cdoStore);
  }

  /**
   *
   * @param arkId
   * @param cdoStore
   * @return
   * @throws ShelfException
   */
  public ByteArrayOutputStream exportObject(ArkId arkId,
      CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(arkId.getDashArk());

    entries.add(new ByteSource(
        FilenameUtils.normalize(
            Paths.get(arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME).toString(), true),
        formatExportedMetadata(koMetaData)));

    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(KnowledgeObject.IMPLEMENTATIONS_TERM);

    if (arkId.isImplementation()) {

      extractImplementation(arkId, cdoStore, entries, arkId.getDashArkImplementation());

    } else if (implementations.isTextual()) {

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

    try {

      String path = ResourceUtils.isUrl(implementationPath) ?
        implementationPath : Paths.get(implementationPath).toString();

      //Get and add KO Implementation metadata export zip entries
      JsonNode implementationNode = cdoStore.getMetadata(path);

      //handle absolute and relative IRIs for metadata
      String metadataFileName = ResourceUtils.isUrl(path) ?
          Paths.get(ResourceUtils.toURI(path).getPath().substring(
              ResourceUtils.toURI(path).getPath().indexOf(arkId.getDashArk())),
              KnowledgeObject.METADATA_FILENAME).toString() :
          FilenameUtils.normalize(Paths.get(implementationPath,
              KnowledgeObject.METADATA_FILENAME).toString(), true);

      entries.add(new ByteSource(metadataFileName, formatExportedMetadata(implementationNode)));

      //Add Implementation binary files to export zip entries
      List<String> binaryNodes = listBinaryNodes(implementationNode);

      binaryNodes.forEach((binaryPath) -> {

        try {

          String uriPath = ResourceUtils.isUrl(binaryPath) ?
              binaryPath : Paths.get(arkId.getDashArk(), binaryPath).toString();

          byte[] bytes = cdoStore.getBinary(uriPath);

          //handle absolute and relative IRIs for binary filesdoc
          String binaryFileName = ResourceUtils.isUrl(binaryPath) ?
              Paths.get(ResourceUtils.toURI(binaryPath).getPath().substring(
                  ResourceUtils.toURI(binaryPath).getPath().indexOf(arkId.getDashArk()))).toString() :
              FilenameUtils.normalize(Paths.get(arkId.getDashArk(), binaryPath).toString(), true);

          entries.add(new ByteSource(binaryFileName, bytes));

        } catch (URISyntaxException ex) {
          throw new ShelfException("Issue creating binary name for extract " + binaryPath, ex);
        }

      });

    } catch (URISyntaxException ex) {
      throw new ShelfException("Issue creating metadata file name for extract " + implementationPath, ex);
    }


  }

  /**
   * Format JSON metadata based on json ld java core, also strip out based from IRI is needed
   *
   * @return formatted JsonNode as a array of byes
   */
  private byte[] formatExportedMetadata(JsonNode jsonNode) {

    DocumentLoader documentLoader = new DocumentLoader();
    Object context = null;

    try {

      Object json = JsonUtils.fromString(jsonNode.toString());

      if (jsonNode.findValue("@type").textValue().contains("KnowledgeObject")) {
        context = JsonUtils
            .fromURL(new URL("http://kgrid.org/koio/contexts/knowledgeobject.jsonld"),
                documentLoader.getHttpClient());
      } else if (jsonNode.findValue("@type").textValue().contains("Implementation")) {
        context = JsonUtils
            .fromURL(new URL("http://kgrid.org/koio/contexts/implementation.jsonld"),
                documentLoader.getHttpClient());
      }

      JsonLdOptions options = new JsonLdOptions();

      if (stripBase) {
        options.setBase(basePath);
      }

      Object compact = JsonLdProcessor.compact(json, context, options);

      byte[] nodeBytes = JsonUtils.toPrettyString(compact).getBytes();
      return nodeBytes;
    } catch (IOException e) {
      throw new ShelfException("Could not create extracted metadata for "
          + jsonNode.findValue("@id"), e);
    }
  }
}
