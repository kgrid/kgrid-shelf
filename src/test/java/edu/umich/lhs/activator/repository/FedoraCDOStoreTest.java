package edu.umich.lhs.activator.repository;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartFile;

public class FedoraCDOStoreTest {

  FedoraCDOStore store;

  @Before
  public void setUp() throws Exception {
    store = new FedoraCDOStore("fedoraAdmin", "secret3", new URI("http://localhost:8080/fcrepo/rest"));
  }

  @Test
  public void getAbsolutePathOfLocalServer() throws Exception {
    URI location = store.getAbsoluteLocation(null);
    assertEquals(new URI("http://localhost:8080/fcrepo/rest"), location);
  }


  @Test
  public void addSampleObjectZipToStore() throws Exception {
    String filename = "99999-fk45m6gq9t.zip";
    URL zipStream = FilesystemCDOStoreTest.class.getResource("/fixtures/" + filename);
    byte[] zippedKO = Files.readAllBytes(Paths.get(zipStream.toURI()));
    MockMultipartFile koZip = new MockMultipartFile("ko", filename, "application/zip", zippedKO);
    ObjectNode json = store.addCompoundObjectToShelf(koZip);

  }
}