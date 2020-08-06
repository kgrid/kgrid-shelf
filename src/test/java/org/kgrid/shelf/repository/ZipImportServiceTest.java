package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zeroturnaround.zip.ZipUtil.pack;

@RunWith(MockitoJUnitRunner.class)
public class ZipImportServiceTest {


    @InjectMocks
    ZipImportService zipImportService;

    @Mock
    CompoundDigitalObjectStore compoundDigitalObjectStore;

    private static final String TRANSACTION_ID = "Wuhuhuh";
    public static final String SERVICE_YAML_PATH = "service.yaml";
    public static final String DEPLOYMENT_YAML_PATH = "deployment.yaml";
    public static final String PAYLOAD_PATH = "src/index.js";
    private static final String NAAN = "naan";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String KO_PATH = NAAN + "-" + NAME + "-" + VERSION;
    private static final ArkId ARK_ID = new ArkId(NAAN, NAME, VERSION);
    private static final String SEPARATOR = "/";

    private JsonNode happyMetadata;

    final byte[] deploymentSpec =
            ("endpoints:\n  /welcome:\n    artifact: " + PAYLOAD_PATH + "\n    function: welcome\n")
                    .getBytes();
    final byte[] serviceSpec =
            ("paths:\n  /welcome:\n    post:\n      x-kgrid-activation:\n        artifact:\n            - "
                    + PAYLOAD_PATH
                    + "\n")
                    .getBytes();
    final byte[] payload = "function(input){return \"hi\";}".getBytes();

    private List<ZipEntrySource> expectedFiles = new ArrayList<>();
    private byte[] happyMetadataBytes;
    private String dashArkWithVersion = ARK_ID.getDashArk() + "-" + VERSION;


    @Before
    public void setUp() throws IOException {
        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();
        when(compoundDigitalObjectStore.createTransaction()).thenReturn(TRANSACTION_ID);
    }


    @Test
    public void importKo_UsesCreateTransactionOnCdoStore() throws IOException {
        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).createTransaction();
    }

    @Test
    public void importKo_UsesCreateContainerOnCdoStore() throws IOException {
        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).createContainer(TRANSACTION_ID, dashArkWithVersion);
    }

    @Test
    public void importKo_CallsSaveBinaryForEachFile() throws IOException {
        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        serviceSpec,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        SERVICE_YAML_PATH);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        deploymentSpec,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        DEPLOYMENT_YAML_PATH);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        payload,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        PAYLOAD_PATH);
    }

    @Test
    public void importKo_CallsSaveMetadata() throws IOException {
        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore)
                .saveMetadata(happyMetadata,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        KoFields.METADATA_FILENAME.asStr());
    }

    @Test
    public void importKo_CallsCommitTransaction() throws IOException {
        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).commitTransaction(TRANSACTION_ID);
    }

    @Test
    public void importKo_ThrowsExceptionWithNoMetadata() throws IOException {
        ByteArrayInputStream inStream = packZip(null, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoIdentifierInMetadata() throws IOException {
        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, false, true, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();

        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Can't import identifier and/or version are not found in the metadata",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoAtIdInMetadata() throws IOException {
        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, false, true, false, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();

        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoVersionInMetadata() throws IOException {
        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, false, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();

        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Can't import identifier and/or version are not found in the metadata",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoTypeInMetadata() throws IOException {
        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, false);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();

        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }


    @Test
    public void importKo_ThrowsExceptionWhenSavingGoesWrong() throws IOException {
        doThrow(new ShelfException("OPE")).when(compoundDigitalObjectStore).saveMetadata(any(), any(), any(), any());

        happyMetadata = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();

        ByteArrayInputStream inStream = packZip(happyMetadataBytes, deploymentSpec, serviceSpec, payload);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Could not import " + ARK_ID.toString(),
                shelfException.getMessage());
    }


    private ByteArrayInputStream packZip(byte[] metadata, byte[] deploymentSpec, byte[] serviceSpec, byte[] payload) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if (metadata != null && metadata.length != 0) {
            metadata = JsonUtils.toPrettyString(happyMetadata).getBytes();
            expectedFiles.add(
                    new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
        }
        if (deploymentSpec != null && deploymentSpec.length != 0) {
            expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + DEPLOYMENT_YAML_PATH, deploymentSpec));
        }
        if (serviceSpec != null && serviceSpec.length != 0) {
            expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpec));
        }
        if (payload != null && payload.length != 0) {
            expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + PAYLOAD_PATH, payload));
        }
        pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private JsonNode generateMetadata(String serviceYamlPath, String deploymentYamlPath, boolean hasAtId, boolean hasIdentifier, boolean hasVersion, boolean hasType) {
        ObjectNode metadata = new ObjectMapper().createObjectNode();
        if (hasAtId) {
            metadata.put("@id", KO_PATH);
        }
        if (hasType) {
            metadata.put("@type", "koio:KnowledgeObject");
        }
        if (hasIdentifier) {
            metadata.put("identifier", ARK_ID.toString());
        }
        if (hasVersion) {
            metadata.put(KoFields.VERSION.asStr(), VERSION);
        }
        if (deploymentYamlPath != null) {
            metadata.put(KoFields.DEPLOYMENT_SPEC_TERM.asStr(), deploymentYamlPath);
        }
        if (serviceYamlPath != null) {
            metadata.put(KoFields.SERVICE_SPEC_TERM.asStr(), serviceYamlPath);
        }
        return metadata;
    }
}
