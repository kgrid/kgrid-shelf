package org.kgrid.shelf.repository;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.KOIOKnowledgeObject;
@Category(FedoraIntegrationTest.class)
public class ZipFIleProcessorFedoraStoreTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void saveKnowledgeObjectFedoraCDO() {

    FedoraCDOStore fedoraCDOStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

    KOIOKnowledgeObject ko = processZipFile(fedoraCDOStore);

    assertEquals("Hello  World Title", ko.getKnowledgeObject().get("title").asText());
    assertEquals(2, ko.getImplementations().size());
  }

  protected KOIOKnowledgeObject processZipFile(CompoundDigitalObjectStore cdoStore) {
    String filename = "/fixtures/hello-world-jsonld.zip";
    InputStream zipStream = ZipFIleProcessorFedoraStoreTest.class.getResourceAsStream(filename);

    ZipFileProcessor zipFIleProcessor = new ZipFileProcessor();
    zipFIleProcessor.createCompoundDigitalObject("hello-world",zipStream, cdoStore);

    ObjectNode objectNode = cdoStore.getMetadata("hello-world");
    return new KOIOKnowledgeObject(objectNode);
  }
}