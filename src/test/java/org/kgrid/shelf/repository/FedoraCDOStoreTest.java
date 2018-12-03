package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.mock.web.MockMultipartFile;

@Category(FedoraIntegrationTest.class)
public class FedoraCDOStoreTest {

  FedoraCDOStore fedoraCDOStore = new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  //The Folder will be created before each test method and (recursively) deleted after each test method.
  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  /**
   ** Load hello world into fedora instance.  W
   */
  public void loadFedora() {

    try {

      String filename = "/fixtures/hello-world.zip";
      URL zipStream = FilesystemCDOStoreTest.class.getResource(filename);
      byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
      MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
      ZipImportService zipFileProcessor = new ZipImportService();
      zipFileProcessor
          .importCompoundDigitalObject(new ArkId("hello", "world"), koZip.getInputStream(),
              fedoraCDOStore);

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }

  }


  @Test
  public void findKO() throws Exception {

    ObjectNode koNode = fedoraCDOStore.getMetadata("hello-world");

    assertEquals("Hello World Title", koNode.findValue("title").asText());


  }

  @Test
  public void findBinary() {

    assertNotNull(
        fedoraCDOStore.getBinary("hello-world/koio.v1/welcome.js"));
  }



  @AfterClass
  public static void deleteKO() throws Exception {

    //fedoraCDOStore.removeFile("hello-world");

  }


}