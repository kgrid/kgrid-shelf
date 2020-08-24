package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KoFields;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KnowledgeObjectRepositoryTest {

  KnowledgeObjectRepository repository;

  @Mock CompoundDigitalObjectStore compoundDigitalObjectStore;

  @Mock ZipExportService zipExportService;

  private ArkId helloWorld1ArkId = new ArkId("hello", "world", "v0.1.0");
  private ArkId helloWorld2ArkId = new ArkId("hello", "world", "v0.2.0");
  private ArkId noSpecArkId = new ArkId("bad", "bad", "bad");
  private String helloWorld1Location =
      helloWorld1ArkId.getDashArk() + "-" + helloWorld1ArkId.getVersion();
  private String helloWorld2Location =
      helloWorld2ArkId.getDashArk() + "-" + helloWorld2ArkId.getVersion();
  private JsonNode helloWorld1Metadata;
  private JsonNode helloWorld2Metadata;
  private List<String> koLocations;
  private JsonNode noSpecMetadata;
  private String badLocation = noSpecArkId.getDashArk() + "-" + noSpecArkId.getVersion();

  @Before
  public void setUp() throws Exception {

    koLocations = Arrays.asList(helloWorld1Location, helloWorld2Location, badLocation);
    helloWorld1Metadata =
        new ObjectMapper()
            .readTree(
                "{\n"
                    + "  \"@id\" : \"hello-world\",\n"
                    + "  \"@type\" : \"koio:KnowledgeObject\",\n"
                    + "  \"identifier\" : \"ark:/hello/world\",\n"
                    + "  \"version\":\"v0.1.0\",\n"
                    + "  \"title\" : \"Hello World Title\",\n"
                    + "  \"contributors\" : \"Kgrid Team\",\n"
                    + "  \"keywords\":[\"Hello\",\"example\"],\n"
                    + "  \"hasServiceSpecification\": \"service.yaml\",\n"
                    + "  \"hasDeploymentSpecification\": \"deployment.yaml\",\n"
                    + "  \"hasPayload\": \"src/index.js\",\n"
                    + "  \"@context\" : [\"http://kgrid.org/koio/contexts/knowledgeobject.jsonld\" ]\n"
                    + "}");
    helloWorld2Metadata =
        new ObjectMapper()
            .readTree(
                "{\n"
                    + "  \"@id\" : \"hello-world\",\n"
                    + "  \"@type\" : \"koio:KnowledgeObject\",\n"
                    + "  \"identifier\" : \"ark:/hello/world\",\n"
                    + "  \"version\":\"v0.2.0\",\n"
                    + "  \"title\" : \"Hello World Title\",\n"
                    + "  \"contributors\" : \"Kgrid Team\",\n"
                    + "  \"keywords\":[\"Hello\",\"example\"],\n"
                    + "  \"hasServiceSpecification\": \"service2.yaml\",\n"
                    + "  \"hasDeploymentSpecification\": \"deployment.yaml\",\n"
                    + "  \"hasPayload\": \"src/index.js\",\n"
                    + "  \"@context\" : [\"http://kgrid.org/koio/contexts/knowledgeobject.jsonld\" ]\n"
                    + "}");

    noSpecMetadata =
        new ObjectMapper().readTree("{  \"identifier\" : \"ark:/bad/bad\",\n\"version\":\"bad\"}");
    when(compoundDigitalObjectStore.getChildren("")).thenReturn(koLocations);
    when(compoundDigitalObjectStore.getMetadata(helloWorld1Location))
        .thenReturn((ObjectNode) helloWorld1Metadata);
    when(compoundDigitalObjectStore.getMetadata(helloWorld2Location))
        .thenReturn((ObjectNode) helloWorld2Metadata);
    when(compoundDigitalObjectStore.getMetadata(badLocation))
        .thenReturn((ObjectNode) noSpecMetadata);
    repository = new KnowledgeObjectRepository(compoundDigitalObjectStore, zipExportService);
  }

  @Test
  public void inspectsMetadataDuringLoading() {
    verify(compoundDigitalObjectStore, times(1)).getMetadata(helloWorld1Location);
  }

  @Test
  public void getCorrectMetadata() {
    repository.findKnowledgeObjectMetadata(helloWorld1ArkId);
    verify(compoundDigitalObjectStore, times(2)).getMetadata(helloWorld1Location);
  }

  @Test
  public void deleteVersion() {
    repository.delete(helloWorld1ArkId);
    verify(compoundDigitalObjectStore).delete(helloWorld1Location);
  }

  @Test
  public void editMetadataResolvesToCorrectLocation() throws JsonProcessingException {
    String newMetadataStr = "{\"@id\" : \"goodbye-world\"}";
    JsonNode metadata = new ObjectMapper().readTree(newMetadataStr);
    repository.editMetadata(helloWorld1ArkId, newMetadataStr);
    verify(compoundDigitalObjectStore)
        .saveMetadata(
            metadata,
            helloWorld1Location
                + FileSystems.getDefault().getSeparator()
                + KoFields.METADATA_FILENAME.asStr());
  }

  @Test
  public void editMetadataReturnsSavedData() {
    String newMetadataStr = "{\"@id\" : \"goodbye-world\"}";
    repository.editMetadata(helloWorld1ArkId, newMetadataStr);
    verify(compoundDigitalObjectStore, times(1))
        .getMetadata(
            helloWorld1Location
                + FileSystems.getDefault().getSeparator()
                + KoFields.METADATA_FILENAME.asStr());
  }

  @Test(expected = ShelfException.class)
  public void editMetadataThrowsCorrectError() {
    String badMetadata = "{\"@id\" : \"goodbye-world}";
    repository.editMetadata(helloWorld1ArkId, badMetadata);
  }

  @Test
  public void extractZip_getsCorrectZip() throws IOException {
    OutputStream oStream = Mockito.mock(OutputStream.class);
    when(zipExportService.exportObject(
            helloWorld1ArkId, helloWorld1Location, compoundDigitalObjectStore))
        .thenReturn(new ByteArrayOutputStream());
    repository.extractZip(helloWorld1ArkId, oStream);
    verify(zipExportService)
        .exportObject(helloWorld1ArkId, helloWorld1Location, compoundDigitalObjectStore);
  }

  @Test
  public void extractZip_writesToOutputStream() throws IOException {
    OutputStream oStream = Mockito.mock(OutputStream.class);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    when(zipExportService.exportObject(
            helloWorld1ArkId, helloWorld1Location, compoundDigitalObjectStore))
        .thenReturn(byteArrayOutputStream);
    repository.extractZip(helloWorld1ArkId, oStream);
    verify(oStream).write(byteArrayOutputStream.toByteArray());
  }

  @Test
  public void findAll_refreshesMap() {
    repository.findAll();
    verify(compoundDigitalObjectStore, times(2)).getMetadata(helloWorld1Location);
    verify(compoundDigitalObjectStore, times(2)).getChildren("");
  }

  @Test
  public void findAll_returnsObjectMap() {
    Map<ArkId, JsonNode> map = new HashMap<>();
    map.put(helloWorld1ArkId, helloWorld1Metadata);
    map.put(helloWorld2ArkId, helloWorld2Metadata);
    map.put(noSpecArkId, noSpecMetadata);
    assertEquals(repository.findAll(), map);
  }

  @Test
  public void findDeploymentSpec_fetchesDeployment() {
    String deployment = "{\"This is a deployment spec\": \"yay\"}";

    when(compoundDigitalObjectStore.getBinary(
            helloWorld1Location + FileSystems.getDefault().getSeparator() + "deployment.yaml"))
        .thenReturn(deployment.getBytes());
    JsonNode deploymentSpec = repository.findDeploymentSpecification(helloWorld1ArkId);
    assertEquals("yay", deploymentSpec.get("This is a deployment spec").asText());
  }

  @Test(expected = ShelfException.class)
  public void findDeploymentSpec_throwsErrorWithNoSpec() {
    repository.findDeploymentSpecification(noSpecArkId);
  }

  @Test
  public void findKOMetadata_getsMetadataWithGoodArk() {
    assertEquals(helloWorld1Metadata, repository.findKnowledgeObjectMetadata(helloWorld1ArkId));
  }

  @Test
  public void findKOMetadata_getsAllVersionsWithUnversionedArk() {
    ArrayNode array = new ObjectMapper().createArrayNode();
    array.add(helloWorld2Metadata);
    array.add(helloWorld1Metadata);
    assertEquals(array, repository.findKnowledgeObjectMetadata(new ArkId("hello", "world")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void findKOMetadata_nullArk() {
    repository.findKnowledgeObjectMetadata(null);
  }

  @Test(expected = ShelfException.class)
  public void findKOMetadata_unknownArk() {
    ArkId notInMap = new ArkId("hello", "whirled", "wow");
    repository.findKnowledgeObjectMetadata(notInMap);
  }

  @Test(expected = ShelfException.class)
  public void findKOMetadata_unknownVersion() {
    ArkId notInMap = new ArkId("hello", "world", "wow");
    repository.findKnowledgeObjectMetadata(notInMap);
  }

  @Test
  public void findServiceSpec_getsCorrectSpec() {
    String deployment = "{\"This is a service spec\": \"yay\"}";
    when(compoundDigitalObjectStore.getBinary(
            helloWorld1Location + FileSystems.getDefault().getSeparator() + "service.yaml"))
        .thenReturn(deployment.getBytes());
    JsonNode serviceSpec = repository.findServiceSpecification(helloWorld1ArkId);
    assertEquals("yay", serviceSpec.get("This is a service spec").asText());
  }

  @Test(expected = ShelfException.class)
  public void findServiceSpec_badArkIdThrowsError() {
    repository.findServiceSpecification(noSpecArkId);
  }

  @Test
  public void findServiceSpec_noSpecifiedVersion() {
    ArkId versionless = new ArkId("hello", "world");
    String service = "{\"This is a service spec\": \"yay\"}";
    when(compoundDigitalObjectStore.getBinary(
            helloWorld2Location + FileSystems.getDefault().getSeparator() + "service2.yaml"))
        .thenReturn(service.getBytes());
    JsonNode serviceSpec = repository.findServiceSpecification(versionless);
    assertEquals("yay", serviceSpec.get("This is a service spec").asText());
  }

  @Test
  public void getBinary_returnsBinary() {
    byte[] binaryData = "I'm a binary!".getBytes();
    String childPath = "src/index.js";
    when(compoundDigitalObjectStore.getBinary(helloWorld1Location, childPath))
        .thenReturn(binaryData);
    byte[] binaryResult = repository.getBinary(helloWorld1ArkId, childPath);
    assertArrayEquals(binaryData, binaryResult);
  }

  @Test(expected = ShelfException.class)
  public void getBinary_missingArkThrowsException() {
    ArkId missingArk = new ArkId("missing", "help");
    String childPath = "src/index.js";
    repository.getBinary(missingArk, childPath);
  }

  @Test
  public void getKoRepoLocation_returnsDataStoreLocation() {
    when(compoundDigitalObjectStore.getAbsoluteLocation("")).thenReturn("good");
    assertEquals("good", repository.getKoRepoLocation());
  }

  @Test
  public void getObjectLocation_returnsLocation() {
    assertEquals(helloWorld1Location, repository.getObjectLocation(helloWorld1ArkId));
  }

  @Test
  public void getObjectLocation_missingObjectIsNull() throws JsonProcessingException {
    ArkId hellov4 = new ArkId("hello", "world", "v0.4.0");
    String hellov4Location = hellov4.getDashArk() + "-" + hellov4.getVersion();
    List<String> location = Collections.singletonList("hello-world-v0.4.0");
    when(compoundDigitalObjectStore.getChildren("")).thenReturn(location);
    JsonNode v4Metadata =
        new ObjectMapper()
            .readTree("{  \"identifier\" : \"ark:/hello/world\",\n\"version\":\"v0.4.0\"}");
    when(compoundDigitalObjectStore.getMetadata(hellov4Location))
        .thenReturn((ObjectNode) v4Metadata);
    assertEquals(hellov4Location, repository.getObjectLocation(hellov4));
  }
}
