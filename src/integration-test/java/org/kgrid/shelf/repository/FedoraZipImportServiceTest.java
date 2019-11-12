package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;


@Category(FedoraIntegrationTest.class)
public class FedoraZipImportServiceTest {

  public static final String IMPLEMENTATIONS_TERM = "hasImplementation";
  public static final String SERVICE_SPEC_TERM = "hasServiceSpecification";
  public static final String DEPLOYMENT_SPEC_TERM = "hasDeploymentSpecification";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ZipImportService service = new ZipImportService();

   FedoraCDOStore compoundDigitalObjectStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");


  @Test
  public void testImportKnowledgeObject()  {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-world.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    ObjectNode metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "world").getDashArk() );

    assertNotNull(metadata);

    assertEquals("should have 2 versions", 3,
        metadata.findValue(IMPLEMENTATIONS_TERM).size());


    metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "world").getDashArk()+"/"+ "v0.1.0" );

    assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-world/v0.1.0/service.yaml",
        metadata.findValue(SERVICE_SPEC_TERM).asText());

    compoundDigitalObjectStore.delete("hello-world");

  }

  @Test
  public void testImportKnowledgeObjectFolder()  {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/mycoolko.zip");

    try {
      service.importKO(zipStream, compoundDigitalObjectStore);
      ObjectNode metadata = compoundDigitalObjectStore.getMetadata( new ArkId("hello", "folder").getDashArk() +"/"+ "v0.1.0");
      assertEquals("should have ", "http://localhost:8080/fcrepo/rest/hello-folder/v0.1.0/service-specification.yaml",
          metadata.findValue(SERVICE_SPEC_TERM).asText());

    } catch (ShelfException e) {
      assertTrue("Should not be able to import an object with a folder name that does not match the ark id", true);
    }

    compoundDigitalObjectStore.delete("hello-folder");

  }

  @Test
  public void testBadKOMetaData() throws IOException {

    InputStream zipStream = FedoraZipImportServiceTest.class
        .getResourceAsStream("/fixtures/bad-kometadata.zip");

    try{

      service.importKO(zipStream, compoundDigitalObjectStore);

      fail("should throw exception");

    } catch (ShelfException se){

      try {
        ObjectNode metadata = compoundDigitalObjectStore.getMetadata(
            Paths.get(new ArkId("hello", "world").getDashArk()).toString());
        assertTrue("Should throw exception", false);
      } catch (ShelfException e){
        assertTrue("Should not find  hello world because not defined in meatadata, not found will throw exception", true);

      }
    }

  }

}
