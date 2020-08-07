package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.kgrid.shelf.repository.ZipImportExportTestHelper.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZipExportServiceTest {

    @InjectMocks
    private ZipExportService service;
    @Mock
    private CompoundDigitalObjectStore cdoStore;

    public static final String SERVICE_SPEC_URL = "http://localhost/" + KO_PATH + "/service.yaml";

    private JsonNode happyMetadata;
    private byte[] happyMetadataBytes;
    private JsonNode serviceUrlMetadata;
    private JsonNode noDeploymentMetadata;
    private JsonNode noServiceSpecMetadata;

    private List<ZipEntrySource> expectedFiles = new ArrayList<>();
    private ByteArrayOutputStream expectedOutputStream = new ByteArrayOutputStream();

    @Before
    public void setUp() throws IOException {

        happyMetadata = generateMetadata(SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH, true, true, true, true);
        happyMetadataBytes = JsonUtils.toPrettyString(happyMetadata).getBytes();
        serviceUrlMetadata = generateMetadata(SERVICE_SPEC_URL, DEPLOYMENT_YAML_PATH, true, true, true, true);
        noDeploymentMetadata = generateMetadata(SERVICE_SPEC_URL, null, true, true, true, true);
        noServiceSpecMetadata = generateMetadata(null, DEPLOYMENT_YAML_PATH, true, true, true, true);

        when(cdoStore.getBinary(Paths.get(KO_PATH, DEPLOYMENT_YAML_PATH).toString()))
                .thenReturn(DEPLOYMENT_BYTES);
        when(cdoStore.getBinary(Paths.get(KO_PATH, PAYLOAD_PATH).toString())).thenReturn(PAYLOAD_BYTES);
        when(cdoStore.getBinary(Paths.get(KO_PATH, SERVICE_YAML_PATH).toString()))
                .thenReturn(SERVICE_BYTES);
        when(cdoStore.getBinary(KO_PATH, DEPLOYMENT_YAML_PATH)).thenReturn(DEPLOYMENT_BYTES);
        when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn(SERVICE_BYTES);
        when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) happyMetadata);
    }

    @Test
    public void exportObject_happyPathReturnsStream() {
        byte[] zippedObject = service.exportObject(ARK_ID, KO_PATH, cdoStore).toByteArray();
        expectedOutputStream = packZipForExport(happyMetadataBytes, DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES);
        byte[] expectedObject = expectedOutputStream.toByteArray();

        assertArrayEquals(expectedObject, zippedObject);
    }

    @Test
    public void exportObject_returnsStreamGivenUrlPaths() {
        when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) serviceUrlMetadata);
        byte[] expectedObject = packZipForExport(serviceUrlMetadata.toPrettyString().getBytes(), DEPLOYMENT_BYTES, SERVICE_BYTES, PAYLOAD_BYTES).toByteArray();
        byte[] zippedObject = service.exportObject(ARK_ID, KO_PATH, cdoStore).toByteArray();

        assertArrayEquals(expectedObject, zippedObject);
    }

    @Test
    public void exportObject_worksWithNoDeploymentSpec() {
        when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noDeploymentMetadata);
        byte[] expectedObject = packZipForExport(noDeploymentMetadata.toPrettyString().getBytes(), null, SERVICE_BYTES, PAYLOAD_BYTES).toByteArray();
        byte[] zippedObject = service.exportObject(ARK_ID, KO_PATH, cdoStore).toByteArray();

        assertArrayEquals(expectedObject, zippedObject);
    }

    @Test
    public void exportObject_worksWithNoServiceSpec() {
        when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noServiceSpecMetadata);
        byte[] expectedObject = packZipForExport(noServiceSpecMetadata.toPrettyString().getBytes(), DEPLOYMENT_BYTES, null, null).toByteArray();
        byte[] zippedObject = service.exportObject(ARK_ID, KO_PATH, cdoStore).toByteArray();

        assertArrayEquals(expectedObject, zippedObject);
    }

    @Test
    public void exportObject_failsNoDeploymentAnywhere() {
        when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noDeploymentMetadata);
        final byte[] serviceSpecNoXKgrid = "paths:\n  /endpoint:\n    post:\n      data".getBytes();
        when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn(serviceSpecNoXKgrid);
        when(cdoStore.getBinary(Paths.get(KO_PATH, SERVICE_YAML_PATH).toString()))
                .thenReturn(serviceSpecNoXKgrid);
        byte[] expectedObject = packZipForExport(noDeploymentMetadata.toPrettyString().getBytes(), null, serviceSpecNoXKgrid, null).toByteArray();
        byte[] zippedObject = service.exportObject(ARK_ID, KO_PATH, cdoStore).toByteArray();

        assertArrayEquals(expectedObject, zippedObject);
    }

    @Test
    public void exportObject_failsUnparsableDeployment() {
        when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn("\tbroken".getBytes());
        assertThrows(ShelfException.class, () -> service.exportObject(ARK_ID, KO_PATH, cdoStore));
    }

}
