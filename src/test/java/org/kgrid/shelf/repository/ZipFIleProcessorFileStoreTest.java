package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.KOIOKnowledgeObject;

public class ZipFIleProcessorFileStoreTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void saveKnowledgeObjectFileCDO() {

    String connectionURL = "filesystem:" + folder.getRoot().toURI();

    FilesystemCDOStore fileCDOStore =  new FilesystemCDOStore(connectionURL);

    KOIOKnowledgeObject ko = processZipFile(fileCDOStore);

    assertEquals("Hello  World Title", ko.getKnowledgeObject().get("title").asText());
    assertEquals(2, ko.getImplementations().size());
  }

  protected KOIOKnowledgeObject processZipFile(CompoundDigitalObjectStore cdoStore) {
    String filename = "/fixtures/hello-world-jsonld.zip";
    InputStream zipStream = ZipFIleProcessorFileStoreTest.class.getResourceAsStream(filename);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();
    zipFIleProcessor.createCompoundDigitalObject("hello-world",zipStream, cdoStore);

    ObjectNode objectNode = cdoStore.getMetadata("hello-world");
    return new KOIOKnowledgeObject(objectNode);
  }
}