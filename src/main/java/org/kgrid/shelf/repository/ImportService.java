package org.kgrid.shelf.repository;

import static org.kgrid.shelf.domain.KoFields.DEPLOYMENT_SPEC_TERM;
import static org.kgrid.shelf.domain.KoFields.METADATA_FILENAME;
import static org.kgrid.shelf.domain.KoFields.SERVICE_SPEC_TERM;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportService {
  @Autowired CompoundDigitalObjectStore cdoStore;
  @Autowired ApplicationContext applicationContext;
  private ObjectMapper jsonMapper = new ObjectMapper();
  YAMLMapper yamlMapper = new YAMLMapper();

  Logger log = LoggerFactory.getLogger(ImportService.class);

  public URI importZip(URI zipUri) throws IOException {
    Resource zipResource = applicationContext.getResource(zipUri.toString());
    URI id = importZip(zipResource);
    return id;
  }

  public URI importZip(Resource zipResource) {
    URI id = null;
    try {
      // URIs are relative to `metadata.json`; can be resolved against zip base and `@id`
      Map<KoFields, URI> koParts = getMetadataURIs(zipResource.getInputStream());

      // get KO base URI (`@id`)
      id = getId(koParts.get(METADATA_FILENAME), zipResource.getInputStream());

      // get zip base metadata URI from `metadata.json` artifact relative URI
      URI zipBase = getZipBase(koParts.get(METADATA_FILENAME));
      koParts.replace(METADATA_FILENAME, URI.create(METADATA_FILENAME.asStr()));

      JsonNode deploymentSpec = getSpecification(
          koParts.get(DEPLOYMENT_SPEC_TERM), zipResource.getInputStream(), zipBase);
      JsonNode serviceSpec = getSpecification(
          koParts.get(SERVICE_SPEC_TERM), zipResource.getInputStream(), zipBase);

      // fetch all artifact locations from deployment spec (and possibly payload).
      List<URI> artifacts = getArtifactLocations(deploymentSpec, serviceSpec);
      artifacts.addAll(koParts.values());

      extractAndSaveArtifacts(zipResource, artifacts, id, zipBase);
    } catch (Exception e) {
      final String errorMsg = "Error importing: " + zipResource.getDescription();
      log.warn(errorMsg);
      throw new ImportExportException(errorMsg, e);
    }
    return id;
  }

  public URI getZipBase(URI manifestLocation) {
    return URI.create(StringUtils.removeEnd(
        manifestLocation.toString(),
        METADATA_FILENAME.asStr()
    ));
  }

  public Map<KoFields, URI> getMetadataURIs(InputStream zipStream) {

    Map<KoFields, URI> koPieces = new HashMap<>();

    ZipUtil.iterate(
        zipStream,
        (inputStream, zipEntry) -> {
          if (zipEntry.getName().endsWith(METADATA_FILENAME.asStr())) {
            final JsonNode metadataNode = jsonMapper.readTree(inputStream.readAllBytes());
            final URI metadataLocation = URI.create(zipEntry.getName());

            koPieces.put(METADATA_FILENAME, metadataLocation);
            koPieces.put(
                DEPLOYMENT_SPEC_TERM,
                URI.create(metadataNode.at("/" + DEPLOYMENT_SPEC_TERM.asStr()).asText()));
            koPieces.put(
                SERVICE_SPEC_TERM,
               URI.create(metadataNode.at("/" + SERVICE_SPEC_TERM.asStr()).asText()));
          }
        });

    return koPieces;
  }

  public JsonNode getSpecification(URI specLocation, InputStream zipStream,
      URI zipBase) throws IOException {

    final String location = zipBase.resolve(specLocation).toString();
    final byte[] content = ZipUtil.unpackEntry(zipStream, location);
    final JsonNode jsonNode = yamlMapper.readTree(content);

    return jsonNode;
  }

  public URI getId(URI metadataLocation, InputStream zipStream) throws IOException {
    final byte[] content = ZipUtil.unpackEntry(zipStream, metadataLocation.toString());
    final String str = jsonMapper.readTree(content).get("@id").asText() + "/";
    final URI uri = URI.create(str);
    return uri;
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

  public void extractAndSaveArtifacts(
      Resource zipResource, List<URI> artifacts, URI identifier, URI zipBase) {
    artifacts.forEach(
        (artifact) -> {
          URI cdoUri;
          final byte[] data;
          URI zipURI = zipBase.resolve(artifact);
          cdoUri = identifier.resolve(artifact);
          try (InputStream zipStream = zipResource.getInputStream()) {
            data = ZipUtil.unpackEntry(zipStream, zipURI.toString());
          } catch (IOException e) {
            throw new ImportExportException(
                "Cannot save " + zipURI + " -> " + cdoUri,
                e);
          }
          cdoStore.saveBinary(
              data,
              cdoUri.toString());
        });
  }
}
