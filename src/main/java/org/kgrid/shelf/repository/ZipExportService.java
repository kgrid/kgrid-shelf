package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.FilenameUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.zeroturnaround.zip.ZipUtil.pack;

@Service
public class ZipExportService {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipExportService.class);

  /**
   * @param arkId export object ark id
   * @param cdoStore digital object store
   * @param koPath path to the ko
   * @return byte stream of the zip
   * @throws ShelfException export process exception
   */
  public ByteArrayOutputStream exportObject(
      ArkId arkId, String koPath, CompoundDigitalObjectStore cdoStore) throws ShelfException {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    List<ZipEntrySource> entries = new ArrayList<>();

    // Get KO and add to export zip entries
    ObjectNode koMetaData = cdoStore.getMetadata(koPath);

    // Add version binary files to export zip entries
    extractVersion(arkId, koPath, koMetaData, cdoStore, entries);

    try {
      entries.add(
          new ByteSource(
              FilenameUtils.normalize(
                  Paths.get(
                          arkId.getDashArk()
                              + "-"
                              + koMetaData.get(KoFields.VERSION.asStr()).asText(),
                          KoFields.METADATA_FILENAME.asStr())
                      .toString(),
                  true),
              JsonUtils.toPrettyString(koMetaData).getBytes()));
    } catch (IOException e) {
      throw new ShelfException("Issue extracting KO metadata for " + arkId, e);
    }

    // Package it all up
    pack(entries.toArray(new ZipEntrySource[entries.size()]), outputStream);

    return outputStream;
  }

  /**
   * Extract a single version JsonNode
   *
   * @param arkId Ark ID
   * @param cdoStore CDO Store
   * @param entries List of all of the zip entries
   * @param koNode
   * @param koPath
   */
  private void extractVersion(
      ArkId arkId,
      String koPath,
      JsonNode koNode,
      CompoundDigitalObjectStore cdoStore,
      List<ZipEntrySource> entries) {

    // Add version binary files to export zip entries
    List<String> binaryNodes;
    try {
      binaryNodes = findVersionBinaries(koPath, cdoStore, koNode);

    } catch (IOException | URISyntaxException e) {
      throw new ShelfException("Cannot export zip for object " + arkId, e);
    }
    binaryNodes.forEach(
        (binaryPath) -> {
          try {
            String uriPath =
                ResourceUtils.isUrl(binaryPath)
                    ? getResourceSlug(arkId, binaryPath)
                    : Paths.get(koPath, binaryPath).toString();

            byte[] bytes = cdoStore.getBinary(uriPath);

            // handle absolute and relative IRIs for binary filesdoc
            String binaryFileName =
                ResourceUtils.isUrl(binaryPath)
                    ? FilenameUtils.normalize(getResourceSlug(arkId, binaryPath), true)
                    : FilenameUtils.normalize(
                        Paths.get(
                                arkId.getDashArk()
                                    + "-"
                                    + koNode.get(KoFields.VERSION.asStr()).asText(),
                                binaryPath)
                            .toString(),
                        true);

            entries.add(new ByteSource(binaryFileName, bytes));

          } catch (URISyntaxException ex) {
            throw new ShelfException("Issue creating metadata file name for extract " + koPath, ex);
          }
        });
  }

  /**
   * Finds all of the binaries imported in the version folder
   *
   * @param koPath path to ko
   * @param cdoStore data store
   * @param versionNode jsonnode of version
   * @return list of binary paths for the version
   */
  private List<String> findVersionBinaries(
      String koPath, CompoundDigitalObjectStore cdoStore, JsonNode versionNode)
      throws IOException, URISyntaxException {

    List<String> binaryNodes = new ArrayList<>();

    if (versionNode.has(KoFields.DEPLOYMENT_SPEC_TERM.asStr())
        && !getSpecLocation(versionNode, KoFields.DEPLOYMENT_SPEC_TERM).startsWith("$.")) {
      binaryNodes.add(getSpecLocation(versionNode, KoFields.DEPLOYMENT_SPEC_TERM));
    }
    if (versionNode.has(KoFields.SERVICE_SPEC_TERM.asStr())) {
      binaryNodes.add(getSpecLocation(versionNode, KoFields.SERVICE_SPEC_TERM));
      getArtifacts(koPath, cdoStore, versionNode, binaryNodes);
    }

    // remove dups
    return binaryNodes.stream().distinct().collect(Collectors.toList());
  }

  private void getArtifacts(
      String koPath,
      CompoundDigitalObjectStore cdoStore,
      JsonNode versionNode,
      List<String> binaryNodes)
      throws IOException, URISyntaxException {
    YAMLMapper yamlMapper = new YAMLMapper();
    JsonNode serviceDescription =
        yamlMapper.readTree(
            cdoStore.getBinary(
                koPath,
                ResourceUtils.isUrl(getSpecLocation(versionNode, KoFields.SERVICE_SPEC_TERM))
                    ? getServiceSpecSlug(koPath, versionNode)
                    : getSpecLocation(versionNode, KoFields.SERVICE_SPEC_TERM)));

    serviceDescription
        .get("paths")
        .fields()
        .forEachRemaining(
            service -> {
              JsonNode artifact = null;
              try {
                JsonNode deploymentSpecification =
                    yamlMapper.readTree(
                        cdoStore.getBinary(
                            koPath, getSpecLocation(versionNode, KoFields.DEPLOYMENT_SPEC_TERM)));
                artifact =
                    deploymentSpecification
                        .get("endpoints")
                        .get(service.getKey())
                        .get(KoFields.ARTIFACT.asStr());
              } catch (Exception e) {
                log.info(
                    koPath
                        + " has no deployment descriptor, looking for info in the service spec.");

                JsonNode post = service.getValue().get(HttpMethod.POST.name().toLowerCase());
                if (post.has(KoFields.SERVICE_ACTIVATION_KEY.asStr())) {
                  artifact =
                      post.get(KoFields.SERVICE_ACTIVATION_KEY.asStr())
                          .get(KoFields.ARTIFACT.asStr());
                }
              }
              if (artifact != null) {
                if (artifact.isArray()) {
                  artifact.forEach((JsonNode element) -> binaryNodes.add(element.asText()));
                } else {
                  binaryNodes.add(artifact.asText());
                }
              } else {
                log.warn(
                    "Cannot find location of artifact in service spec or deployment descriptor for endpoint "
                        + service.getKey());
              }
            });
  }

  private String getSpecLocation(JsonNode versionNode, KoFields specTerm) {
    return versionNode.findValue(specTerm.asStr()).asText();
  }

  private String getServiceSpecSlug(String koPath, JsonNode versionNode) throws URISyntaxException {
    final String specPath =
        ResourceUtils.toURI(getSpecLocation(versionNode, KoFields.SERVICE_SPEC_TERM)).getPath();
    return Paths.get(specPath.substring(specPath.indexOf(koPath) + koPath.length() + 1)).toString();
  }

  private String getResourceSlug(ArkId arkId, String binaryPath) throws URISyntaxException {
    return Paths.get(
            ResourceUtils.toURI(binaryPath)
                .getPath()
                .substring(ResourceUtils.toURI(binaryPath).getPath().indexOf(arkId.getDashArk())))
        .toString();
  }
}
