package kgrid.org.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Component
@Qualifier("fedora")
public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;

  private String password;

  private URI storagePath;

  private final Logger log = LoggerFactory.getLogger(FedoraCDOStore.class);

  public FedoraCDOStore (@Value("${shelf.location:.}") String storagePath, @Value("${fedora.username:}") String userName, @Value("${fedora.password:}") String password) {
    try {
      this.storagePath = new URI(storagePath);
      this.userName = userName;
      this.password = password;
    } catch (URISyntaxException e) {
      log.error("Cannot create storage path URI from " + storagePath + " " + e);
    }
  }

  @Override
  public List<Path> getChildren(Path filePath) {
    try {
      URI metadataURI =  new URI(storagePath.toString() + filePath.toString());
       ObjectNode rdf = getRdfJson(metadataURI);
      ArrayList<Path> children = new ArrayList<>();
        rdf.get("http://www.w3.org/ns/ldp#contains").forEach(jsonNode -> {
          children.add(Paths.get(jsonNode.get("@id").asText()));
        });
      return children;
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    }

  }

  @Override
  public Path getAbsoluteLocation(Path relativePath) {
    if(relativePath != null) {
      return Paths.get(storagePath.toString()  + relativePath.toString());
    } else {
      return Paths.get(storagePath.toString());
    }
  }

  @Override
  public ObjectNode getMetadata(Path relativePath) {
    try {
      return getRdfJson(new URI(storagePath + relativePath.toString()));
    } catch (URISyntaxException ex) {
      log.error("Cannot create uri from path " + relativePath);
      return null;
    }
  }

  @Override
  public byte[] getBinary(Path relativePath) {
    URI path = null;
    try {
      path = new URI(storagePath + relativePath.toString());
    } catch (URISyntaxException e) {
      log.warn(e.getMessage());
    }
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(instance));
    ResponseEntity<byte[]> response = restTemplate.exchange(path, HttpMethod.GET, authenticationHeader(), byte[].class);
    return response.getBody();
  }

  @Override
  public void saveMetadata(Path relativePath, JsonNode node) {
    try {
      URI destination = new URI(storagePath + relativePath.toString());
      HttpClient instance = HttpClientBuilder.create()
          .setRedirectStrategy(new DefaultRedirectStrategy()).build();
      RestTemplate restTemplate = new RestTemplate(
          new HttpComponentsClientHttpRequestFactory(instance));
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(new MediaType("application", "n-triples"));
      headers.putAll(authenticationHeader().getHeaders());

      Model metadataModel = ModelFactory.createDefaultModel();
      Resource resource = metadataModel.createResource(destination.toString());
      addJsonToRdfResource(node, resource, metadataModel);

      StringWriter writer = new StringWriter();
      metadataModel.write(writer, "N-TRIPLE");
      RequestEntity request = RequestEntity.put(new URI(destination.toString() + "/fcr:metadata"))
          .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
          .header("Prefer", "handling=lenient; received=\"minimal\"")
          .contentType(new MediaType("application", "n-triples", StandardCharsets.UTF_8))
          .body(writer.toString());
      ResponseEntity<String> response = restTemplate.exchange(request, String.class);
    } catch (URISyntaxException e) {
      log.warn("Cannot put metadata at location " + relativePath + " " + e);
    }
  }

  @Override
  public void saveBinary(Path relativePath, byte[] data) {
    try {
      URI destination = new URI(storagePath + relativePath.toString());
      HttpClient instance = HttpClientBuilder.create()
          .setRedirectStrategy(new DefaultRedirectStrategy()).build();

      RestTemplate restTemplate = new RestTemplate(
          new HttpComponentsClientHttpRequestFactory(instance));

      RequestEntity request = RequestEntity.put(destination)
          .header("Authorization", authenticationHeader().getHeaders().getFirst("Authorization"))
          .body(data);

      ResponseEntity<String> response = restTemplate.exchange(request, String.class);
      log.info(response.toString());
    } catch (URISyntaxException e) {
      log.error("Cannot create URI " + e);
    }
  }

  @Override
  public ObjectNode addCompoundObjectToShelf(MultipartFile zip) {

    try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {

        if (!entry.getName().contains("/.")) {
          String dir = entry.getName();
          if (!entry.isDirectory()) {
            StringBuilder dataString = new StringBuilder();
            Scanner sc = new Scanner(zis);
            if(entry.getName().endsWith("metadata.json")) {
              while (sc.hasNextLine()) {
                dataString.append(sc.nextLine());
              }
              dir = dir.substring(0, dir.length() - ("metadata.json".length() + 1));
              JsonNode node = new ObjectMapper().readTree(dataString.toString());
              saveMetadata(Paths.get(dir), node);
            } else {
              while (sc.hasNextLine()) {
                dataString.append(sc.nextLine());
              }
              saveBinary(Paths.get(dir), dataString.toString().getBytes());

            }
          }
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return null;
  }

  @Override
  public void getCompoundObjectFromShelf(Path relativeDestination, OutputStream outputStream) throws IOException {
    // Todo: implement me
  }

  private void addJsonToRdfResource(JsonNode json, Resource resource, Model metadataModel) {
    json.fields().forEachRemaining(element -> {
      if(element.getValue().isObject()) {
        addJsonToRdfResource(element.getValue(), resource, metadataModel);
      } else if (element.getValue().isArray()) {
        element.getValue().elements().forEachRemaining(arrayElement -> addJsonToRdfResource(arrayElement, resource, metadataModel));
      } else {
        resource.addLiteral(
            metadataModel.createProperty("http://kgrid.org/ko#" + element.getKey()),
            element.getValue().asText());
      }
    });
  }

  @Override
  public void removeFile(Path relativePath) {
    try {
      URI destination = new URI(storagePath + relativePath.toString());
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
    } catch (URISyntaxException e) {
      log.error("Cannot remove file at " + relativePath);
    }
    
  }

  public ObjectNode getRdfJson(URI objectURI) {

    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(new MediaType("application", "ld+json")));
    headers.putAll(authenticationHeader().getHeaders());

    HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
    try {
      if(objectURI.toString().endsWith("metadata.json")) {
        objectURI = new URI(objectURI.toString().substring(0, objectURI.toString().length() - "metadata.json".length()));
      }
      ResponseEntity<String> response = restTemplate.exchange(objectURI, HttpMethod.GET,
          entity, String.class);

      InputStream ins = new ByteArrayInputStream(response.getBody().getBytes());

      JsonNode node = new ObjectMapper().readTree(ins);

      ins.close();

      if(node.isArray()) {
        return (ObjectNode)node.get(0);
      }
      return (ObjectNode)node;

    } catch (HttpClientErrorException | URISyntaxException | IOException ex) {
      throw new IllegalArgumentException("Cannot find object at URI " + objectURI);
    }
  }

  public URI createContainer(URI uri) throws URISyntaxException {
    HttpClient instance = HttpClientBuilder.create()
        .setRedirectStrategy(new DefaultRedirectStrategy()).build();

    RestTemplate restTemplate = new RestTemplate(
        new HttpComponentsClientHttpRequestFactory(instance));

    ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PUT,
        authenticationHeader(), String.class);

    return new URI(response.getHeaders().getFirst("Location"));
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
