package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.HttpClientErrorException;

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

      String filename = "/hello-world.zip";
      URL zipStream = FilesystemCDOStoreTest.class.getResource(filename);
      byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
      MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
      ArkId arkId = fedoraCDOStore.addCompoundObjectToShelf(new ArkId("hello-world"), koZip);

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }

  }


  @Test
  public void findKO() throws Exception {

    assertEquals("Hello, World",fedoraCDOStore.getMetadata(
        "hello-world/v0.0.1").get("title").asText());

  }

  @Test
  public void exportKOZip() throws Exception {

    File helloWorldFile = temporaryFolder.newFile("hello-world.zip");
    OutputStream output = new FileOutputStream(helloWorldFile);
    fedoraCDOStore.getCompoundObjectFromShelf("hello-world",false,output);
    output.close();

  }

   @Test
    public void findBinary() {

     assertNotNull(
         fedoraCDOStore.getBinary("hello-world/v0.0.1/model/resource/welcome.js"));

   }
  @Test
  public void findChildren()  {

    assertEquals(2,
        fedoraCDOStore.getChildren("hello-world/v0.0.1/model").size());
  }

  @AfterClass
  public static void deleteKO() throws Exception {

    fedoraCDOStore.removeFile("hello-world");

    File helloWorldFile = temporaryFolder.newFile("delete.zip");
    OutputStream output = new FileOutputStream(helloWorldFile);
    try {
      fedoraCDOStore.getCompoundObjectFromShelf("hello-world", false, output);
      assertFalse("Should throw 410 exception ", true);
    } catch (HttpClientErrorException e) {
      assertTrue("Should throw 410 exception ", true);
    } finally {
      output.close();
    }
  }


}