package org.kgrid.shelf.repository;

import static org.zeroturnaround.zip.ZipUtil.pack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FilenameUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

@Service
public class ZipExportService  {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipExportService.class);

  /**
   * @param arkId export object ark id
   * @param cdoStore digital object store
   * @param koPath path to the ko
   * @return byte stream of the zip
   * @throws ShelfException export process exception
   */
  public ByteArrayOutputStream exportObject(ArkId arkId,
      String koPath, CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList();

    //Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(koPath);

    //Get KO Implementations
    JsonNode implementations = koMetaData.findPath(KnowledgeObject.IMPLEMENTATIONS_TERM);

    if (arkId.isImplementation()) {

      extractImplementation(arkId, koPath, cdoStore, entries, arkId.getDashArkImplementation());
      koMetaData.put(KnowledgeObject.IMPLEMENTATIONS_TERM, arkId.getDashArkImplementation());

    } else if (implementations.isTextual()) {

      extractImplementation(arkId, koPath, cdoStore, entries, implementations.asText());

    } else {

      implementations.forEach(jsonNode -> {

        extractImplementation(arkId, koPath, cdoStore, entries, jsonNode.asText());

      });

    }

    try {
      entries.add(new ByteSource(
          FilenameUtils.normalize(
              Paths.get(arkId.getDashArk(), KnowledgeObject.METADATA_FILENAME).toString(), true),
          JsonUtils.toPrettyString(koMetaData).getBytes()));
    } catch (IOException e) {
      throw new ShelfException("Issue extracting KO metadata for " + arkId, e);
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
  private void extractImplementation(ArkId arkId, String koPath,
      CompoundDigitalObjectStore cdoStore,
      List<ZipEntrySource> entries, String implementationPath) {

    //Get and add KO Implementation metadata export zip entries
    JsonNode implementationNode = cdoStore
        .getMetadata(implementationPath.replace(arkId.getDashArk(), koPath));

    try {

      //handle absolute and relative IRIs for metadata
      String metadataFileName = ResourceUtils.isUrl(implementationPath) ?
          Paths.get(ResourceUtils.toURI(implementationPath).getPath().substring(
              ResourceUtils.toURI(implementationPath).getPath().indexOf(arkId.getDashArk())),
              KnowledgeObject.METADATA_FILENAME).toString() :
          FilenameUtils.normalize(Paths.get(implementationPath,
              KnowledgeObject.METADATA_FILENAME).toString(), true);

      try {
        entries.add(new ByteSource(metadataFileName,
            JsonUtils.toPrettyString(implementationNode).getBytes()));
      } catch (IOException e) {
        throw new ShelfException("Issue extracting Implementation metadata for " + arkId, e);
      }

      //Add Implementation binary files to export zip entries
      List<String> binaryNodes =
          findImplementationBinaries(koPath, cdoStore, implementationPath, implementationNode);


      binaryNodes.forEach((binaryPath) -> {
        try{

          String uriPath = ResourceUtils.isUrl(binaryPath) ?
            Paths.get(ResourceUtils.toURI(binaryPath).getPath().substring(
                ResourceUtils.toURI(binaryPath).getPath().indexOf(arkId.getDashArk()))).toString() :
            Paths.get(koPath, binaryPath).toString();

          byte[] bytes = cdoStore.getBinary(uriPath);

          //handle absolute and relative IRIs for binary filesdoc
          String binaryFileName = ResourceUtils.isUrl(binaryPath) ?
              Paths.get(ResourceUtils.toURI(binaryPath).getPath().substring(
                  ResourceUtils.toURI(binaryPath).getPath().indexOf(arkId.getDashArk()))).toString()
              :
                  FilenameUtils
                      .normalize(Paths.get(arkId.getDashArk(), binaryPath).toString(), true);

          entries.add(new ByteSource(binaryFileName, bytes));

        } catch (URISyntaxException ex) {
          throw new ShelfException(
              "Issue creating metadata file name for extract " + implementationPath, ex);
        }

      });

    } catch (URISyntaxException ex) {
      throw new ShelfException(
          "Issue creating metadata file name for extract " + implementationPath, ex);
    }
  }

  /**
   * Finds all of the binaries imported in the implementation folder
   *
   * @param koPath path to ko
   * @param cdoStore data store
   * @param implementationPath path to implementation
   * @param implementationNode jsonnode of implementation
   * @return list of binary paths for the implementation
   */
  protected List<String> findImplementationBinaries(String koPath,
      CompoundDigitalObjectStore cdoStore,
      String implementationPath, JsonNode implementationNode) {

    List<String> binaryNodes = new ArrayList<>();

    if (implementationNode.has(KnowledgeObject.DEPLOYMENT_SPEC_TERM)) {
      binaryNodes.add(implementationNode.findValue(KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText());
    }
    if (implementationNode.has(KnowledgeObject.SERVICE_SPEC_TERM)) {
      binaryNodes.add(implementationNode.findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText());

    }

    if (implementationNode.has(KnowledgeObject.SERVICE_SPEC_TERM)) {
      YAMLMapper yamlMapper = new YAMLMapper();
      try {

        JsonNode serviceDescription = yamlMapper
            .readTree(cdoStore.getBinary(koPath,
                ResourceUtils.isUrl(implementationNode
                .findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText())?
                    Paths.get(ResourceUtils.toURI(implementationNode
                        .findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText()).getPath().substring(
                        ResourceUtils.toURI(implementationNode
                            .findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText()).getPath().indexOf(koPath)+koPath.length()+1)).toString():
                    implementationNode.findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText()));



        serviceDescription.get("paths").fields().forEachRemaining(service -> {
          String artifact = null;
          try {
            JsonNode deploymentSpecification = yamlMapper
                .readTree(cdoStore.getBinary(koPath, implementationNode
                    .findValue(KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText()));
            artifact = deploymentSpecification.get("endpoints").get(service.getKey())
                .get("artifact").asText();
          } catch (Exception e) {
            log.info(implementationPath
                + " has no deployment descriptor, looking for info in the service spec.");

            JsonNode post = service.getValue().get("post");
            if (post.has("x-kgrid-activation")) {
              artifact = post.get("x-kgrid-activation").get("artifact").asText();
            }
          }
          if(artifact != null) {
            binaryNodes.add(Paths.get(implementationNode.get("@id").asText(), artifact).toString());
          } else {
            log.warn("Cannot find location of artifact in service spec or deployment descriptor for endpoint " + service.getKey());
          }
        });

      } catch (IOException ioe) {

        log.info(implementationPath, " has no service descriptor, can't export");

      } catch (URISyntaxException urie) {
        log.info(implementationPath, " issue handling URI process of path");
      }
    }

    // remove dups
    return binaryNodes.stream().distinct().collect(Collectors.toList());
  }

}
