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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.zeroturnaround.zip.ZipUtil.pack;

@RunWith(MockitoJUnitRunner.class)
public class ZipExportServiceTest {

  @InjectMocks private ZipExportService service;
  @Mock private CompoundDigitalObjectStore cdoStore;

  public static final String SERVICE_YAML_PATH = "service.yaml";
  public static final String DEPLOYMENT_YAML_PATH = "deployment.yaml";
  public static final String PAYLOAD_PATH = "src/index.js";
  private static final String NAAN = "naan";
  private static final String NAME = "name";
  private static final String VERSION = "version";
  private static final String KO_PATH = NAAN + "-" + NAME + "-" + VERSION;
  private static final ArkId ARK_ID = new ArkId(NAAN, NAME, VERSION);
  public static final String SERVICE_SPEC_URL = "http://localhost/" + KO_PATH + "/service.yaml";
  private static final String SEPARATOR = "/";

  private JsonNode happyMetadata;
  private JsonNode serviceUrlMetadata;
  private JsonNode noDeploymentMetadata;
  private JsonNode noServiceSpecMetadata;

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
  private ByteArrayOutputStream expectedOutputStream = new ByteArrayOutputStream();

  @Before
  public void setUp() {

    happyMetadata = generateMetadata(SERVICE_YAML_PATH, DEPLOYMENT_YAML_PATH);
    serviceUrlMetadata = generateMetadata(SERVICE_SPEC_URL, DEPLOYMENT_YAML_PATH);
    noDeploymentMetadata = generateMetadata(SERVICE_SPEC_URL, null);
    noServiceSpecMetadata = generateMetadata(null, DEPLOYMENT_YAML_PATH);

    when(cdoStore.getBinary(Paths.get(KO_PATH, DEPLOYMENT_YAML_PATH).toString()))
        .thenReturn(deploymentSpec);
    when(cdoStore.getBinary(Paths.get(KO_PATH, PAYLOAD_PATH).toString())).thenReturn(payload);
    when(cdoStore.getBinary(Paths.get(KO_PATH, SERVICE_YAML_PATH).toString()))
        .thenReturn(serviceSpec);
    when(cdoStore.getBinary(KO_PATH, DEPLOYMENT_YAML_PATH)).thenReturn(deploymentSpec);
    when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn(serviceSpec);
    when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) happyMetadata);
  }

  @Test
  public void exportObject_happyPathReturnsStream() throws IOException {

    ByteArrayOutputStream outputStream = service.exportObject(ARK_ID, KO_PATH, cdoStore);
    byte[] zippedObject = outputStream.toByteArray();

    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + DEPLOYMENT_YAML_PATH, deploymentSpec));
    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpec));
    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + PAYLOAD_PATH, payload));
    byte[] metadata = JsonUtils.toPrettyString(happyMetadata).getBytes();
    expectedFiles.add(
        new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
    pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), expectedOutputStream);
    byte[] expectedObject = expectedOutputStream.toByteArray();

    assertArrayEquals(expectedObject, zippedObject);
  }

  @Test
  public void exportObject_returnsStreamGivenUrlPaths() throws IOException {

    when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) serviceUrlMetadata);
    ByteArrayOutputStream outputStream = service.exportObject(ARK_ID, KO_PATH, cdoStore);
    byte[] zippedObject = outputStream.toByteArray();

    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + DEPLOYMENT_YAML_PATH, deploymentSpec));
    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpec));
    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + PAYLOAD_PATH, payload));
    byte[] metadata = JsonUtils.toPrettyString(serviceUrlMetadata).getBytes();
    expectedFiles.add(
        new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
    pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), expectedOutputStream);
    byte[] expectedObject = expectedOutputStream.toByteArray();

    assertArrayEquals(expectedObject, zippedObject);
  }

  @Test
  public void exportObject_worksWithNoDeploymentSpec() throws IOException {

    when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noDeploymentMetadata);
    ByteArrayOutputStream outputStream = service.exportObject(ARK_ID, KO_PATH, cdoStore);
    byte[] zippedObject = outputStream.toByteArray();

    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpec));
    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + PAYLOAD_PATH, payload));
    byte[] metadata = JsonUtils.toPrettyString(noDeploymentMetadata).getBytes();
    expectedFiles.add(
        new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
    pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), expectedOutputStream);
    byte[] expectedObject = expectedOutputStream.toByteArray();

    assertArrayEquals(expectedObject, zippedObject);
  }

  @Test
  public void exportObject_worksWithNoServiceSpec() throws IOException {

    when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noServiceSpecMetadata);
    ByteArrayOutputStream outputStream = service.exportObject(ARK_ID, KO_PATH, cdoStore);
    byte[] zippedObject = outputStream.toByteArray();

    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + DEPLOYMENT_YAML_PATH, deploymentSpec));
    byte[] metadata = JsonUtils.toPrettyString(noServiceSpecMetadata).getBytes();
    expectedFiles.add(
        new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
    pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), expectedOutputStream);
    byte[] expectedObject = expectedOutputStream.toByteArray();

    assertArrayEquals(expectedObject, zippedObject);
  }

  @Test
  public void exportObject_failsNoDeploymentAnywhere() throws IOException {
    when(cdoStore.getMetadata(KO_PATH)).thenReturn((ObjectNode) noDeploymentMetadata);
    final byte[] serviceSpecNoXKgrid = "paths:\n  /endpoint:\n    post:\n      data".getBytes();
    when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn(serviceSpecNoXKgrid);
    when(cdoStore.getBinary(Paths.get(KO_PATH, SERVICE_YAML_PATH).toString()))
        .thenReturn(serviceSpecNoXKgrid);
    ByteArrayOutputStream outputStream = service.exportObject(ARK_ID, KO_PATH, cdoStore);
    byte[] zippedObject = outputStream.toByteArray();

    expectedFiles.add(new ByteSource(KO_PATH + SEPARATOR + SERVICE_YAML_PATH, serviceSpecNoXKgrid));
    byte[] metadata = JsonUtils.toPrettyString(noDeploymentMetadata).getBytes();
    expectedFiles.add(
        new ByteSource(KO_PATH + SEPARATOR + KoFields.METADATA_FILENAME.asStr(), metadata));
    pack(expectedFiles.toArray(new ZipEntrySource[expectedFiles.size()]), expectedOutputStream);
    byte[] expectedObject = expectedOutputStream.toByteArray();

    assertArrayEquals(expectedObject, zippedObject);
  }

  @Test
  public void exportObject_failsUnparsableDeployment() throws IOException {

    when(cdoStore.getBinary(KO_PATH, SERVICE_YAML_PATH)).thenReturn("\tbroken".getBytes());
    assertThrows(ShelfException.class, () -> service.exportObject(ARK_ID, KO_PATH, cdoStore));
  }

  private JsonNode generateMetadata(String serviceYamlPath, String deploymentYamlPath) {
    ObjectNode metadata = new ObjectMapper().createObjectNode();
    metadata.put("@id", KO_PATH);
    metadata.put(KoFields.VERSION.asStr(), VERSION);
    if (deploymentYamlPath != null) {
      metadata.put(KoFields.DEPLOYMENT_SPEC_TERM.asStr(), deploymentYamlPath);
    }
    if (serviceYamlPath != null) {
      metadata.put(KoFields.SERVICE_SPEC_TERM.asStr(), serviceYamlPath);
    }
    return metadata;
  }
}
