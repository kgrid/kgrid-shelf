package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.service.ImportService;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("${kgrid.shelf.endpoint:kos}")
@CrossOrigin(origins = "${cors.url:}")
public class ImportExportController extends ShelfExceptionHandler implements InitializingBean {

    final String[] startupManifestLocations;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ObjectMapper mapper;

    ImportService importService;

    public ImportExportController(
            ImportService importService,
            KnowledgeObjectRepository shelf,
            Optional<KnowledgeObjectDecorator> kod,
            @Value("${kgrid.shelf.manifest:}") String[] startupManifestLocations) {
        super(shelf, kod);
        this.startupManifestLocations = startupManifestLocations;
        this.importService = importService;
    }

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

    Map<String, Object> loadManifestIfSet(String startupManifestLocation) {
        Resource manifestResource;
        try {
            manifestResource = applicationContext.getResource(startupManifestLocation);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return Collections.emptyMap();
        }

        try (InputStream stream = manifestResource.getInputStream()) {
            JsonNode manifest = mapper.readTree(stream);
            return depositKnowledgeObject(manifest).getBody();
        } catch (IOException e) {
            log.warn(e.getMessage());
            return Collections.emptyMap();
        }
    }

    @GetMapping(
            path = "/{naan}/{name}/{version}",
            headers = "Accept=application/zip",
            produces = "application/zip")
    public void exportKnowledgeObjectVersion(
            @PathVariable String naan,
            @PathVariable String name,
            @PathVariable String version,
            HttpServletResponse response) {

        log.info("get ko zip for " + naan + "/" + name);
        ArkId arkId = new ArkId(naan, name, version);

        exportZip(response, arkId);
    }

    @GetMapping(
            path = "/{naan}/{name}",
            headers = "Accept=application/zip",
            produces = "application/zip")
    public void exportKnowledgeObject(
            @PathVariable String naan,
            @PathVariable String name,
            @RequestParam(name = "v", required = false) String version,
            HttpServletResponse response) {

        ArkId arkId;
        if (version != null && !"".equals(version)) {
            log.info("get ko zip for " + naan + "/" + name + "/" + version);
            arkId = new ArkId(naan, name, version);
        } else {
            log.info("get ko zip for " + naan + "/" + name);
            arkId = new ArkId(naan, name);
        }
        exportZip(response, arkId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> depositKnowledgeObject(
            @RequestParam("ko") MultipartFile zippedKo) {

        log.info("Add ko via zip");
//        ArkId arkId = shelf.importZip(zippedKo);
        URI id = importService.importZip(zippedKo);

        Map<String, String> response = new HashMap<String, String>();
        HttpHeaders headers = addKOHeaderLocation(id);
        response.put("Added", id.toString());

        return new ResponseEntity<>(response, headers, HttpStatus.CREATED);
    }

    @PostMapping(path = "/manifest", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> depositKnowledgeObject(
            @RequestBody JsonNode manifest) {

        log.info("Add kos from manifest {}", manifest.asText());

        if (!manifest.has("manifest")) {
            throw new IllegalArgumentException(
                    "Provide manifest field with url or array of urls as the value");
        }

        Map<String, Object> response = new HashMap<String, Object>();
        JsonNode uris = manifest.get("manifest");
        ArrayNode arkList = new ObjectMapper().createArrayNode();
        log.info("importing {} kos", uris.size());
        uris.forEach(
                ko -> {
//                    String koLocation = ko.asText();
                    URI koUri = URI.create(ko.asText());
                    try {
//                        Resource koURL = applicationContext.getResource(koLocation);
                        log.info("import {}", koUri);
//                        InputStream zipStream = koURL.getInputStream();
//                        ArkId arkId = shelf.importZip(zipStream);
                        URI result = importService.importZip(koUri);
                        arkList.add(result.toString());
                    } catch (Exception ex) {
                        log.warn("Error importing {}, {}", koUri, ex.getMessage());
                    }
                });
        response.put("Added", arkList);
        return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
    }

    protected void exportZip(HttpServletResponse response, ArkId arkId) {

        response.setHeader("Content-Type", "application/octet-stream");
        response.addHeader(
                "Content-Disposition",
                "attachment; filename=\""
                        + (arkId.hasVersion()
                        ? arkId.getDashArk() + "-" + arkId.getVersion()
                        : arkId.getDashArk())
                        + ".zip\"");
        try {
            shelf.extractZip(arkId, response.getOutputStream());
        } catch (IOException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } finally {
            try {
                response.getOutputStream().close();
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    private HttpHeaders addKOHeaderLocation(ArkId arkId) {
        URI loc =
            ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(arkId.getSlashArk())
                .build()
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(loc);
        return headers;
    }

    private HttpHeaders addKOHeaderLocation(URI id) {
        URI loc =
            ServletUriComponentsBuilder.fromCurrentRequestUri()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();

        headers.setLocation(loc.resolve(id));
        return headers;
    }
}
