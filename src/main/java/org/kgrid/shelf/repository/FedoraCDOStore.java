package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kgrid.shelf.ShelfException;
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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Qualifier("fedora")
public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;


  private String password;

  private String storagePath;

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
  public List<String> getChildren(String... relativePathParts) {
    final String EXPANDED_CONTAINS = "http://www.w3.org/ns/ldp#contains";
    final String CONTAINS = "contains";
    final String GRAPH = "@graph";
    final String ID = "@id";

    ObjectNode rdf = getRdfJson(pathBuilder(relativePathParts));
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
    if(relativePathParts.length == 0) {
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
  public String getAbsoluteLocation(String... relativePathParts) {
    return pathBuilder(relativePathParts);
  }

  @Override
  public boolean isMetadata(String... relativePathParts) {
    try {
      ObjectNode json = getRdfJson(pathBuilder(relativePathParts));
      if(json.has("@type") && json.get("@type").isArray()) {
        ArrayNode types = (ArrayNode) json.get("@type");
        return types.toString().contains("ldp:RDFSource");
      } else {
        log.warn("RDF at " + pathBuilder(relativePathParts) + " has no types");
      }
    } catch (Exception ex ) {
      return false;
    }
    return false;
  }

  @Override
  public ObjectNode getMetadata(String... relativePathParts) {
    return getRdfJson(pathBuilder(relativePathParts));
  }

  @Override
  public byte[] getBinary(String... relativePathParts) {
    URI path = URI.create(pathBuilder(relativePathParts));

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    ResponseEntity<byte[]> response = restTemplate
        .exchange(path, HttpMethod.GET, authenticationHeader(), byte[].class);
    return response.getBody();
  }

  @Override
  public void saveMetadata(JsonNode node, String... relativePathParts) {
    String path = pathBuilder(relativePathParts);
    if(path.endsWith(KnowledgeObject.METADATA_FILENAME)) {
      path = StringUtils.substringBeforeLast(path, "/");
    }
    URI destination = URI.create(path);

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();
    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    log.info("Sending jsonLD node to the store at url " + node.toString());

    RequestEntity request = RequestEntity.put(URI.create(destination.toString()))
//        .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
        .header("Prefer", "handling=lenient; received=\"minimal\"")
        .contentType(new MediaType("application", "ld+json", StandardCharsets.UTF_8))
        .body(node.toString());
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    log.info("Saved metadata " + response);

  }

  @Override
  public void saveBinary(byte[] data, String... relativePathParts) {
    URI destination = URI.create(pathBuilder(relativePathParts));
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

  @Override
  public void removeFile(String... relativePathParts) {
    URI destination = URI.create(pathBuilder(relativePathParts));
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    ResponseEntity<String> response = restTemplate.exchange(destination, HttpMethod.DELETE,
        authenticationHeader(), String.class);

    if (response.getStatusCode() == HttpStatus.GONE
        || response.getStatusCode() == HttpStatus.NO_CONTENT) {

      ResponseEntity<String> tombstoneResponse = restTemplate.exchange(destination+"/fcr:tombstone", HttpMethod.DELETE,
          authenticationHeader(), String.class);

    } else {
      log.error(
          "Unable to delete fedora resource " + destination + " due to " + response.getBody());
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

  private ObjectNode getRdfJson(String objectURI) {

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    HttpHeaders headers = new HttpHeaders();
    headers
        .add("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"");
    headers.add("Prefer", "return=\"representation\";include=\"http://fedora.info/definitions/v4/repository#EmbedResources \"");
    headers.putAll(authenticationHeader().getHeaders());

    HttpEntity<String> entity = new HttpEntity<>("", headers);
    try {
      if (objectURI.endsWith(KnowledgeObject.METADATA_FILENAME)) {
        objectURI = objectURI.substring(0,
            objectURI.length() - KnowledgeObject.METADATA_FILENAME.length());
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

    } catch (HttpClientErrorException | ResourceAccessException | IOException ex) {
      throw new ShelfException("Cannot find metadata at URI " + objectURI, ex);
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
  @Override
  public void createContainer(String... relativePathParts) {

    URI destination = URI.create(pathBuilder(relativePathParts));
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();
    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity request = RequestEntity.put(URI.create(destination.toString()))
        .header("Prefer", "handling=lenient; received=\"minimal\"")
        .contentType(new MediaType("application", "ld+json", StandardCharsets.UTF_8))
        .body("{}");
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

  }

  @Override
  public void delete(String cdoIdentifier) throws ShelfException {

  }

  private String pathBuilder(String... relativePathParts) {
    StringBuilder relativePath = new StringBuilder(storagePath);
    for (String part : relativePathParts) {
      if(!relativePath.toString().endsWith("/")) {
        relativePath.append("/");
      }
      relativePath.append(part);
    }
    return relativePath.toString();
  }
}
