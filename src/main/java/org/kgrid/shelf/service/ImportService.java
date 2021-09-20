package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;

import static org.kgrid.shelf.domain.KoFields.METADATA_FILENAME;

@Service
public class ImportService {

    @Autowired
    CompoundDigitalObjectStore cdoStore;
    @Autowired
    KnowledgeObjectRepository koRepo;
    @Autowired
    ApplicationContext applicationContext;

    Logger log = LoggerFactory.getLogger(ImportService.class);

    public URI importZip(URI zipUri) throws MalformedURLException {
        URL url = new URL(zipUri.toString());
        if (url.getProtocol().contains("http")) {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Accept", "application/zip");
            httpHeaders.add("Accept",MediaType.APPLICATION_OCTET_STREAM_VALUE);
            HttpEntity<HttpHeaders> httpEntity = new HttpEntity<>(httpHeaders);
            ResponseEntity<Resource> zipResource =
                    new RestTemplate().exchange(url.toString(), HttpMethod.GET, httpEntity, Resource.class);
            return importZip(zipResource.getBody());
        } else {
            Resource zipResource = applicationContext.getResource(zipUri.toString());
            return importZip(zipResource);
        }
    }

    public URI importZip(MultipartFile zippedKo) {
        Resource zipResource = zippedKo.getResource();
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
            koRepo.addKnowledgeObjectToLocationMap(id, metadata);

        } catch (Exception e) {
            final String errorMsg =
                    "Error importing: " + zipResource.getDescription() + ", " + e.getMessage();
            log.warn(e.getMessage());
            throw new ImportExportException(errorMsg, e);
        }
        return id;
    }

    private void copyArtifactsToShelf(ZipImportReader reader, KnowledgeObjectWrapper kow) {
        HashSet<URI> artifacts = kow.getArtifactLocations();
        URI identifier = kow.getId();
        artifacts.forEach(
                artifact -> {
                    try {
                        InputStream data = reader.getFileStream(artifact);
                        cdoStore.saveBinary(data, identifier.resolve(artifact));
                    } catch (IOException e) {
                        cdoStore.delete(identifier);
                        throw new ImportExportException(
                                "Cannot read in file " + artifact + " to copy onto shelf", e);
                    }
                });
    }
}
