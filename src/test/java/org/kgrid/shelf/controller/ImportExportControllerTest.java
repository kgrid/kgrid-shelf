package org.kgrid.shelf.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@JsonTest
@SpringBootTest(classes = {ImportExportControllerTest.class})
public class ImportExportControllerTest {

  public static final String MANIFEST_WITH_CLASSPATH_RESOURCES = "classpath:/static/manifest-with-classpath-resource.json";
  @Mock
  KnowledgeObjectRepository shelf;

  @Autowired
  WebApplicationContext ctx;

  @Autowired
  ObjectMapper mapper;

  @Test
  public void whenTwoItemManifestImportCalledTwice() throws Exception {
    ImportExportController importExportController = getImportExportController();

    when(shelf.importZip(any(InputStream.class))).thenAnswer( getRandomArkIdAnswer() );

    Map<String, Object> loaded = importExportController
        .loadManifestIfSet(MANIFEST_WITH_CLASSPATH_RESOURCES);

    verify(shelf, times(2)).importZip(any(InputStream.class));
  }

  @Test
  public void emptyLocationNeverTriesToImport() throws Exception {
    ImportExportController importExportController = getImportExportController();

    importExportController.afterPropertiesSet();

    verify(shelf, never()).importZip((InputStream) any());
  }

  @Test public void nullLocationSkipsCallingImport() throws Exception {
    ImportExportController importExportController = getImportExportController(null);

    importExportController.afterPropertiesSet();

    verify(shelf, never()).importZip((InputStream) any());
  }

  @Test
  public void singleShelfErrorIsSkipped() throws Exception {
    ImportExportController importExportController = getImportExportController();

    when(shelf.importZip((InputStream) any()))
        .thenThrow(new RuntimeException())
        .thenAnswer( getRandomArkIdAnswer() )
    ;

    Map<String, Object> loaded = importExportController
        .loadManifestIfSet(MANIFEST_WITH_CLASSPATH_RESOURCES);

    verify(shelf, times(2)).importZip((InputStream) any());

    assertEquals("should skip one and import one:",
        1,
        ((ArrayNode) loaded.get("Added")).size());
  }


  // utility methods
  Answer<ArkId> getRandomArkIdAnswer() {
    return new Answer<ArkId>() {
      @Override
      public ArkId answer(InvocationOnMock invocationOnMock) throws Throwable {
        return new ArkId("a", RandomStringUtils.randomNumeric(2), "version");
      }
    };
  }

  ImportExportController getImportExportController(String startupManifestLocation) {
    ImportExportController importExportController
        = new ImportExportController(shelf, null, startupManifestLocation);
    importExportController.ctx = this.ctx;
    importExportController.mapper = this.mapper;
    return importExportController;
  }

  ImportExportController getImportExportController() {
    return getImportExportController("");
  }
}
