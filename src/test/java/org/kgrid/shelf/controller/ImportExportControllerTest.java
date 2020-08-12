package org.kgrid.shelf.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.ImportService;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportExportControllerTest {

    private static final String GOOD_MANIFEST_PATH = "GOOD_MANIFEST_PATH";
    private static final String BAD_MANIFEST_PATH = "BAD_MANIFEST_PATH";
    private static final String RESOURCE_1_URI = "RESOURCE_1_URI";
    private static final String RESOURCE_2_URI = "RESOURCE_2_URI";
    private static final String GOOD_NAAN = "naan";
    private static final String GOOD_NAME = "name";
    private static final String GOOD_VERSION = "version";
    private KnowledgeObjectRepository mockKnowledgeObjectRepository;
    private ApplicationContext mockApplicationContext;
    private ObjectMapper mockMapper;
    private ArkId validArkId;
    private InputStream mockResourceInputStream;
    private ServletOutputStream mockServletOutputStream;
    private ImportExportController importExportController;
    private HttpServletResponse servletResponse;
    private MultipartFile mulitPartFile;
    private ImportService mockImportService;

    @Before
    public void setup() throws Exception {
        mockImportService = Mockito.mock(ImportService.class);
        mockKnowledgeObjectRepository = Mockito.mock(KnowledgeObjectRepository.class);
        mockApplicationContext = Mockito.mock(ApplicationContext.class);
        mockMapper = Mockito.mock(ObjectMapper.class);
        Resource mockResource = Mockito.mock(Resource.class);
        mockResourceInputStream = Mockito.mock(InputStream.class);
        mockServletOutputStream = Mockito.mock(ServletOutputStream.class);
        validArkId = new ArkId(GOOD_NAAN, GOOD_NAME, GOOD_VERSION);
        ObjectNode manifestNode = getManifestNode();
        servletResponse = mock(HttpServletResponse.class);
        mulitPartFile = mock(MultipartFile.class);

        when(mockResource.getInputStream()).thenReturn(mockResourceInputStream);
        when(mockApplicationContext.getResource(GOOD_MANIFEST_PATH)).thenReturn(mockResource);
        when(mockApplicationContext.getResource(BAD_MANIFEST_PATH))
                .thenThrow(new NullPointerException());
//        when(mockApplicationContext.getResource(RESOURCE_1_URI)).thenReturn(mockResource);
//        when(mockApplicationContext.getResource(RESOURCE_2_URI)).thenReturn(mockResource);
        when(mockMapper.readTree(mockResourceInputStream)).thenReturn(manifestNode);
//        when(mockKnowledgeObjectRepository.importZip(mockResourceInputStream)).thenReturn(validArkId);
        when(mockKnowledgeObjectRepository.importZip(mulitPartFile)).thenReturn(validArkId);
        when(servletResponse.getOutputStream()).thenReturn(mockServletOutputStream);
    }

    @Test
    public void afterPropertiesSet_LoadsGoodManifestsAndHandlesBadManifests() throws IOException {
        String[] manifests = new String[]{GOOD_MANIFEST_PATH, BAD_MANIFEST_PATH, GOOD_MANIFEST_PATH};
        importExportController = getImportExportControllerForManifestList(manifests);
        importExportController.afterPropertiesSet();

        verify(mockImportService, times(4)).importZip(any(URI.class));
    }

    @Test
    public void afterPropertiesSet_emptyLocationNeverTriesToImport() {
        importExportController = getImportExportControllerForManifestList(null);

        importExportController.afterPropertiesSet();

        verify(mockKnowledgeObjectRepository, never()).importZip((InputStream) any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void afterPropertiesSet_malformedManifestThrowsIllegalArgumentException()
            throws IOException {

        ObjectNode badManifest = JsonNodeFactory.instance.objectNode().put("shmanifest", "bad");
        when(mockMapper.readTree(mockResourceInputStream)).thenReturn(badManifest);

        importExportController =
                getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.afterPropertiesSet();
    }

    @Test
    public void afterPropertiesSet_DoesNothingIfUnreadableManifestResourceIsLoaded()
            throws IOException {
        when(mockMapper.readTree(mockResourceInputStream)).thenThrow(new IOException());

        importExportController =
                getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.afterPropertiesSet();

        verify(mockKnowledgeObjectRepository, never()).importZip((InputStream) any());
    }

    @Test
    public void afterPropertiesSet_singleShelfErrorIsSkipped() throws IOException {
        when(mockImportService.importZip((URI) any()))
                .thenThrow(new RuntimeException())
                .thenReturn(URI.create("test42"));
        importExportController = getImportExportControllerForManifestList(null);

        Map<String, Object> loaded = importExportController.loadManifestIfSet(GOOD_MANIFEST_PATH);

        verify(mockImportService, times(2)).importZip((URI) any());

        assertEquals("should skip one and import one:", 1, ((ArrayNode) loaded.get("Added")).size());
    }

    @Test
    public void exportKnowledgeObjectVersion_SetsContentHeaderOnServletResponse() {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setHeader("Content-Type", "application/octet-stream");
    }

    @Test
    public void exportKnowledgeObjectVersion_AddsContentDispositionHeaderToResponse() {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).addHeader("Content-Disposition",
                "attachment; filename=\"" + GOOD_NAAN + "-" + GOOD_NAME + "-" + GOOD_VERSION + ".zip\"");
    }

    @Test
    public void exportKnowledgeObjectVersion_CallsExtractZipWithOutputStream() throws IOException {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(mockKnowledgeObjectRepository).extractZip(validArkId, mockServletOutputStream);
    }

    @Test
    public void exportKnowledgeObjectVersion_ClosesResponseOutputStream() throws IOException {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(mockServletOutputStream).close();
    }

    @Test
    public void exportKnowledgeObjectVersion_HandlesIOExceptionFromKORepo() throws IOException {
        doThrow(new IOException("OPE")).when(mockKnowledgeObjectRepository).extractZip(any(), any());
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void exportKnowledgeObjectVersion_HandlesIOExceptionFromClosingOStream() throws IOException {
        doThrow(new IOException("OPE")).when(mockServletOutputStream).close();
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObjectVersion(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void exportKnowledgeObject_SetsContentHeaderOnServletResponse() {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setHeader("Content-Type", "application/octet-stream");
    }

    @Test
    public void exportKnowledgeObject_AddsContentDispositionHeaderToResponse() {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).addHeader("Content-Disposition",
                "attachment; filename=\"" + GOOD_NAAN + "-" + GOOD_NAME + "-" + GOOD_VERSION + ".zip\"");
    }

    @Test
    public void exportKnowledgeObject_CallsExtractZipWithOutputStream() throws IOException {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(mockKnowledgeObjectRepository).extractZip(validArkId, mockServletOutputStream);
    }

    @Test
    public void exportKnowledgeObject_ClosesResponseOutputStream() throws IOException {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(mockServletOutputStream).close();
    }

    @Test
    public void exportKnowledgeObject_HandlesIOExceptionFromKORepo() throws IOException {
        doThrow(new IOException("OPE")).when(mockKnowledgeObjectRepository).extractZip(any(), any());
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void exportKnowledgeObject_HandlesIOExceptionFromClosingOStream() throws IOException {
        doThrow(new IOException("OPE")).when(mockServletOutputStream).close();
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, GOOD_VERSION, servletResponse);

        verify(servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void exportKnowledgeObject_AddsContentDispositionHeaderToResponse_WhenArkIsVersionless() {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, null, servletResponse);

        verify(servletResponse).addHeader("Content-Disposition",
                "attachment; filename=\"" + GOOD_NAAN + "-" + GOOD_NAME + ".zip\"");
    }

    @Test
    public void exportKnowledgeObject_CallsExtractZipWithOutputStream_WhenArkIsVersionless() throws IOException {
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});

        importExportController.exportKnowledgeObject(GOOD_NAAN, GOOD_NAME, null, servletResponse);

        verify(mockKnowledgeObjectRepository).extractZip(new ArkId(GOOD_NAAN, GOOD_NAME), mockServletOutputStream);
    }

    @Test
    public void depositKnowledgeObject_CallsImportZipOnKORepo() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String requestUri = "requestUri";
        request.setRequestURI(requestUri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        importExportController = getImportExportControllerForManifestList(new String[]{GOOD_MANIFEST_PATH});
        String s = "http://localhost/" + requestUri + "/" + GOOD_NAAN + "/" + GOOD_NAME;
        HttpHeaders expectedHeaders = new HttpHeaders();
        expectedHeaders.setLocation(URI.create(s));

        ResponseEntity<Map<String, String>> mapResponseEntity = importExportController.depositKnowledgeObject(mulitPartFile);

        assertEquals(expectedHeaders, mapResponseEntity.getHeaders());
    }

    private ObjectNode getManifestNode() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        ArrayNode uris = node.putArray("manifest");
        uris.add(RESOURCE_1_URI);
        uris.add(RESOURCE_2_URI);
        return node;
    }

    private ImportExportController getImportExportControllerForManifestList(String[] manifests) {
        ImportExportController controller =
                new ImportExportController(mockImportService, mockKnowledgeObjectRepository, null, manifests);
        controller.applicationContext = mockApplicationContext;
        controller.mapper = mockMapper;
        return controller;
    }
}
