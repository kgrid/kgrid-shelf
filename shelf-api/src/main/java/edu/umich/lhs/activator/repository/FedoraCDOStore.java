package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


public class FedoraCDOStore implements CompoundDigitalObjectStore {

  private String userName;

  private String password;

  private URI storagePath;

  private final Logger log = LoggerFactory.getLogger(FedoraCDOStore.class);

  public FedoraCDOStore(String userName, String password, URI storagePath) {
    this.userName = userName;
    this.password = password;
    this.storagePath = storagePath;
  }

  @Override
  public List<String> getChildren(URI filePath) {
    Model rdf = null;
    try {
       rdf = getRdfJson(new URI(storagePath.toString() + filePath.toString()));
    } catch (URISyntaxException ex) {
      ex.printStackTrace();
    }
    List<String> children = new ArrayList<>();
    StmtIterator iterator = rdf.listStatements();
    while(iterator.hasNext()) {

      Statement statement = iterator.nextStatement();
      if(statement.getPredicate().getLocalName().equals("contains")) {
        children.add(statement.getObject().toString().substring(storagePath.toString().length()));
      }
    }
    return children;
  }

  @Override
  public URI getAbsoluteLocation(URI relativePath) {
    try {
      if(relativePath != null) {
        return new URI(storagePath.toString() + "/" + relativePath.toString());
      } else {
        return storagePath;
      }
    } catch (URISyntaxException e) {
      log.warn("Cannot make uri for path " + relativePath + " " + e);
      return null;
    }
  }

  @Override
  public ObjectNode getMetadata(URI relativePath) {

    try {
      Model metadataRDF = getRdfJson(new URI(storagePath + "/" + relativePath));
      StringWriter stringWriter = new StringWriter();
      metadataRDF.write(stringWriter, "JSON-LD");
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode node = (ObjectNode)mapper.readTree(stringWriter.toString());
      return node;
    } catch (URISyntaxException | IOException e) {
      log.error("Cannot get metadata from location " + relativePath + " " + e);
      return null;
    }

  }

  @Override
  public byte[] getBinary(URI relativePath) {
    URI path = null;
    try {
      path = new URI(storagePath + "/" + relativePath);
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
  public void saveMetadata(URI relativePath, JsonNode node) {
    try {
      URI destination = new URI(storagePath + "/" + relativePath);
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
  public void saveBinary(URI relativePath, byte[] data) {
    try {
      URI destination = new URI(storagePath + "/" + relativePath);
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

    try {
      ZipInputStream zis = new ZipInputStream(zip.getInputStream());
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {

        if (!entry.getName().contains("/.")) {
          URI dir = new URI(entry.getName());
          if (!entry.isDirectory()) {
            StringBuilder dataString = new StringBuilder();
            Scanner sc = new Scanner(zis);
            if(entry.getName().endsWith("metadata.json")) {
              while (sc.hasNextLine()) {
                dataString.append(sc.nextLine());
              }
              dir = new URI(dir.toString().substring(0, dir.toString().length() - ("metadata.json".length() + 1)));
              JsonNode node = new ObjectMapper().readTree(dataString.toString());
              saveMetadata(dir, node);
            } else {
              while (sc.hasNextLine()) {
                dataString.append(sc.nextLine());
              }
              saveBinary(dir, dataString.toString().getBytes());

            }
          }
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
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
  public void removeFile(URI relativePath) {
    try {
      URI destination = new URI(storagePath + "/" + relativePath);
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

    Model model = ModelFactory.createDefaultModel().read(ins, this.storagePath.toString(), "JSON-LD");

    try {
      ins.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    return model;

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
