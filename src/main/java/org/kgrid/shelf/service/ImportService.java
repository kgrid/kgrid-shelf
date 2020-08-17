package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.shelf.domain.KoFields;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.kgrid.shelf.domain.KoFields.*;

@Service
public class ImportService {

  @Autowired
  CompoundDigitalObjectStore cdoStore;
  @Autowired ApplicationContext applicationContext;

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
      ZipImportReader reader = new ZipImportReader(zipResource);

      // URIs are relative to `metadata.json`; can be resolved against zip base and `@id`
      JsonNode metadata = reader.getMetadata(URI.create(METADATA_FILENAME.asStr()));
      // get KO base URI (`@id`)

//      KnowledgeObjectWrapper kow = new KnowledgeObjectWrapper(metadata);
      id = getId(metadata);
      Map<KoFields, URI> koParts = getKoParts(metadata);

//      JsonNode deploymentSpec = reader.getMetadata(kow.SERVICE_SPEC);
      JsonNode deploymentSpec = reader.getMetadata(koParts.get(DEPLOYMENT_SPEC_TERM));
      JsonNode serviceSpec = reader.getMetadata(koParts.get(SERVICE_SPEC_TERM));

//      kow.add(deploymentSpec);
//      kow.add(serviceSpec);

      //fetch all artifact locations from deployment spec (and possibly payload).
      List<URI> artifacts = getArtifactLocations(deploymentSpec, serviceSpec);
      artifacts.addAll(koParts.values());

      copyArtifactsToShelf(reader, id, artifacts);

//      copyArtifactsToShelf(reader, kow);


    } catch (Exception e) {
      final String errorMsg = "Error importing: " + zipResource.getDescription();
      log.warn(errorMsg);
      throw new ImportExportException(errorMsg, e);
    }
    return id;
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

  public void copyArtifactsToShelf(ZipImportReader reader, URI identifier,
      List<URI> artifacts) {
    artifacts.forEach(
        artifact -> {
          try {
            byte[] data = reader.getBinary(artifact);
            cdoStore.saveBinary(data, identifier.resolve(artifact).toString());
          } catch (IOException e) {
            throw new ImportExportException(
                "Cannot read in file " + artifact + " to copy onto shelf", e);
          }
        });
  }
}
