package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;

  private String password;

  private URI storagePath;

  @Override
  public List<String> getChildren(Path filePath) {
    Model rdf = getRdfJson(filePath.toUri());
    return null;
  }

  @Override
  public Path getAbsolutePath(Path filePath) {
    return null;
  }

  @Override
  public ObjectNode getMetadata(Path filePath) {
    return null;
  }

  @Override
  public byte[] getBinary(Path filePath) {
    return new byte[0];
  }

  @Override
  public void saveMetadata(Path destination, JsonNode metadata) {

  }

  @Override
  public void saveBinary(Path destination, byte[] data) {

  }

  @Override
  public ObjectNode addCompoundObjectToShelf(MultipartFile zip) {
    return null;
  }

  @Override
  public void removeFile(Path filePath) throws IOException {
    
  }

  public Model getRdfJson(URI objectURI) {

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(new MediaType("application", "ld+json")));
    headers.putAll(authenticationHeader().getHeaders());

    HttpEntity<String> entity = new HttpEntity<>("parameters", headers);

    ResponseEntity<String> response = restTemplate.exchange(objectURI, HttpMethod.GET,
        entity, String.class);

    InputStream ins = new ByteArrayInputStream(response.getBody().getBytes());

    return ModelFactory.createDefaultModel().read(ins, this.storagePath.toString(), "JSON-LD");

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
