package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.CompoundDigitalObject;
import org.springframework.mock.web.MockMultipartFile;

@Category(FedoraIntegrationTest.class)
public class FedoraCDOStoreTest {

  static FedoraCDOStore fedoraCDOStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

    //The Folder will be created before each test method and (recursively) deleted after each test method.
  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  /**
  ** Load hello world into fedora instance.  W
   */
  public static void loadFedora() {

    try {

      String filename = "/fixtures/hello-world-jsonld.zip";
      URL zipStream = FilesystemCDOStoreTest.class.getResource(filename);
      byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
      MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
      ZipFileProcessor zipFileProcessor = new ZipFileProcessor();
      zipFileProcessor.importCompoundDigitalObject("hello-world", koZip.getInputStream(),fedoraCDOStore);

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }

  }


  @Test
  public void findKO() throws Exception {

    ObjectNode koNode = fedoraCDOStore.getMetadata("hello-world");

    assertEquals(3,koNode.get("@graph").size());
    assertNotNull(koNode.get("@context"));

  }

  @Test
  public void exportKOZip() throws Exception {

    File helloWorldFile = temporaryFolder.newFile("hello-world-jsonld.zip");
    OutputStream output = new FileOutputStream(helloWorldFile);
    fedoraCDOStore.getCompoundObjectFromShelf("hello-world",false,output);
    output.close();

  }

   @Test
    public void findBinary() {

     assertNotNull(
         fedoraCDOStore.getBinary("hello-world/v0.0.1/welcome.js"));

   }
  @Test
  public void findChildren()  {

    List<String> paths = fedoraCDOStore.getChildren("hello-world/");

    assertEquals(3,
        fedoraCDOStore.getChildren("hello-world/v0.0.1").size());


  }

  @AfterClass
  public static void deleteKO() throws Exception {

    //fedoraCDOStore.removeFile("hello-world");


  }


}