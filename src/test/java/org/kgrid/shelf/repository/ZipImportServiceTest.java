package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jsonldjava.utils.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kgrid.shelf.repository.ZipImportExportTestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZipImportServiceTest {

    private static String TRANSACTION_ID = "transacting out of line";
    @InjectMocks
    ZipImportService zipImportService;

    @Mock
    CompoundDigitalObjectStore compoundDigitalObjectStore;

    private JsonNode metadataNode;
    private byte[] metadataBytes;
    private final String dashArkWithVersion = ARK_ID.getDashArk() + "-" + VERSION;

    @Before
    public void setUp() throws IOException {
        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();
        when(compoundDigitalObjectStore.createTransaction()).thenReturn(TRANSACTION_ID);
    }

    @Test
    public void importKo_UsesCreateTransactionOnCdoStore() throws IOException {
        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).createTransaction();
    }

    @Test
    public void importKo_UsesCreateContainerOnCdoStore() throws IOException {
        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).createContainer(TRANSACTION_ID, dashArkWithVersion);
    }

    @Test
    public void importKo_CallsSaveBinaryForEachFile() throws IOException {
        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        SERVICE_BYTES,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        SERVICE_YAML_PATH);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        DEPLOYMENT_BYTES,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        DEPLOYMENT_YAML_PATH);
        verify(compoundDigitalObjectStore)
                .saveBinary(
                        PAYLOAD_BYTES,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        PAYLOAD_PATH);
    }

    @Test
    public void importKo_CallsSaveMetadata() throws IOException {
        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore)
                .saveMetadata(metadataNode,
                        TRANSACTION_ID,
                        dashArkWithVersion,
                        KoFields.METADATA_FILENAME.asStr());
    }

    @Test
    public void importKo_CallsCommitTransaction() throws IOException {
        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        zipImportService.importKO(inStream, compoundDigitalObjectStore);
        verify(compoundDigitalObjectStore).commitTransaction(TRANSACTION_ID);
    }

    @Test
    public void importKo_ThrowsExceptionWithNoMetadata() throws IOException {
        ByteArrayInputStream inStream = packZip(null, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoIdentifierInMetadata() throws IOException {
        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, false, true, true);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();

        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Can't import identifier and/or version are not found in the metadata",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoAtIdInMetadata() throws IOException {
        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, false, true, false, true);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();

        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoVersionInMetadata() throws IOException {
        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, false, true);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();

        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Can't import identifier and/or version are not found in the metadata",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWithNoTypeInMetadata() throws IOException {
        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, false);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();

        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("The imported zip is not a valid knowledge object, no valid metadata found",
                shelfException.getMessage());
    }

    @Test
    public void importKo_ThrowsExceptionWhenSavingGoesWrong() throws IOException {
        doThrow(new ShelfException("OPE")).when(compoundDigitalObjectStore).saveMetadata(any(), any(), any(), any());

        metadataNode = generateMetadata(
                SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
        metadataBytes = JsonUtils.toPrettyString(metadataNode).getBytes();

        ByteArrayInputStream inStream = packZip(metadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        ShelfException shelfException = assertThrows(ShelfException.class,
                () -> zipImportService.importKO(inStream, compoundDigitalObjectStore));
        assertEquals("Could not import " + ARK_ID.toString(),
                shelfException.getMessage());
    }
}
