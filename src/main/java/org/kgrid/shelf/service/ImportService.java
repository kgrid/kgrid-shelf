package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
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
import java.util.List;

import static org.kgrid.shelf.domain.KoFields.METADATA_FILENAME;

@Service
public class ImportService {

  @Autowired CompoundDigitalObjectStore cdoStore;
  @Autowired ApplicationContext applicationContext;

  Logger log = LoggerFactory.getLogger(ImportService.class);

  public URI importZip(URI zipUri) {
    Resource zipResource = applicationContext.getResource(zipUri.toString());
    return importZip(zipResource);
  }

  public URI importZip(MultipartFile zippedKo) {
    Resource zipResource;
    try {
      zipResource = new ByteArrayResource(zippedKo.getBytes(), zippedKo.getOriginalFilename());
    } catch (IOException e) {
      throw new ImportExportException("Couldn't handle file upload " + zippedKo.getName(), e);
    }
    return importZip(zipResource);
  }

  public URI importZip(Resource zipResource) {
    URI id;
    try {
      ZipImportReader reader = new ZipImportReader(zipResource);

      // URIs are relative to `metadata.json`; can be resolved against zip base and `@id`
      JsonNode metadata = reader.getMetadata(URI.create(METADATA_FILENAME.asStr()));

      KnowledgeObjectWrapper kow = new KnowledgeObjectWrapper(metadata);
      // get KO base URI (`@id`)
      id = kow.getId();

      JsonNode deploymentSpec = reader.getMetadata(kow.getDeploymentLocation());
      JsonNode serviceSpec = reader.getMetadata(kow.getServiceSpecLocation());

      kow.addDeployment(deploymentSpec);
      kow.addService(serviceSpec);

      copyArtifactsToShelf(reader, kow);

    } catch (Exception e) {
      final String errorMsg = "Error importing: " + zipResource.getDescription();
      log.warn(errorMsg);
      throw new ImportExportException(errorMsg, e);
    }
    return id;
  }

  private void copyArtifactsToShelf(ZipImportReader reader, KnowledgeObjectWrapper kow) {
    List<URI> artifacts = kow.getArtifactLocations();
    URI identifier = kow.getId();
    artifacts.forEach(
        artifact -> {
          try {
            byte[] data = reader.getBinary(artifact);
            cdoStore.saveBinary(data, identifier.resolve(artifact));
          } catch (IOException e) {
            cdoStore.delete(identifier);
            throw new ImportExportException(
                "Cannot read in file " + artifact + " to copy onto shelf", e);
          }
        });
  }
}
