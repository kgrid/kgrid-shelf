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
import org.zeroturnaround.zip.ZipUtil;

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
  /**
   * Happy Day test, unzips into folder with all the stuff in the zip
   * @throws IOException
   */
  @Test
  public void testImportKnowledgeObject() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/import-export/hello-world-v3.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-world-v3"), 3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(3, filesPaths.size());

    zipStream = ZipImportServiceTest.class.getResourceAsStream("/fixtures/import-export/hello-world-v3.zip");
    service.importKO(zipStream, compoundDigitalObjectStore);

  }


  @Test( expected = ShelfException.class)
  public void testImportDifferentDirectoryKnowledgeObject() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/import-export/bad-package.zip");

    service.importKO(zipStream, compoundDigitalObjectStore);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(), "hello-world-v3"), 3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file -> {
      System.out.println(file.toAbsolutePath().toString());
    });
    assertEquals(3, filesPaths.size());

  }

  @Test
  public void testBadKOMetaData() throws IOException {

    InputStream zipStream = ZipImportServiceTest.class
        .getResourceAsStream("/fixtures/import-export/bad-kometadata.zip");

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





}
