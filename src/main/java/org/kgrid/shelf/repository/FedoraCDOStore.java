package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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

@Qualifier("fedora")
public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;

  private final RestTemplateBuilder builder = new RestTemplateBuilder();
  private String password;
  private final String storagePath;

  private final Logger log = LoggerFactory.getLogger(FedoraCDOStore.class);

  public FedoraCDOStore(
      @Value(
              "${kgrid.shelf.cdostore.url:fedora:http://localhost:8080/fcrepo/rest/?user=fedoraAdmin&password=secret3}")
          String connectionURI) {

    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    String paramDelimiter = "&", userKey = "user=", passKey = "password=";
    if (uri.getQuery() == null) {
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
  public List<URI> getChildren() {
    final String EXPANDED_CONTAINS = "http://www.w3.org/ns/ldp#contains";
    final String CONTAINS = "contains";
    final String GRAPH = "@graph";
    final String ID = "@id";

    HttpHeaders header = new HttpHeaders();
    header.add("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"");
    header.add(
        "Prefer",
        "return=\"representation\";include=\"http://fedora.info/definitions/v4/repository#EmbedResources \"");
    header.putAll(authenticationHeader().getHeaders());

    ObjectNode rdf = getJsonResource(null, header);
    ArrayList<URI> children = new ArrayList<>();
    if (rdf.has(GRAPH) && rdf.get(GRAPH).get(0).has(CONTAINS)) {
      if (!rdf.get(GRAPH).get(0).get(CONTAINS).isArray()) {
        children.add(URI.create(rdf.get(GRAPH).get(0).get(CONTAINS).asText()));
      } else {
        rdf.get(GRAPH)
            .get(0)
            .get(CONTAINS)
            .forEach(
                jsonNode ->
                    children.add(URI.create(jsonNode.asText().substring(storagePath.length()))));
      }
    } else if (rdf.has(CONTAINS)) {
      if (!rdf.get(CONTAINS).isArray()) {
        children.add(URI.create(rdf.get(CONTAINS).asText().substring(storagePath.length())));
      } else {
        rdf.get(CONTAINS)
            .forEach(
                jsonNode ->
                    children.add(URI.create(jsonNode.asText().substring(storagePath.length()))));
      }
    } else if (rdf.has(EXPANDED_CONTAINS)) {
      rdf.get(EXPANDED_CONTAINS)
          .forEach(
              jsonNode ->
                  children.add(
                      URI.create(jsonNode.get(ID).asText().substring(storagePath.length()))));
    }

    return children.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public URI getAbsoluteLocation(URI relativePath) {
    final URI baseURI = URI.create(storagePath);
    if (relativePath == null) {
      return baseURI;
    }
    return baseURI.resolve(relativePath);
  }

  @Override
  public ObjectNode getMetadata(URI relativePath) {
    return getJsonResource(relativePath);
  }

  @Override
  public byte[] getBinary(URI relativePath) {

    RestTemplate restTemplate = builder.build();

    try {

      if (relativePath.getHost().contains(getAbsoluteLocation(null).toString())) {
        throw new ShelfResourceNotFound(
            "Binary resource not located on this CDO shelf "
                + getAbsoluteLocation(null)
                + " requested path "
                + relativePath);
      }

      ResponseEntity<byte[]> response =
          restTemplate.exchange(relativePath, HttpMethod.GET, authenticationHeader(), byte[].class);
      return response.getBody();

    } catch (HttpClientErrorException ex) {
      throw new ShelfResourceNotFound("Binary resource not found " + relativePath, ex);
    }
  }

  @Override
  public void saveMetadata(JsonNode node, URI relativePath) {
    String path = relativePath.toString();
    if (path.endsWith(KoFields.METADATA_FILENAME.asStr())) {
      path = StringUtils.substringBeforeLast(path, "/");
    }
    URI destination = URI.create(path);

    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();
    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    log.info("Sending jsonLD node to the store at url " + node.toString());

    RequestEntity<String> request =
        RequestEntity.put(URI.create(destination.toString()))
            //        .header("Authorization",
            // authenticationHeader().getHeaders().getFirst("Authorization"))
            .header("Prefer", "handling=lenient; received=\"minimal\"")
            .contentType(new MediaType("application", "ld+json", StandardCharsets.UTF_8))
            .body(node.toString());
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    log.info("Saved metadata " + response);
  }

  @Override
  public void saveBinary(byte[] data, URI relativePath) {
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity<byte[]> request =
        RequestEntity.put(relativePath)
            .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
            .body(data);

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    log.info(response.toString());
  }

  @Override
  public String createTransaction() {
    URI destination = URI.create(storagePath + "fcr:tx");
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity<Object> request =
        RequestEntity.post(destination)
            .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
            .body("");

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    String transactionId =
        response.getHeaders().get("Location").get(0).substring(storagePath.length());
    if (response.getStatusCode().equals(HttpStatus.CREATED)) {
      log.info("Opening transaction with fcrepo with ID " + transactionId);
    } else {
      log.warn(
          "Attempted to open transaction with fcrepo but failed with http status "
              + response.getStatusCode());
    }
    return transactionId;
  }

  @Override
  public void commitTransaction(String transactionId) {
    URI destination = URI.create(storagePath + transactionId + "/fcr:tx/fcr:commit");
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity<Object> request =
        RequestEntity.post(destination)
            .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
            .body("");

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("Committed transaction in fcrepo with ID " + transactionId);
    } else {
      log.warn(
          "Attempted to commit transaction with id "
              + transactionId
              + " but failed with http status "
              + response.getStatusCode());
    }
  }

  @Override
  public void rollbackTransaction(String transactionId) {
    URI destination = URI.create(storagePath + transactionId + "/fcr:tx/fcr:rollback");
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity<Object> request =
        RequestEntity.post(destination)
            .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
            .body("");

    ResponseEntity<String> response = restTemplate.exchange(request, String.class);

    if (response.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("Rolled back transaction in fcrepo with ID " + transactionId);
    } else {
      log.warn(
          "Attempted to commit transaction with id "
              + transactionId
              + " but failed with http status "
              + response.getStatusCode());
    }
  }

  private ObjectNode getJsonResource(URI objectLocation) {
    HttpHeaders header = new HttpHeaders();
    header.add("Accept", "application/ld+json; profile=\"http://www.w3.org/ns/json-ld#compacted\"");
    header.add(
        "Prefer",
        "return=\"minimal\";include=\"http://fedora.info/definitions/v4/repository#EmbedResources \"");
    header.putAll(authenticationHeader().getHeaders());

    return getJsonResource(objectLocation, header);
  }

  private ObjectNode getJsonResource(URI objectURI, HttpHeaders header) {

    try {

      if (objectURI.getHost().contains(getAbsoluteLocation(null).toString())) {
        throw new ShelfResourceNotFound(
            "Metadata resource not located on this CDO shelf "
                + getAbsoluteLocation(null)
                + " requested path "
                + objectURI);
      }

      HttpClient instance =
          HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

      RestTemplate restTemplate =
          new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

      HttpEntity<String> entity = new HttpEntity<>("", header);

      if (objectURI.toString().endsWith(KoFields.METADATA_FILENAME.asStr())) {
        objectURI = URI.create(StringUtils.substringBeforeLast(objectURI.toString(), "/"));
      }
      ResponseEntity<String> response =
          restTemplate.exchange(objectURI, HttpMethod.GET, entity, String.class);

      InputStream ins = new ByteArrayInputStream(response.getBody().getBytes());

      JsonNode node = new ObjectMapper().readTree(ins);

      ins.close();

      if (node.isArray()) {
        return (ObjectNode) node.get(0);
      }
      return (ObjectNode) node;

    } catch (HttpClientErrorException
        | ResourceAccessException
        | NullPointerException
        | IOException ex) {
      throw new ShelfResourceNotFound("Metadata resource not found  " + objectURI, ex);
    }
  }

  public URI createAutoNamedContainer(URI uri) {
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    ResponseEntity<String> response =
        restTemplate.exchange(uri, HttpMethod.PUT, authenticationHeader(), String.class);

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
  public void createContainer(URI relativePath) {

    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();
    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    RequestEntity<String> request =
        RequestEntity.put(URI.create(relativePath.toString()))
            .header("Prefer", "handling=lenient; received=\"minimal\"")
            .contentType(new MediaType("application", "ld+json", StandardCharsets.UTF_8))
            .body("{}");
    ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    log.info(" metadata container created " + response);
  }

  @Override
  public void delete(URI relativePath) throws ShelfException {
    HttpClient instance =
        HttpClientBuilder.create().setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));

    try {
      restTemplate.exchange(relativePath, HttpMethod.DELETE, authenticationHeader(), String.class);

      restTemplate.exchange(
          relativePath + "/fcr:tombstone", HttpMethod.DELETE, authenticationHeader(), String.class);

    } catch (HttpClientErrorException exception) {
      log.info("Issue deleting resource " + relativePath.toString());
    }
  }
}
