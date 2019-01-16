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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
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

    service.importObject(new ArkId("hello", "world"), zipStream, compoundDigitalObjectStore);

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
    service.importObject(new ArkId("hello", "world"), zipStream, compoundDigitalObjectStore);

  }


  @Test
  public void testImportKnowledgeObjectExtraFiles() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/hello-usa-jsonld.zip");

    service.importObject(new ArkId("hello", "usa"), zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-usa"), 2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(9, filesPaths.size());


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