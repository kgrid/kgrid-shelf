package org.kgrid.shelf.repository;

import static org.junit.Assert.assertFalse;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.mock.web.MockMultipartFile;

public class LoadCPIC {

  static FedoraCDOStore fedoraCDOStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  @Test
  public void loadCPICKOS(){

    try {

      String filename = "/hello-world.zip";
      URL zipStream = FilesystemCDOStoreTest.class.getResource(filename);
      byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
      MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
      ArkId arkId = fedoraCDOStore.addCompoundObjectToShelf(new ArkId( filename.substring(1, filename.indexOf("."))), koZip);

    } catch (Exception exception) {
      assertFalse(exception.getMessage(), true);
    }

  }

}
