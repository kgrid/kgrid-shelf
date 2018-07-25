package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileUtils;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Qualifier("fedora")
public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;

  private String password;

  private String storagePath;

  private static final String KGRID_NAMESPACE = "http://kgrid.org/ko#";

  private final Logger log = LoggerFactory.getLogger(FedoraCDOStore.class);

  public FedoraCDOStore(
      @Value("${kgrid.shelf.cdostore.url:fedora:http://localhost:8080/fcrepo/rest/?user=fedoraAdmin&password=secret3}") String connectionURI) {

    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    String paramDelimiter = "&", userKey = "user=", passKey = "password=";
    if(uri.getQuery() == null) {
      this.storagePath = uri.toString();
    } else {
      this.storagePath = uri.toString().substring(0, uri.toString().indexOf("?"));
      String[] parameters = uri.getQuery().split(paramDelimiter);
      for (String parameter : parameters) {
        if (parameter.startsWith(userKey)) {
          this.userName = parameter.substring(userKey.length());
        } else if (parameter.startsWith(passKey)) {
          this.password = parameter.substring(passKey.length());
        }
      }
    }
  }

  @Override
  public List<String> getChildren(String relativePath) {
    final String EXPANDED_CONTAINS = "http://www.w3.org/ns/ldp#contains";
    final String CONTAINS = "contains";
    final String GRAPH = "@graph";
    final String ID = "@id";

    URI metadataURI;

    if (relativePath != null) {
      metadataURI = URI.create(storagePath + relativePath);
    } else {
      metadataURI = URI.create(storagePath);
    }
    ObjectNode rdf = getRdfJson(metadataURI);
    ArrayList<String> children = new ArrayList<>();
    if (rdf.has(GRAPH) && rdf.get(GRAPH).get(0).has(CONTAINS)) {
      if(!rdf.get(GRAPH).get(0).get(CONTAINS).isArray()) {
        children.add((rdf.get(GRAPH).get(0).get(CONTAINS).asText()));
      } else {
        rdf.get(GRAPH).get(0).get(CONTAINS).forEach(jsonNode ->
            children.add(jsonNode.asText().substring(storagePath.length()))
        );
      }
    } else if (rdf.has(CONTAINS)) {
      if(!rdf.get(CONTAINS).isArray()) {
        children.add(rdf.get(CONTAINS).asText().substring(storagePath.length()));
      } else {
        rdf.get(CONTAINS).forEach(jsonNode ->
            children.add(jsonNode.asText().substring(storagePath.length()))
        );
      }
    } else if (rdf.has(EXPANDED_CONTAINS)) {
      rdf.get(EXPANDED_CONTAINS).forEach(jsonNode ->
          children.add(jsonNode.get(ID).asText().substring(storagePath.length()))
      );
    }

    // At the top level fcrepo returns the full arkid/version when it is deposited with arkid/version
    //  so we need to just return the arkid
    if(relativePath == null) {
      return children.stream().map(path -> {
        String parent = StringUtils.substringBefore(path, "/");
        if(parent != null && ! parent.isEmpty())
          return parent;
        else return path;
      }).filter(Objects::nonNull).collect(Collectors.toList());
    }
    return children;
  }

  @Override
  public String getAbsoluteLocation(String relativePath) {
    if (relativePath != null) {
      return storagePath + relativePath;
    } else {
      return storagePath;
    }
  }

  @Override
  public boolean isMetadata(String relativePath) {
    ObjectNode json = getRdfJson(URI.create(storagePath + relativePath));
    if(json.has("@type") && json.get("@type").isArray()) {
      ArrayNode types = (ArrayNode) json.get("@type");
      return types.toString().contains("ldp:RDFSource");
    } else {
      log.warn("RDF at " + relativePath + " has no types");
    }
    return false;
  }

  @Override
  public ObjectNode getMetadata(String relativePath) {
    return getRdfJson(URI.create(storagePath + relativePath));
  }

  @Override
  public byte[] getBinary(String relativePath) {
    URI path = URI.create(storagePath + relativePath);

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    ResponseEntity<byte[]> response = restTemplate
        .exchange(path, HttpMethod.GET, authenticationHeader(), byte[].class);
    return response.getBody();
  }

  @Override
  public void saveMetadata(String relativePath, JsonNode node) {

    URI destination = URI.create(storagePath + relativePath);
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();
    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    Model metadataModel = ModelFactory.createDefaultModel();
    Resource resource = metadataModel.createResource(destination.toString());

    // Doing this to add a namespace to objects so we can convert from json to rdf triples
    // When we are fully json-ld this can possibly be eliminated
    serializeJsonToRdfResource(node, resource, metadataModel);

    StringWriter writer = new StringWriter();
    metadataModel.write(writer, FileUtils.langNTriple);

    RequestEntity request = RequestEntity.put(URI.create(destination.toString() + "/fcr:metadata"))
        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .header("Prefer", "handling=lenient; received=\"minimal\"")
        .contentType(new MediaType("application", "n-triples", StandardCharsets.UTF_8))
        .body(writer.toString());
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    log.info("Saved metadata " + response);

  }

  @Override
  public void saveBinary(String relativePath, byte[] data) {
    URI destination = URI.create(storagePath + relativePath);
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity request = RequestEntity.put(destination)
        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .body(data);

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    log.info(response.toString());
  }

  @Override
  public ArkId addCompoundObjectToShelf(ArkId urlArkId, MultipartFile zip) {
    int totalSize = 0;
    int entries = 0;

    String transactionId = createTransaction();

    try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
      ZipEntry entry;
      ArkId arkId = null;
      String topLevelFolderName = null;
      while ((entry = zis.getNextEntry()) != null) {
        if(topLevelFolderName == null) {
          topLevelFolderName = entry.getName();
          if (topLevelFolderName.contains("/")) {
            arkId = new ArkId(StringUtils.substringBefore(topLevelFolderName, "/"));
          } else {
            arkId = new ArkId(topLevelFolderName);
          }
          if (!arkId.equals(urlArkId)) {
            rollbackTransaction(transactionId);
            throw new InputMismatchException(
                "URL does not match internal id in zip file url ark=" + urlArkId + " zipped ark="
                    + arkId);
          }
        }
        if (!entry.getName().contains("/.")) {
          String dir = entry.getName();
          if (!entry.isDirectory()) {
            byte[] zipContents = IOUtils.toByteArray(zis);
            if (entry.getName().endsWith(KnowledgeObject.METADATA_FILENAME)) {
              // chop off the "/metadata.json" off the end of rdf metadata
              dir = StringUtils.substringBeforeLast(dir, "/");
              JsonNode node = new ObjectMapper().readTree(zipContents);
              saveMetadata(transactionId + "/" + dir, node);
            } else {
              saveBinary(transactionId + "/" + dir, zipContents);
            }

            // Prevent zip bombs from using all available resources
            entries++;
            totalSize+= zipContents.length;
            if (entries > 1024) {
              throw new IllegalStateException(
                  "Zip file " + zip.getName() + " has too many files in it to unzip.");
            }
            if (totalSize > 0x6400000) { // Over 100 MB
              log.error("Zip file " + zip.getName() + " has too many files in it to unzip.");
              throw new IllegalStateException(
                  "Zip file " + zip.getName() + " is too large to unzip.");
            }
          }
        }
      }
      commitTransaction(transactionId);
      return arkId;

    } catch (HttpClientErrorException hcee) {
      rollbackTransaction(transactionId);
      throw new IllegalStateException("Cannot overwrite existing knowledge object in fedora");
    } catch (IOException ex) {
      rollbackTransaction(transactionId);
      log.warn("Cannot load zip into fedora " + ex.getMessage());
      throw new IllegalArgumentException(ex);
    }
  }

  @Override
  public void getCompoundObjectFromShelf(String relativeDestination, boolean isVersion,
      OutputStream outputStream) throws IOException {
    getCompoundObjectFromShelf(relativeDestination, outputStream, 100);
  }

  private void getCompoundObjectFromShelf(String relativeDestination, OutputStream outputStream,
      int maxDepth) throws IOException {
    ZipOutputStream zos = new ZipOutputStream(outputStream);
    List<String> descendants = getAllFedoraDescendants(relativeDestination, maxDepth);
    descendants.add(relativeDestination); // Add top-level metadata
    for (String path : descendants) {
      ZipEntry zipEntry;
      try {
        JsonNode metadata = getMetadata(path);
        zipEntry = new ZipEntry(path + "/" + KnowledgeObject.METADATA_FILENAME);
        zos.putNextEntry(zipEntry);
        zos.write(metadata.toString().getBytes());
        zos.closeEntry();
      } catch (IOException ex) {
        log.error("Cannot write file " + path + " to the zip file " + ex);
      } catch (IllegalArgumentException e) {
        try {
          zipEntry = new ZipEntry(path);
          zos.putNextEntry(zipEntry);
          zos.write(getBinary(path));
          zos.closeEntry();
        } catch (IOException ex) {
          log.error("Cannot write file " + path + " to the zip file " + ex);
        }
      }
    }
    zos.close();
  }

  private List<String> getAllFedoraDescendants(String start, int maxLevels) {
    ArrayList<String> descendants = new ArrayList<>();
    List<String> children;
    try {
      children = getChildren(start);
    } catch (IllegalArgumentException e) {
      return descendants;
    }
    if (maxLevels > 0 && children != null && !children.isEmpty()) {

      for (String child : children) {
        descendants.addAll(getAllFedoraDescendants(child, maxLevels - 1));
      }
      descendants.addAll(children);
    }
    return descendants;
  }

  private void serializeJsonToRdfResource(JsonNode json, Resource resource, Model metadataModel) {
    json.fields().forEachRemaining(element -> {
      if (element.getValue().isObject()) {
        serializeJsonToRdfResource(element.getValue(), resource, metadataModel);
      } else if (element.getValue().isArray()) {
        element.getValue().elements().forEachRemaining(
            arrayElement -> serializeJsonToRdfResource(arrayElement, resource, metadataModel));
      } else {
        resource.addLiteral(
            metadataModel.createProperty(KGRID_NAMESPACE + element.getKey()),
            element.getValue().asText());
      }
    });
  }

  @Override
  public void removeFile(String relativePath) {
    URI destination = URI.create(storagePath + relativePath);
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    ResponseEntity<String> response = restTemplate.exchange(destination, HttpMethod.DELETE,
        authenticationHeader(), String.class);

    if (response.getStatusCode() == HttpStatus.GONE
        || response.getStatusCode() == HttpStatus.NO_CONTENT) {
      log.info("Fedora resource " + relativePath + " deleted.");
    } else {
      log.error(
          "Unable to delete fedora resource " + relativePath + " due to " + response.getBody());
    }
  }

  private String createTransaction() {
    URI destination = URI.create(storagePath + "fcr:tx");
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity request = RequestEntity.post(destination)
        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .body(null);

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    String transactionId = response.getHeaders().get("Location").get(0).substring(storagePath.length());
    if(response.getStatusCode().equals(HttpStatus.CREATED)) {
      log.info("Opening transaction with fcrepo with ID " + transactionId);
    } else {
      log.warn("Attempted to open transaction with fcrepo but failed with http status " + response.getStatusCode());
    }
    return transactionId;
  }

  private void commitTransaction(String transactionId) {
    URI destination = URI.create(storagePath + transactionId + "/fcr:tx/fcr:commit");
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity request = RequestEntity.post(destination)
        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .body(null);


    ResponseEntity response = restTemplate.exchange(request, String.class);

    if(response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("Committed transaction in fcrepo with ID " + transactionId);
    } else {
      log.warn("Attempted to commit transaction with id " + transactionId + " but failed with http status " + response.getStatusCode());
    }
  }

  private void rollbackTransaction(String transactionId) {
    URI destination = URI.create(storagePath + transactionId + "/fcr:tx/fcr:rollback");
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity request = RequestEntity.post(destination)
        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .body(null);

    ResponseEntity response = restTemplate.exchange(request, String.class);

    if(response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("Rolled back transaction in fcrepo with ID " + transactionId);
    } else {
      log.warn("Attempted to commit transaction with id " + transactionId + " but failed with http status " + response.getStatusCode());
    }
  }

  private ObjectNode getRdfJson(URI objectURI) {

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    HttpHeaders headers = new HttpHeaders();
    headers
        .add("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"");
    headers.add("Prefer", "return=\"representation\";");
    headers.putAll(authenticationHeader().getHeaders());

    HttpEntity<String> entity = new HttpEntity<>("", headers);
    try {
      if (objectURI.toString().endsWith(KnowledgeObject.METADATA_FILENAME)) {
        objectURI = URI.create(objectURI.toString().substring(0,
            objectURI.toString().length() - KnowledgeObject.METADATA_FILENAME.length()));
      }
      ResponseEntity<String> response = restTemplate.exchange(objectURI, HttpMethod.GET,
          entity, String.class);

      InputStream ins = new ByteArrayInputStream(response.getBody().getBytes());

      JsonNode node = new ObjectMapper().readTree(ins);

      ins.close();

      if (node.isArray()) {
        return (ObjectNode) node.get(0);
      }
      return (ObjectNode) node;

    } catch (HttpClientErrorException | IOException ex) {
      throw new IllegalArgumentException("Cannot find object at URI " + objectURI, ex);
    }
  }

  public URI createContainer(URI uri) {
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT,
        authenticationHeader(), String.class);

    return URI.create(response.getHeaders().getFirst("Location"));
  }

  private HttpEntity<HttpHeaders> authenticationHeader() {
    final String CHARSET = "US-ASCII";
    HttpHeaders header = new HttpHeaders();
    String auth = userName + ":" + password;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName(CHARSET)));
    String authHeader = "Basic " + new String(encodedAuth);
    header.set("Authorization", authHeader);
    return new HttpEntity<>(header);
  }

}
