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
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.KOIOKnowledgeObject;

public class ZipFIleProcessorFileStoreTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void importCDO() {

    String connectionURL = "filesystem:" + folder.getRoot().toURI();

    FilesystemCDOStore fileCDOStore =  new FilesystemCDOStore(connectionURL);

    KOIOKnowledgeObject ko = imporTheCDO(fileCDOStore);

    assertEquals("Hello  World Title", ko.getKnowledgeObject().get("dc:title").asText());
  }

  @Test
  public void exportCDO(){
    String connectionURL = "filesystem:" + folder.getRoot().toURI();

    FilesystemCDOStore fileCDOStore =  new FilesystemCDOStore(connectionURL);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();

    KOIOKnowledgeObject ko = imporTheCDO(fileCDOStore);

    try {
      ByteArrayOutputStream zipOutputStream = (ByteArrayOutputStream) zipFIleProcessor.exportCompoundDigitalObject("hello-world",fileCDOStore);
      Path path = Paths.get(folder.getRoot().getAbsolutePath(), "hello-world.zip");
      try(OutputStream outputStream = new FileOutputStream(path.toString())) {
        zipOutputStream.writeTo(outputStream);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected KOIOKnowledgeObject imporTheCDO(CompoundDigitalObjectStore cdoStore) {

    String filename = "/fixtures/hello-world-jsonld.zip";
    InputStream zipStream = ZipFIleProcessorFileStoreTest.class.getResourceAsStream(filename);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();
    zipFIleProcessor.importCompoundDigitalObject("hello-world",zipStream, cdoStore);

    ObjectNode objectNode = cdoStore.getMetadata("hello-world");

    return new KOIOKnowledgeObject(objectNode);
  }
}