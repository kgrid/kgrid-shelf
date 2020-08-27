package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ManifestReader implements InitializingBean {

  @Autowired ApplicationContext applicationContext;
  @Autowired ObjectMapper mapper;
  @Autowired ImportService importService;

  @Value("${kgrid.shelf.manifest:}")
  String[] startupManifestLocations;

  Logger log = LoggerFactory.getLogger(ManifestReader.class);

  @Override
  public void afterPropertiesSet() {
    if (null != startupManifestLocations) {
      log.info("Initializing shelf with {} Manifests", startupManifestLocations.length);
      for (String location : startupManifestLocations) {
        log.info("Loading manifest from location: {}", location);
        loadManifestIfSet(location);
      }
    }
  }

  private void loadManifestIfSet(String startupManifestLocation) {
    Resource manifestResource;
    try {
      manifestResource = applicationContext.getResource(startupManifestLocation);
    } catch (Exception e) {
      log.warn(e.getMessage());
      return;
    }

    try (InputStream stream = manifestResource.getInputStream()) {
      JsonNode manifest = mapper.readTree(stream);
      loadManifest(manifest);
    } catch (IOException e) {
      log.warn(e.getMessage());
    }
  }

  public Map<String, ArrayNode> loadManifest(JsonNode manifest) {

    if (!manifest.has("manifest")) {
      throw new IllegalArgumentException(
          "Provide manifest field with url or array of urls as the value");
    }

    Map<String, ArrayNode> response = new HashMap<>();

    JsonNode uris = manifest.get("manifest");
    ArrayNode arkList = mapper.createArrayNode();
    log.info("importing {} kos", uris.size());
    uris.forEach(
        ko -> {
          URI koUri = URI.create(ko.asText());
          try {
            log.info("import {}", koUri);
            URI result = importService.importZip(koUri);
            arkList.add(result.toString());
          } catch (Exception ex) {
            log.warn("Error importing {}, {}", koUri, ex.getMessage());
          }
        });
    response.put("Added", arkList);
    return response;
  }
}
