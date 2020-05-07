package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@JsonTest
@SpringBootTest(classes = {ImportExportControllerTest.class})
public class ImportExportControllerTest {

    public static final String MANIFEST_WITH_CLASSPATH_RESOURCES = "classpath:/static/manifest-with-classpath-resource.json";
    public static final String MANIFEST_WITH_FILE_RESOURCES = "classpath:/static/manifest-with-filesystem-resource.json";
    @Mock
    KnowledgeObjectRepository shelf;

    @Autowired
    WebApplicationContext ctx;

    @Autowired
    ObjectMapper mapper;

    @Test
    public void controllerLoadsManifestOnStartIfSetWithSingleManifest() {
        ImportExportController importExportController = getImportExportController(new String[]{MANIFEST_WITH_CLASSPATH_RESOURCES});

        importExportController.afterPropertiesSet();

        verify(shelf, times(2)).importZip(any(InputStream.class));
    }

    @Test
    public void controllerLoadsManifestOnStartIfSetWithManifestArray() {
        String[] manifests = new String[]{MANIFEST_WITH_CLASSPATH_RESOURCES, MANIFEST_WITH_FILE_RESOURCES};
        ImportExportController importExportController = getImportExportController(manifests);

        importExportController.afterPropertiesSet();

        verify(shelf, times(4)).importZip(any(InputStream.class));
    }

    @Test
    public void whenTwoItemManifestImportCalledTwice() throws Exception {
        ImportExportController importExportController = getImportExportController(new String[]{});

        when(shelf.importZip(any(InputStream.class))).thenAnswer(getRandomArkIdAnswer());

        Map<String, Object> loaded = importExportController
                .loadManifestIfSet(MANIFEST_WITH_CLASSPATH_RESOURCES);

        verify(shelf, times(2)).importZip(any(InputStream.class));
    }

    @Test
    public void emptyLocationNeverTriesToImport() throws Exception {
        ImportExportController importExportController = getImportExportController(new String[]{});

        importExportController.afterPropertiesSet();

        verify(shelf, never()).importZip((InputStream) any());
    }

    @Test
    public void nullLocationSkipsCallingImport() throws Exception {
        ImportExportController importExportController = getImportExportController(null);

        importExportController.afterPropertiesSet();

        verify(shelf, never()).importZip((InputStream) any());
    }

    @Test
    public void emptyManifestLocationStringSkipsLoad() {
        ImportExportController importExportController = getImportExportController(new String[]{});

        importExportController.afterPropertiesSet();

        verify(shelf, never()).importZip((InputStream) any());
    }

    @Test
    public void singleShelfErrorIsSkipped() throws Exception {
        ImportExportController importExportController = getImportExportController(new String[]{});

        when(shelf.importZip((InputStream) any()))
                .thenThrow(new RuntimeException())
                .thenAnswer(getRandomArkIdAnswer())
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

    ImportExportController getImportExportController(String[] startupManifestLocation) {
        ImportExportController importExportController
                = new ImportExportController(shelf, null, startupManifestLocation);
        importExportController.ctx = ctx;
        importExportController.mapper = mapper;
        return importExportController;
    }
}
