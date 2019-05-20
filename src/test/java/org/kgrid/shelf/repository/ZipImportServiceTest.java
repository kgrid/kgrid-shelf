package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
import org.springframework.beans.factory.annotation.Autowired;

public class ZipImportServiceTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ZipImportService service = new ZipImportService();

  @Autowired
  CompoundDigitalObjectStore compoundDigitalObjectStore;


  @Before
  public void setUp() throws Exception {

    String connectionURL = "filesystem:" + temporaryFolder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);


  }

  @Test
  public void testImportKnowledgeObject() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/hello-world.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-world"), 2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(5, filesPaths.size());

    zipStream = ZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-world.zip");
    service.importKO(zipStream, compoundDigitalObjectStore);

  }
  @Test
  public void testImportDifferentDirectoryKnowledgeObject() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/hello world folder.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-folder"), 2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(5, filesPaths.size());


  }
  @Test
  public void testImportMultiDirectoryKnowledgeObject() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/99999-score.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "99999-score"), 3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(5, filesPaths.size());

    zipStream = ZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-world.zip");
    service.importKO(zipStream, compoundDigitalObjectStore);

  }


  @Test
  public void testImportKnowledgeObjectExtraFiles() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/hello-usa-jsonld.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-usa"), 3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(9, filesPaths.size());


  }
  @Test
  public void testBadKOMetaData() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/bad-kometadata.zip");

    try{

      service.importKO(zipStream, compoundDigitalObjectStore);

      fail("should throw exception");

    } catch (ShelfException se){

      assertFalse(Files.exists(
          Paths.get(temporaryFolder.getRoot().toPath().toString(),
              "bad-kometadata")));
    }

  }


  @Test
  public void testfindKOMetadata() {
    Map<String, JsonNode> containerResources = new HashMap<>();

    ObjectNode koMetadata = new ObjectMapper().createObjectNode();
    koMetadata.put("@id", "hello-world");
    koMetadata.put("@type", "koio:KnowledgeObject");

    ObjectNode implMetadata = new ObjectMapper().createObjectNode();
    implMetadata = new ObjectMapper().createObjectNode();
    implMetadata.put("@id", "v1");
    implMetadata.put("@type", "koio:Implementation\"");

    containerResources.put("v1", implMetadata);
    containerResources.put("v1", koMetadata);

    JsonNode metadata = service.findKOMetadata(containerResources);
    assertEquals("hello-world", metadata.get("@id").asText());
    assertEquals("koio:KnowledgeObject", metadata.get("@type").asText());
  }

  @Test
  public void testfindImplMetadata() {
    Map<String, JsonNode> containerResources = new HashMap<>();

    ObjectNode koMetadata = new ObjectMapper().createObjectNode();
    koMetadata.put("@id", "hello-world");
    koMetadata.put("@type", "koio:KnowledgeObject");

    containerResources.put("hello-world", koMetadata);

    ObjectNode implMetadataV1 = new ObjectMapper().createObjectNode();
    implMetadataV1 = new ObjectMapper().createObjectNode();
    implMetadataV1.put("@id", "v1");
    implMetadataV1.put("@type", "koio:Implementation");
    containerResources.put("v1", implMetadataV1);

    ObjectNode implMetadatav2 = new ObjectMapper().createObjectNode();
    implMetadatav2 = new ObjectMapper().createObjectNode();
    implMetadatav2.put("@id", "v2");
    implMetadatav2.put("@type", "koio:Implementation");
    containerResources.put("v2", implMetadatav2);


    List<JsonNode> metadata = service.findImplemtationMetadata(containerResources);
    assertEquals(2, metadata.size());
    assertTrue(metadata.contains( implMetadataV1 ));
    assertTrue(metadata.contains( implMetadatav2 ));
  }

  @Test
  public void testFindBinaries(){

    Map<String, byte[]> binaryResources = new HashMap<>();

    binaryResources.put("hello   world/koio.v1/deployment-specification.yaml","test data".getBytes());
    binaryResources.put("hello   world/koio.v1/service-specification.yaml","service-specification.yaml".getBytes());
    binaryResources.put("hello   world/v2/service-specification.yaml","test data".getBytes());
    binaryResources.put("hello   world/v2/deployment-specification.yaml","v2deployment".getBytes());

    byte[] serviceSpec = service.findBinaries(binaryResources, "koio.v1/service-specification.yaml");
    assertEquals("service-specification.yaml", new String (serviceSpec) );

    byte[] deploymentSpec = service.findBinaries(binaryResources, "v2/deployment-specification.yaml");
    assertEquals("v2deployment", new String (deploymentSpec) );

  }



  @Test
  public void testValidatorStringTypeSuccess() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@id", "hello/world");
    metadata.put("@type", "koio:KnowledgeObject");
    metadata.put("@context", "");

    service.validateMetadata("test.json", metadata);
  }

  @Test
  public void testValidatorArrayTypeSuccess() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    ArrayNode type = new ObjectMapper().createArrayNode();
    type.add("koio:KnowledgeObject");
    type.add("something else");
    metadata.put("@id", "hello/world");
    metadata.set("@type", type);
    metadata.put("@context", "");

    service.validateMetadata("test.json", metadata);
  }

  @Test(expected = ShelfException.class)
  public void testValidatorNumber() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@id", "hello/world");
    metadata.put("@type", 20);
    metadata.put("@context", "");

    service.validateMetadata("test.json", metadata);
  }

  @Test(expected = ShelfException.class)
  public void testValidatorNoData() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    service.validateMetadata("test.json", metadata);
  }

  @Test(expected = ShelfException.class)
  public void testValidatorNoContext() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@id", "hello/world");
    metadata.put("@type", "koio:KnowledgeObject");

    service.validateMetadata("test.json", metadata);
  }

  @Test(expected = ShelfException.class)
  public void testValidatorNoId() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@context", "");
    metadata.put("@type", "koio:KnowledgeObject");

    service.validateMetadata("test.json", metadata);
  }

  @Test(expected = ShelfException.class)
  public void testValidatorNoType() {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@id", "hello/world");
    metadata.put("@context", "");

    service.validateMetadata("test.json", metadata);
  }


}