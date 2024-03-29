package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Component
public class ManifestReader implements InitializingBean {

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    ImportService importService;

    @Value("${kgrid.shelf.manifest:}")
    String[] startupManifestLocations;

    private final Logger log = LoggerFactory.getLogger(ManifestReader.class);

    @Override
    public void afterPropertiesSet() {
        if (null != startupManifestLocations) {
            log.info("Initializing shelf with {} Manifests", startupManifestLocations.length);
            for (String location : startupManifestLocations) {
                log.info("Loading manifest from location: {}", location);
                loadManifestFromLocation(location);
            }
        }
    }

    private ArrayNode loadManifestFromLocation(String manifestLocation) {
        Resource manifestResource;
        try {
            manifestResource = applicationContext.getResource(manifestLocation);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return null;
        }

        try (InputStream stream = manifestResource.getInputStream()) {
            JsonNode manifest = mapper.readTree(stream);
            URI baseURI = getBaseUri(manifestResource);
            return loadManifest(manifest, baseURI);
        } catch (IOException e) {
            log.warn("Failed to load manifest; {}", e.getMessage());
            return null;
        }
    }

    public ArrayNode loadManifests(JsonNode manifestList) {
        ArrayNode loadedObjects = new ObjectMapper().createArrayNode();
        for (JsonNode node : manifestList) {
            ArrayNode objects = loadManifestFromLocation(node.asText());
            if (objects != null) {
                loadedObjects.addAll(objects);
            }
        }

        return loadedObjects;
    }

    public ArrayNode loadManifest(JsonNode manifest) {
        return loadManifest(manifest, null);
    }

    private ArrayNode loadManifest(JsonNode manifest, URI baseUri) {
        ArrayNode uris;
        if (manifest.has("manifest")) {
            uris = (ArrayNode) manifest.get("manifest");
        } else {
            ArrayNode idNodes = JsonNodeFactory.instance.arrayNode();
            manifest.forEach(node -> {
                idNodes.add(node.get("@id"));
            });
            uris = idNodes.deepCopy();
        }

        ArrayNode arkList = JsonNodeFactory.instance.arrayNode();
        log.info("importing {} kos", uris.size());
        uris.forEach(
                ko -> {
                    try {
                        URI koUri = URI.create(ko.asText());
                        if (baseUri != null && !koUri.isAbsolute()) {
                            koUri = baseUri.resolve(koUri);
                        }
                        log.info("import {}", koUri);
                        URI result = importService.importZip(koUri);
                        arkList.add(result.toString());
                    } catch (Exception ex) {
                        log.warn("Error importing {}, {}", ko.asText(), ex.getMessage());
                    }
                });
        return arkList;
    }

    private URI getBaseUri(Resource manifestResource) throws IOException {
        String fullUriWithFile = manifestResource.getURI().toString();
        String fullPathWithoutFile = FilenameUtils.getFullPath(fullUriWithFile);
        return URI.create(fullPathWithoutFile);
    }
}
