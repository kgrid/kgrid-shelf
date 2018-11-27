package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.beans.factory.annotation.Autowired;


@Category(FedoraIntegrationTest.class)
public class FedoraZipImportServiceTest {

  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";
  public static final String PAYLOAD_TERM = "hasPayload";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ZipImportService service = new ZipImportService();

  static FedoraCDOStore compoundDigitalObjectStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");


  @Test
  public void testImportKnowledgeObject()  {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-world-jsonld.zip");

    service.importCompoundDigitalObject(new ArkId("hello", "world"), zipStream, compoundDigitalObjectStore);

    ObjectNode metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "world").getAsSimpleArk() );

    assertNotNull(metadata);

    assertEquals("should have 2 implementations", 2,
        metadata.findValue(IMPLEMENTATIONS_TERM).size());

    metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "world").getAsSimpleArk()+"/"+ "v0.0.1" );

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-world/v0.0.1/welcome.js",
        metadata.findValue(PAYLOAD_TERM).asText());

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-world/v0.0.1/service-specification.yaml",
        metadata.findValue(SERVICE_SPEC_TERM).asText());

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-world/v0.0.1/deployment-specification.yaml",
        metadata.findValue(DEPLOYMENT_SPEC_TERM).asText());

  }


  @Test
  public void testImportKnowledgeObjectExtraFiles()  {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-usa-jsonld.zip");

    service.importCompoundDigitalObject(new ArkId("hello", "usa"), zipStream, compoundDigitalObjectStore);

    ObjectNode metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "usa").getAsSimpleArk() );

    assertEquals("should have 2 implementations", 2,
        metadata.findValue(IMPLEMENTATIONS_TERM).size());

    try {
      metadata = compoundDigitalObjectStore.getMetadata(
          Paths.get(new ArkId("hello", "world").getAsSimpleArk(), "v0.0.3").toString());
      assertTrue("Should throw exception", false);
    } catch (IllegalArgumentException e){
      assertTrue("Should not find v.0.0.3 because not defined in meatadata, not found will throw exception", true);

    }

  }

  @Test
  public void testImportMixedFormatKnowledgeObject()  {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-koio.zip");

    service.importCompoundDigitalObject(new ArkId("hello", "koio"), zipStream, compoundDigitalObjectStore);


    ObjectNode metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "koio").getAsSimpleArk() );

    assertEquals("should have 1 implementations", "http://localhost:8080/fcrepo/rest/hello-koio/koio",
        metadata.findValue(IMPLEMENTATIONS_TERM).asText());


    metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "koio").getAsSimpleArk()+"/"+ "koio" );

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-koio/koio/welcome.js",
        metadata.findValue(PAYLOAD_TERM).asText());

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-koio/koio/service-specification.yaml",
        metadata.findValue(SERVICE_SPEC_TERM).asText());

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-koio/koio/deployment-specification.yaml",
        metadata.findValue(DEPLOYMENT_SPEC_TERM).asText());


  }

}