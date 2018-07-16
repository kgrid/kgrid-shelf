package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.mock.web.MockMultipartFile;

@Category(IntegrationTest.class)
public class FedoraCDOStoreTest {

  FedoraCDOStore store;

  @Before
  public void setUp() throws Exception {
    store = new FedoraCDOStore("http://localhost:8080/fcrepo/rest;username=fedoraAdmin;password=secret3");
    String filename = "99999-fk45m6gq9t.zip";
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/" + filename);
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
    store.addCompoundObjectToShelf(new ArkId("99999-fk45m6gq9t"), koZip);
  }

  @After
  public void deleteKOFromFedora() throws Exception {
    store.removeFile("99999-fk45m6gq9t");
    store.removeFile("99999-fk45m6gq9t/fcr:tombstone");
  }

//  @Test
  public void getAbsolutePathOfLocalServer() throws Exception {
    String location = store.getAbsoluteLocation(null);
    assertEquals("http://localhost:8080/fcrepo/rest", location);
  }

//  @Test
  public void addSampleObjectZipToStore() throws Exception {
    String filename = "99999-fk45m6gq9t.zip";
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/" + filename);
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
    ArkId arkId = store.addCompoundObjectToShelf(new ArkId("99999-fk45m6gq9t"), koZip);
  }

//  @Test
  public void getMetadataFromStore() throws Exception {
    String filename = "99999-fk45m6gq9t";
    assertEquals("{\"@id\":\"ht", store.getMetadata(filename).toString().substring(0, 10));
  }

//  @Test
  public void getBinaryDataFromStore() throws Exception {
    String filename = "99999-fk45m6gq9t/v0.0.1/model/resource/content.js";
    byte[] data = store.getBinary(filename);
    assertEquals("function c", new String(data).substring(0, 10));
  }
}