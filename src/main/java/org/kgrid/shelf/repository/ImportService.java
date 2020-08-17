package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.kgrid.shelf.domain.KoFields.*;

@Service
public class ImportService {
  @Autowired CompoundDigitalObjectStore cdoStore;
  @Autowired ApplicationContext applicationContext;
  private final ObjectMapper jsonMapper = new ObjectMapper();
  private final YAMLMapper yamlMapper = new YAMLMapper();

  Logger log = LoggerFactory.getLogger(ImportService.class);

  public URI importZip(URI zipUri) {
    Resource zipResource = applicationContext.getResource(zipUri.toString());
    URI id = importZip(zipResource);
    return id;
  }

  public URI importZip(MultipartFile zippedKo) {
    Resource zipResource;
    try {
      zipResource = new ByteArrayResource(zippedKo.getBytes());
    } catch (IOException e) {
      throw new ImportExportException("Couldn't handle file upload " + zippedKo.getName(), e);
    }
    URI id = importZip(zipResource);
    return id;
  }

  public URI importZip(Resource zipResource) {

    URI id;
    try {
      File koBase = createKoBase(zipResource);
      // URIs are relative to `metadata.json`; can be resolved against zip base and `@id`
      JsonNode metadata = getSpecification(koBase, URI.create(METADATA_FILENAME.asStr()));
      // get KO base URI (`@id`)
      id = getId(metadata);
      Map<KoFields, URI> koParts = getKoParts(metadata);

      JsonNode deploymentSpec = getSpecification(koBase, koParts.get(DEPLOYMENT_SPEC_TERM));
      JsonNode serviceSpec = getSpecification(koBase, koParts.get(SERVICE_SPEC_TERM));

      // fetch all artifact locations from deployment spec (and possibly payload).
      List<URI> artifacts = getArtifactLocations(deploymentSpec, serviceSpec);
      artifacts.addAll(koParts.values());

      copyArtifactsToShelf(koBase, id, artifacts);
    } catch (Exception e) {
      final String errorMsg = "Error importing: " + zipResource.getDescription();
      log.warn(errorMsg);
      throw new ImportExportException(errorMsg, e);
    }
    return id;
  }

  private File createKoBase(Resource zipResource) throws IOException {
    String koName = StringUtils.removeEnd(zipResource.getFilename(), ".zip");
    File parentDir = unzipToTemp(zipResource.getInputStream());
    FileUtils.forceDeleteOnExit(parentDir);
    File koBase = new File(parentDir, koName);
    return koBase;
  }

  private File unzipToTemp(InputStream inputStream) {
    try {
      File temp = Files.createTempDirectory("ko").toFile();
      ZipUtil.unpack(inputStream, temp);
      return temp;
    } catch (IOException e) {
      throw new ImportExportException("Cannot create temporary directory to unpack zip", e);
    }
  }

  public Map<KoFields, URI> getKoParts(JsonNode metadataNode) {

    Map<KoFields, URI> koParts = new HashMap<>();
    koParts.put(METADATA_FILENAME, URI.create(METADATA_FILENAME.asStr()));
    koParts.put(
        DEPLOYMENT_SPEC_TERM,
        URI.create(metadataNode.at("/" + DEPLOYMENT_SPEC_TERM.asStr()).asText()));
    koParts.put(
        SERVICE_SPEC_TERM, URI.create(metadataNode.at("/" + SERVICE_SPEC_TERM.asStr()).asText()));
    return koParts;
  }

  public JsonNode getSpecification(File koBase, URI specName) throws IOException {

    final File specFile = new File(koBase, specName.toString());

    JsonNode jsonNode;
    if (specName.getPath().endsWith(".json")) {
      jsonNode = jsonMapper.readTree(specFile);
    } else {
      jsonNode = yamlMapper.readTree(specFile);
    }

    return jsonNode;
  }

  public URI getId(JsonNode metadata) {
    String id = metadata.get("@id").asText() + "/";
    return URI.create(id);
  }

  public List<URI> getArtifactLocations(JsonNode deploymentSpec, JsonNode serviceSpec) {

    List<URI> artifacts = new ArrayList<>();
    deploymentSpec
        .at("/" + KoFields.ENDPOINTS.asStr())
        .forEach(
            endpoint -> {
              final JsonNode artifactNode = endpoint.get(KoFields.ARTIFACT.asStr());
              if (artifactNode.isArray()) {
                artifactNode.forEach(node -> artifacts.add(URI.create(node.asText())));
              } else {
                artifacts.add(URI.create(artifactNode.asText()));
              }
            });
    return artifacts;
  }

  public void copyArtifactsToShelf(File koBase, URI identifier, List<URI> artifacts) {
    artifacts.forEach(
        artifact -> {
          File artifactFile = new File(koBase, artifact.toString());
          try {
            final byte[] data = Files.readAllBytes(artifactFile.toPath());
            cdoStore.saveBinary(data, identifier.resolve(artifact).toString());
          } catch (IOException e) {
            throw new ImportExportException(
                "Cannot read in file " + artifactFile + " to copy onto shelf", e);
          }
        });
  }
}
