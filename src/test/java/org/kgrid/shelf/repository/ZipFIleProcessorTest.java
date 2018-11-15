package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.KOIOKnowledgeObject;

public class ZipFIleProcessorTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void importCDOFileStoreTest() {

    String connectionURL = "filesystem:" + folder.getRoot().toURI();

    FilesystemCDOStore fileCDOStore =  new FilesystemCDOStore(connectionURL);

    KOIOKnowledgeObject ko = importTheCDO(fileCDOStore,"hello-world","/fixtures/hello-world-jsonld.zip");
     ko = importTheCDO(fileCDOStore,"hello-world","/fixtures/hello-world2.zip");

    assertEquals("Hello  World Title", ko.getKnowledgeObject().get("dc:title").asText());
  }

  @Test
  public void exportCDOFileStoreTest() throws IOException {
    String connectionURL = "filesystem:" + folder.getRoot().toURI();

    FilesystemCDOStore fileCDOStore =  new FilesystemCDOStore(connectionURL);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();

    KOIOKnowledgeObject ko = importTheCDO(fileCDOStore,"hello-world","/fixtures/hello-world-jsonld.zip");

    ByteArrayOutputStream zipOutputStream = (ByteArrayOutputStream) zipFIleProcessor.exportCompoundDigitalObject("hello-world",fileCDOStore);

    writeZip(zipOutputStream);
  }


  @Test
  public void importCDOFcrepoTest() {

    FedoraCDOStore fedoraCDOStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

    KOIOKnowledgeObject ko = importTheCDO(fedoraCDOStore,"hello-world","/fixtures/hello-world-jsonld.zip");

    assertEquals("Hello  World Title", ko.getKnowledgeObject().get("title").asText());
    assertEquals(2, ko.getImplementations().size());


  }

  @Test
  public void exportCDOFcrepoTest() throws IOException {

    FedoraCDOStore fedoraCDOStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();

    KOIOKnowledgeObject ko = importTheCDO(fedoraCDOStore,"hello-world","/fixtures/hello-world-jsonld.zip");

    ByteArrayOutputStream zipOutputStream = (ByteArrayOutputStream) zipFIleProcessor.exportCompoundDigitalObject("hello-world",fedoraCDOStore);

    writeZip(zipOutputStream);
  }


  protected void writeZip(ByteArrayOutputStream zipOutputStream) {
    try {
         Path path = Paths.get(folder.getRoot().getAbsolutePath(), "export.zip");
      OutputStream outputStream = new FileOutputStream(path.toString());
      try {
        zipOutputStream.writeTo(outputStream);
      } finally {
        outputStream.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected KOIOKnowledgeObject importTheCDO(CompoundDigitalObjectStore cdoStore, String cdoIdentifier, String zipFile ) {

    InputStream zipStream = ZipFIleProcessorTest.class.getResourceAsStream(zipFile);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();
    zipFIleProcessor.importCompoundDigitalObject(cdoIdentifier,zipStream, cdoStore);

    ObjectNode objectNode = cdoStore.getMetadata(cdoIdentifier);

    return new KOIOKnowledgeObject(objectNode);
  }
}