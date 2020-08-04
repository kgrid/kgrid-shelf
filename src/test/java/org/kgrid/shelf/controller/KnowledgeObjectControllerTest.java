package org.kgrid.shelf.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.NoSuchFileException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KnowledgeObjectControllerTest {

    private static final String NAAN = "naan";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private KnowledgeObjectRepository koRepo;
    private KnowledgeObjectController koController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HashMap<ArkId, JsonNode> koMap = new HashMap<>();
    private final ArkId validArk = new ArkId(NAAN, NAME, VERSION);
    private final ArkId arkNoVersion = new ArkId(NAAN, NAME);
    private MockHttpServletRequest mockServletRequest;
    private final String childpath = "childpath";
    private final String metadataString = "{\"key\":\"a different value\"}";


    @Before
    public void setup() throws JsonProcessingException {
        koRepo = Mockito.mock(KnowledgeObjectRepository.class);
        koController = new KnowledgeObjectController(koRepo, null);
        JsonNode koNode = objectMapper.readTree("{\"key\":\"value\"}");
        koMap.put(validArk, koNode);
        mockServletRequest = new MockHttpServletRequest();
        String requestUri = NAAN + "/" + NAME + "/" + VERSION + "/" + childpath;
        mockServletRequest.setRequestURI(requestUri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockServletRequest));
        when(koRepo.findAll()).thenReturn(koMap);
        when(koRepo.findKnowledgeObjectMetadata(validArk)).thenReturn(koNode);
        when(koRepo.findKnowledgeObjectMetadata(arkNoVersion)).thenReturn(koNode);
        when(koRepo.findServiceSpecification(validArk)).thenReturn(koNode);
        when(koRepo.getBinary(validArk, childpath)).thenReturn("byteArray".getBytes());
    }

    @Test
    public void getAllObjects_CallsFindAllOnKoRepo() {
        koController.getAllObjects();
        verify(koRepo).findAll();
    }

    @Test
    public void getAllObjects_ReturnsCollectionOfKos() {
        Collection kos = koController.getAllObjects();
        assertEquals(kos.size(), 1);
    }

    @Test
    public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
        koController.findKnowledgeObject(NAAN, NAME, VERSION);
        verify(koRepo).findKnowledgeObjectMetadata(validArk);
    }

    @Test
    public void findKnowledgeObject_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
        koController.findKnowledgeObject(NAAN, NAME, null);
        verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
    }

    @Test
    public void findKnowledgeObject_ChecksForFcRepoNaan() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                koController.findKnowledgeObject("fcrepo", "rest", null));
        assertEquals("Cannot connect to fcrepo at the same address as the shelf. " +
                        "Make sure shelf and fcrepo configuration is correct."
                , exception.getMessage());
    }

    @Test
    public void findKnowledgeObject_ThrowsIfObjectNotFound() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                koController.findKnowledgeObject("object", "not", "found"));
        assertEquals("Object not found with id object-not"
                , exception.getMessage());
    }


    @Test
    public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsSupplied() {
        koController.getKnowledgeObjectOldVersion(NAAN, NAME, VERSION);
        verify(koRepo).findKnowledgeObjectMetadata(validArk);
    }

    @Test
    public void findKnowledgeObjectOldVersion_CallsFindMetadataOnKoRepo_WhenVersionIsMissing() {
        koController.getKnowledgeObjectOldVersion(NAAN, NAME, null);
        verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
    }

    @Test
    public void findKnowledgeObjectOldVersion_ChecksForFcRepoNaan() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                koController.getKnowledgeObjectOldVersion("fcrepo", "rest", null));
        assertEquals("Cannot connect to fcrepo at the same address as the shelf. " +
                        "Make sure shelf and fcrepo configuration is correct."
                , exception.getMessage());
    }

    @Test
    public void findKnowledgeObjectOldVersion_ThrowsIfObjectNotFound() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                koController.getKnowledgeObjectOldVersion("object", "not", "found"));
        assertEquals("Object not found with id object-not"
                , exception.getMessage());
    }

    @Test
    public void getServiceDescriptionJson_CallsFindServiceSpecOnKoRepo() {
        koController.getServiceDescriptionJson(NAAN, NAME, VERSION);
        verify(koRepo).findServiceSpecification(validArk);
    }

    @Test
    public void getServiceDescriptionJsonOldVersion_CallsFindServiceSpecOnKoRepo() {
        koController.getServiceDescriptionOldVersionJson(NAAN, NAME, VERSION);
        verify(koRepo).findServiceSpecification(validArk);
    }

    @Test
    public void getServiceDescriptionYaml_CallsFindServiceSpecOnKoRepo() throws JsonProcessingException {
        koController.getServiceDescriptionYaml(NAAN, NAME, VERSION);
        verify(koRepo).findServiceSpecification(validArk);
    }

    @Test
    public void getServiceDescriptionYaml_ReturnsYaml() throws JsonProcessingException {
        Object serviceDescriptionYaml = koController.getServiceDescriptionYaml(NAAN, NAME, VERSION);
        assertEquals("---\nkey: \"value\"\n", serviceDescriptionYaml.toString());
    }

    @Test
    public void getServiceDescriptionYamlOldVersion_CallsFindServiceSpecOnKoRepo() throws JsonProcessingException {
        koController.getOldServiceDescriptionYaml(NAAN, NAME, VERSION);
        verify(koRepo).findServiceSpecification(validArk);
    }

    @Test
    public void getServiceDescriptionYamlOldVersion_ReturnsYaml() throws JsonProcessingException {
        Object serviceDescriptionYaml = koController.getOldServiceDescriptionYaml(NAAN, NAME, VERSION);
        assertEquals("---\nkey: \"value\"\n", serviceDescriptionYaml.toString());
    }

    @Test
    public void getBinary_CallsGetBinaryOnKoRepo() throws NoSuchFileException {
        koController.getBinary(NAAN, NAME, VERSION, mockServletRequest);
        verify(koRepo).getBinary(validArk, childpath);
    }

    @Test
    public void getBinary_ThrowsNoSuchFile_WhenBinaryIsNull() {
        NoSuchFileException exception = assertThrows(NoSuchFileException.class, () ->
                koController.getBinary("not", "gunna", "findit", mockServletRequest));
        assertEquals(exception.getMessage(), "Cannot fetch file at ");
    }

    @Test
    public void editKnowledgeObjectMetadata_CallsEditMetadataOnKoRepo() {
        koController.editKnowledgeObjectMetadata(NAAN, NAME, metadataString);
        verify(koRepo).editMetadata(arkNoVersion, metadataString);
    }

    @Test
    public void editKnowledgeObjectMetadata_GetsNewMetadataFromKoRepo() {
        koController.editKnowledgeObjectMetadata(NAAN, NAME, metadataString);
        verify(koRepo).findKnowledgeObjectMetadata(arkNoVersion);
    }

    @Test
    public void editKnowledgeObjectMetadata_ReturnsNewMetadata() throws JsonProcessingException {
        when(koRepo.findKnowledgeObjectMetadata(arkNoVersion))
                .thenReturn(objectMapper.readTree(metadataString));
        ResponseEntity<JsonNode> newMetaData = koController.editKnowledgeObjectMetadata(NAAN, NAME, metadataString);
        assertEquals(metadataString, Objects.requireNonNull(newMetaData.getBody()).toString());
    }

    @Test
    public void editVersionMetadata_CallsEditMetadataOnKoRepo() {
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
        verify(koRepo).editMetadata(validArk, metadataString);
    }

    @Test
    public void editVersionMetadata_GetsNewMetadataFromKoRepo() {
        koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);
        verify(koRepo).findKnowledgeObjectMetadata(validArk);
    }

    @Test
    public void editVersionMetadata_ReturnsNewMetadata() throws JsonProcessingException {
        when(koRepo.findKnowledgeObjectMetadata(validArk))
                .thenReturn(objectMapper.readTree(metadataString));
        ResponseEntity<JsonNode> newMetaData = koController.editVersionMetadata(NAAN, NAME, VERSION, metadataString);

        assertEquals(metadataString, Objects.requireNonNull(newMetaData.getBody()).toString());
    }

    @Test
    public void deleteKnowledgeObject_callsDeleteOnKoRepo() {
        koController.deleteKnowledgeObject(NAAN, NAME);
        verify(koRepo).delete(arkNoVersion);
    }

    @Test
    public void deleteKnowledgeObject_ReturnsNoContent() {
        ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void deleteKnowledgeObject_callsDeleteOnKoRepo_WithVersion() {
        koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
        verify(koRepo).delete(validArk);
    }

    @Test
    public void deleteKnowledgeObject_ReturnsNoContent_WithVersion() {
        ResponseEntity<String> response = koController.deleteKnowledgeObject(NAAN, NAME, VERSION);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}