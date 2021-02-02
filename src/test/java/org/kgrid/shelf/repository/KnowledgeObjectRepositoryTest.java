package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.kgrid.shelf.TestHelper.*;
import static org.kgrid.shelf.domain.KoFields.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Knowledge Object Repository Tests")
public class KnowledgeObjectRepositoryTest {

    @Mock
    CompoundDigitalObjectStore cdoStore;

    KnowledgeObjectRepository koRepo;

    private final ArkId arkNoVersion = new ArkId(NAAN, NAME);
    private final URI koV1Uri = URI.create(String.format("%s/%s/%s/", NAAN, NAME, VERSION_1));
    private final ObjectNode koV1MetadataNode = (ObjectNode) generateMetadata(koV1Uri.toString(), ARK_ID_V1.getFullArk(), VERSION_1);
    private final URI v1ServiceUri = getFileUri(VERSION_1, SERVICE_YAML_PATH);
    private final URI v1DeploymentUri = getFileUri(VERSION_1, DEPLOYMENT_YAML_PATH);
    private final URI koV2Uri = URI.create(String.format("%s/%s/%s/", NAAN, NAME, VERSION_2));
    private final ObjectNode koV2MetadataNode = (ObjectNode) generateMetadata(koV2Uri.toString(), ARK_ID_V2.getFullArk(), VERSION_2);
    private final URI v2ServiceUri = getFileUri(VERSION_2, SERVICE_YAML_PATH);
    private final URI v2DeploymentUri = getFileUri(VERSION_2, DEPLOYMENT_YAML_PATH);
    private final ArrayList<URI> cdoStoreChildren = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final ArkId missingKoArk = new ArkId("not", "there");

    @BeforeEach
    public void setUp() throws Exception {
        cdoStoreChildren.add(koV1Uri);
        cdoStoreChildren.add(koV2Uri);
        when(cdoStore.getChildren()).thenReturn(cdoStoreChildren);
        when(cdoStore.getMetadata(koV1Uri))
                .thenReturn(koV1MetadataNode);
        lenient().when(cdoStore.getMetadata(koV2Uri))
                .thenReturn(koV2MetadataNode);
        lenient().when(cdoStore.getBinary(v1ServiceUri))
                .thenReturn(SERVICE_BYTES);
        lenient().when(cdoStore.getBinary(v2ServiceUri))
                .thenReturn(SERVICE_BYTES);
        lenient().when(cdoStore.getBinary(v1DeploymentUri))
                .thenReturn(DEPLOYMENT_BYTES);
        lenient().when(cdoStore.getBinary(v2DeploymentUri))
                .thenReturn(DEPLOYMENT_BYTES);
        koRepo = new KnowledgeObjectRepository(cdoStore);
    }

    @Test
    @DisplayName("Delete Calls CdoStore Delete When Ko has only one version")
    public void testDeleteCallsCdoStoreWhenKoHasOnlyOneVersion() {
        cdoStoreChildren.remove(koV2Uri);
        when(cdoStore.getChildren()).thenReturn(cdoStoreChildren);

        koRepo = new KnowledgeObjectRepository(cdoStore);

        koRepo.delete(ARK_ID_V1);

        verify(cdoStore).delete(koV1Uri);
    }

    @Test
    @DisplayName("Delete removes correct version of KO")
    public void testDeleteRemovesCorrectVersion() {
        koRepo.delete(ARK_ID_V1);
        koRepo.getKow(ARK_ID_V2);
    }

    @Test
    @DisplayName("Edit Metadata uses CdoStore to save and get new metadata")
    public void testEditMetadata() throws JsonProcessingException {
        String newMetadata = "{\"metadata\":\"yes, metadata\"}";
        JsonNode newMetadataJson = new ObjectMapper().readTree(newMetadata);
        URI metadataUri = getFileUri(VERSION_1, METADATA_FILENAME.asStr());
        when(cdoStore.getMetadata(metadataUri))
                .thenReturn((ObjectNode) newMetadataJson);

        ObjectNode savedMetadata = koRepo.editMetadata(ARK_ID_V1, newMetadata);

        assertAll(
                () -> verify(cdoStore).saveMetadata(newMetadataJson, metadataUri),
                () -> verify(cdoStore).getMetadata(metadataUri),
                () -> assertEquals(newMetadataJson, savedMetadata)
        );
    }

    @Test
    @DisplayName("Edit Metadata throws if given bad json")
    public void testEditMetadataThrowsIfGivenBadJson() {
        String badJson = "{oof}";
        ShelfException exception = assertThrows(ShelfException.class,
                () -> koRepo.editMetadata(ARK_ID_V1, badJson));
        assertEquals(
                "Cannot parse new metadata",
                exception.getMessage());
    }

    @Test
    @DisplayName("Find All returns all KOs")
    public void testFindAllFindsAll() {
        Map<ArkId, JsonNode> all = koRepo.findAll();
        assertAll(
                () -> assertEquals(2, all.size()),
                () -> assertTrue(all.containsKey(ARK_ID_V1)),
                () -> assertTrue(all.containsKey(ARK_ID_V2))
        );
    }

    @Test
    @DisplayName("Find Deployment Specification returns deployment spec from cdo store")
    public void testFindDeploymentSpec() {
        JsonNode deploymentSpecification = koRepo.findDeploymentSpecification(ARK_ID_V1);
        assertAll(
                () -> verify(cdoStore).getBinary(v1DeploymentUri),
                () -> assertEquals(new YAMLMapper().readTree(DEPLOYMENT_BYTES), deploymentSpecification)
        );
    }

    @Test
    @DisplayName("Find Deployment Specification throws if no spec in metadata")
    public void testFindDeploymentSpecThrowsIfNoSpecInMetadata() throws JsonProcessingException {
        JsonNode metadataWithNoSpec = objectMapper.readTree("{}");

        ShelfException exception = assertThrows(ShelfException.class,
                () -> koRepo.findDeploymentSpecification(ARK_ID_V1, metadataWithNoSpec));
        assertEquals(String.format("Deployment specification not found in metadata for object %s", ARK_ID_V1),
                exception.getMessage());
    }

    @Test
    @DisplayName("find knowledge object metadata calls cdo store and returns result")
    public void testFindKoMetadataCallsCdoStoreAndReturnsMetadata() {
        JsonNode returnedMetadata = koRepo.findKnowledgeObjectMetadata(ARK_ID_V1);
        assertAll(
                () -> verify(cdoStore, times(2)).getMetadata(koV1Uri),
                () -> assertEquals(koV1MetadataNode, returnedMetadata)
        );
    }

    @Test
    @DisplayName("find KO Metadata returns metadata for all versions when no version is given")
    public void testFindKoMetadataWithNoVersion() {
        ArrayList<JsonNode> nodes = new ArrayList<>();
        JsonNode returnedMetadata = koRepo.findKnowledgeObjectMetadata(arkNoVersion);
        nodes.add(returnedMetadata.get(0));
        nodes.add(returnedMetadata.get(1));
        assertAll(
                () -> verify(cdoStore, times(2)).getMetadata(koV1Uri),
                () -> verify(cdoStore, times(2)).getMetadata(koV2Uri),
                () -> assertEquals(returnedMetadata.size(), 2),
                () -> assertTrue(nodes.contains(koV1MetadataNode)),
                () -> assertTrue(nodes.contains(koV2MetadataNode))
        );
    }

    @Test
    @DisplayName("find Ko metadata throws if given null ark id")
    public void testFindKoMetadataThrowsIfGivenNullArkId() {
        ShelfResourceNotFound exception = assertThrows(ShelfResourceNotFound.class,
                () -> koRepo.findKnowledgeObjectMetadata(null));
        assertEquals("Cannot find metadata for null ark id", exception.getMessage());
    }

    @Test
    @DisplayName("find Ko metadata throws if given ark id with no versions map")
    public void testFindKoMetadataThrowsIfGivenArkIdWithNoVersionsInMap() {
        ShelfResourceNotFound exception = assertThrows(ShelfResourceNotFound.class,
                () -> koRepo.findKnowledgeObjectMetadata(missingKoArk));
        assertEquals(String.format("Object location not found for ark id %s",
                missingKoArk.getFullArk()), exception.getMessage());
    }

    @Test
    @DisplayName("find Ko metadata throws if given ark id with version not in version map")
    public void testFindKoMetadataThrowsIfGivenArkIdWithVersionNotInVersionMap() {
        ArkId notInMapArk = new ArkId(NAAN, NAME, "3");
        ShelfResourceNotFound exception = assertThrows(ShelfResourceNotFound.class,
                () -> koRepo.findKnowledgeObjectMetadata(notInMapArk));
        assertEquals(String.format("Object location not found for ark id %s",
                notInMapArk.getFullArk()), exception.getMessage());
    }

    @Test
    @DisplayName("find Service Spec returns service spec from cdo store")
    public void testFindServiceSpecReturnsSpecFromCdoStore() {
        JsonNode serviceSpec = koRepo.findServiceSpecification(ARK_ID_V1, koV1MetadataNode);

        assertAll(
                () -> verify(cdoStore).getBinary(v1ServiceUri),
                () -> assertEquals(yamlMapper.readTree(SERVICE_BYTES), serviceSpec)
        );
    }

    @Test
    @DisplayName("find Service Spec returns service spec from cdo store when given no version")
    public void testFindServiceSpecReturnsSpecFromCdoStoreWhenGivenNoVersion() {
        JsonNode serviceSpec = koRepo.findServiceSpecification(arkNoVersion, koV1MetadataNode);

        assertAll(
                () -> verify(cdoStore).getBinary(any()),
                () -> assertEquals(yamlMapper.readTree(SERVICE_BYTES), serviceSpec)
        );
    }

    @Test
    @DisplayName("find Service Spec throws if cdo store returns bad yaml")
    public void testFindServiceSpecThrowsIfCdoStoreReturnsBadYaml() {
        when(cdoStore.getBinary(v1ServiceUri))
                .thenReturn("  {garbageYaml".getBytes());

        ShelfException exception = assertThrows(ShelfException.class,
                () -> koRepo.findServiceSpecification(ARK_ID_V1, koV1MetadataNode));
        assertEquals(String.format("Could not parse service specification for %s",
                ARK_ID_V1.getFullArk()), exception.getMessage());
    }

    @Test
    @DisplayName("find Service Spec throws if metadata is missing service spec node")
    public void testFindServiceSpecThrowsIfMetadataIsMissingServiceSpecNode() {
        koV1MetadataNode.remove(KoFields.SERVICE_SPEC_TERM.asStr());
        ShelfException exception = assertThrows(ShelfException.class,
                () -> koRepo.findServiceSpecification(ARK_ID_V1, koV1MetadataNode));
        assertEquals(String.format("Metadata for %s is missing a %s field.",
                ARK_ID_V1.getFullArk(), SERVICE_SPEC_TERM.asStr()), exception.getMessage());
    }

    @Test
    @DisplayName("find Service Spec finds metadata when given only an Ark")
    public void testFindServiceSpecFindsMetadataWhenGivenOnlyAnArk() {
        JsonNode serviceSpec = koRepo.findServiceSpecification(ARK_ID_V1);

        assertAll(
                () -> verify(cdoStore, times(2)).getMetadata(koV1Uri),
                () -> verify(cdoStore).getBinary(v1ServiceUri),
                () -> assertEquals(yamlMapper.readTree(SERVICE_BYTES), serviceSpec)
        );
    }

    @Test
    @DisplayName("Get Binary returns binary from cdo store")
    public void testGetBinaryReturnsBinaryFromCdoStore() {
        byte[] binary = koRepo.getBinary(ARK_ID_V1, SERVICE_YAML_PATH);
        assertAll(
                () -> verify(cdoStore).getBinary(v1ServiceUri),
                () -> assertEquals(SERVICE_BYTES, binary)
        );
    }

    @Test
    @DisplayName("Get Binary Stream returns binary Input Stream from cdo store")
    public void testGetBinaryStreamReturnsBinaryInputStreamFromCdoStore() {
        when(cdoStore.getBinaryStream(v1ServiceUri))
                .thenReturn(new ByteArrayInputStream(SERVICE_BYTES));
        InputStream binaryStream = koRepo.getBinaryStream(ARK_ID_V1, SERVICE_YAML_PATH);
        assertAll(
                () -> verify(cdoStore).getBinaryStream(v1ServiceUri),
                () -> assertEquals(Arrays.toString(SERVICE_BYTES),
                        Arrays.toString(binaryStream.readAllBytes()))
        );
    }

    @Test
    @DisplayName("Get binary size returns binary size from cdo store")
    public void testGetBinarySizeReturnsSizeFromCdoStore() {
        long expectedSize = 1234L;
        when(cdoStore.getBinarySize(v1ServiceUri)).thenReturn(expectedSize);

        long returnedSize = koRepo.getBinarySize(ARK_ID_V1, SERVICE_YAML_PATH);

        assertAll(
                () -> verify(cdoStore).getBinarySize(v1ServiceUri),
                () -> assertEquals(expectedSize, returnedSize)
        );
    }

    @Test
    @DisplayName("Get ko repo location returns absolute location from cdo store")
    public void testGetKoRepoLocationReturnsAbsoluteLocationFromCdoStore() {
        URI repoLocation = URI.create("shelfLand");
        when(cdoStore.getAbsoluteLocation(null)).thenReturn(repoLocation);

        URI returnedLocation = koRepo.getKoRepoLocation();

        assertAll(
                () -> verify(cdoStore).getAbsoluteLocation(null),
                () -> assertEquals(repoLocation, returnedLocation)
        );
    }

    @Test
    @DisplayName("Get object location refreshes object map if ko is missing")
    public void testGetObjectLocationRefreshesObjectMapIfKoIsMissing() {
        try {
            koRepo.getObjectLocation(missingKoArk);
        } catch (ShelfResourceNotFound ignored) {
        }
        assertAll(
                () -> verify(cdoStore, times(2)).getMetadata(koV1Uri),
                () -> verify(cdoStore, times(2)).getMetadata(koV2Uri)
        );
    }

    @Test
    @DisplayName("Get object location throws if ko is missing after refresh")
    public void testGetObjectLocationThrowsIfKoIsMissingAfterRefresh() {
        ShelfResourceNotFound exception = assertThrows(ShelfResourceNotFound.class,
                () -> koRepo.getObjectLocation(missingKoArk));
        assertEquals(String.format("Object location not found for ark id %s", missingKoArk.getFullArk()),
                exception.getMessage());
    }

    @Test
    @DisplayName("Refresh Object Map does not throw if metadata is missing identifier")
    public void testRefreshObjectMapDoesNotThrowIfMetadataIsMissingIdentifier() {
        koV1MetadataNode.remove(IDENTIFIER.asStr());
        when(cdoStore.getMetadata(koV1Uri))
                .thenReturn(koV1MetadataNode);
        koRepo = new KnowledgeObjectRepository(cdoStore);
        Map<ArkId, JsonNode> all = koRepo.findAll();
        assertAll(
                () -> assertEquals(1, all.size()),
                () -> assertTrue(all.containsKey(ARK_ID_V2))
        );
    }

    @Test
    @DisplayName("Refresh object map determines ark id if no version specified in metadata")
    public void testRefreshObjectMapDeterminesArkIdIfNoVersionInMetadata() {
        koV1MetadataNode.remove(VERSION.asStr());
        when(cdoStore.getMetadata(koV1Uri))
                .thenReturn(koV1MetadataNode);
        koRepo = new KnowledgeObjectRepository(cdoStore);
        Map<ArkId, JsonNode> all = koRepo.findAll();
        assertAll(
                () -> assertEquals(2, all.size()),
                () -> assertTrue(all.containsKey(ARK_ID_V1)),
                () -> assertTrue(all.containsKey(ARK_ID_V2))
        );
    }

    @Test
    @DisplayName("Refresh object map determines ark id if no version in identifier")
    public void testRefreshObjectMapDeterminesArkIdIfNoVersionInIdentifier() {
        koV1MetadataNode.put(IDENTIFIER.asStr(), String.format("ark:/%s/%s",NAAN,NAME));
        when(cdoStore.getMetadata(koV1Uri))
                .thenReturn(koV1MetadataNode);
        koRepo = new KnowledgeObjectRepository(cdoStore);
        Map<ArkId, JsonNode> all = koRepo.findAll();
        assertAll(
                () -> assertEquals(2, all.size()),
                () -> assertTrue(all.containsKey(ARK_ID_V1)),
                () -> assertTrue(all.containsKey(ARK_ID_V2))
        );
    }

    private URI getFileUri(String version, String file) {
        return URI.create(
                String.format("%s/%s/%s/%s", NAAN, NAME, version, file));
    }
}
