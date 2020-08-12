package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.kgrid.shelf.domain.KoFields;
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
  @Autowired KnowledgeObjectRepository repo;
  @Autowired ZipImportService zipImportService;
  private ObjectMapper jsonMapper = new ObjectMapper();
  YAMLMapper yamlMapper = new YAMLMapper();

  public URI importZip(URI zipUri) throws IOException {
    Resource zipResource = applicationContext.getResource(zipUri.toString());

    final InputStream zipStream = zipResource.getInputStream();
    Map<KoFields, URI> koParts = getMetadataURIs(zipStream);
    JsonNode deploymentSpec =
        getSpecification(koParts.get(KoFields.DEPLOYMENT_SPEC_TERM), zipStream);
    JsonNode serviceSpec = getSpecification(koParts.get(KoFields.SERVICE_SPEC_TERM), zipStream);
    List<URI> artifacts = getArtifactLocations(deploymentSpec, serviceSpec);
    artifacts.addAll(koParts.values());
    URI identifier = getIdentifier(koParts.get(KoFields.METADATA_FILENAME), zipStream);
    extractAndSaveArtifacts(zipStream, artifacts, identifier);

    return identifier;
  }

  public Map<KoFields, URI> getMetadataURIs(InputStream zipStream) {

    Map<KoFields, URI> koPieces = new HashMap<>();

    ZipUtil.iterate(
        zipStream,
        (inputStream, zipEntry) -> {
          if (zipEntry.getName().endsWith(KoFields.METADATA_FILENAME.asStr())) {
            final JsonNode metadataNode = jsonMapper.readTree(inputStream.readAllBytes());
            final URI metadataLocation = URI.create(zipEntry.getName());

            koPieces.put(KoFields.METADATA_FILENAME, metadataLocation);
            koPieces.put(
                KoFields.DEPLOYMENT_SPEC_TERM,
                metadataLocation.resolve(
                    (metadataNode.at("/" + KoFields.DEPLOYMENT_SPEC_TERM.asStr()).asText())));
            koPieces.put(
                KoFields.SERVICE_SPEC_TERM,
                metadataLocation.resolve(
                    (metadataNode.at("/" + KoFields.SERVICE_SPEC_TERM.asStr()).asText())));
          }
        });

    return koPieces;
  }

  public JsonNode getSpecification(URI specLocation, InputStream zipStream) throws IOException {
    return yamlMapper.readTree(ZipUtil.unpackEntry(zipStream, specLocation.toString()));
  }

  public URI getIdentifier(URI metadataLocation, InputStream zipStream) throws IOException {
    return URI.create(
        jsonMapper
                .readTree(ZipUtil.unpackEntry(zipStream, metadataLocation.toString()))
                .get("@id")
                .asText()
            + "/");
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

  public void extractAndSaveArtifacts(InputStream zipStream, List<URI> artifacts, URI base) {
    artifacts.forEach(
        (artifact) ->
            cdoStore.saveBinary(
                ZipUtil.unpackEntry(zipStream, artifact.toString()),
                base.resolve(artifact).toString()));
  }
}
