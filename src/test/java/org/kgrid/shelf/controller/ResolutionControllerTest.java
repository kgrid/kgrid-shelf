package org.kgrid.shelf.controller;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.service.ResolutionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResolutionControllerTest extends TestCase {

  public static final String NAAN = "NAAN";
  public static final String NAME = "NAME";
  public static final String VERSION_1 = "VERSION1";
  public static final String VERSION_1_ARTIFACT_1 = "src/v1artifact1.js";
  public static final String VERSION_1_ARTIFACT_2 = "src/v1artifact1.js";
  public static final String VERSION_2_ARTIFACT_1 = "src/v2artifact2.xml";
  public static final String VERSION_2_ARTIFACT_2 = "src/v2artifact2.json";
  @Mock ResolutionService resolutionService;
  @InjectMocks ResolutionController resolutionController;
  private final ArkId arkId = new ArkId(NAAN, NAME, VERSION_1);
  private final ArkId arkIdNoVersion = new ArkId(NAAN, NAME);
  private final URI version1Artifact1Uri =
      URI.create(NAAN + "/" + NAME + "/" + VERSION_1 + "/" + VERSION_1_ARTIFACT_1);
  private final URI version1Artifact2Uri =
      URI.create(NAAN + "/" + NAME + "/" + VERSION_1 + "/" + VERSION_1_ARTIFACT_2);
  private final URI version2Artifact1Uri =
      URI.create(NAAN + "/" + NAME + "/" + VERSION_1 + "/" + VERSION_2_ARTIFACT_1);
  private final URI version2Artifact2Uri =
      URI.create(NAAN + "/" + NAME + "/" + VERSION_1 + "/" + VERSION_2_ARTIFACT_2);
  private final List<URI> version1ArtifactUris =
      Arrays.asList(version1Artifact1Uri, version1Artifact2Uri);
  private final List<URI> noVersionArtifactUris =
      Arrays.asList(
          version1Artifact1Uri, version1Artifact2Uri, version2Artifact1Uri, version2Artifact2Uri);

  @Before
  public void setup() {
    when(resolutionService.resolveArtifactsForArk(arkId)).thenReturn(version1ArtifactUris);
    when(resolutionService.resolveArtifactsForArk(arkIdNoVersion))
        .thenReturn(noVersionArtifactUris);
  }

  @Test
  public void testResolveArtifactsForArkCallsResolutionService_withVersion() {
    resolutionController.resolveArtifactsForArk(NAAN, NAME, VERSION_1);
    verify(resolutionService).resolveArtifactsForArk(arkId);
  }

  @Test
  public void testResolveArtifactsForArkCallsResolutionService_withoutVersion() {
    resolutionController.resolveArtifactsForArk(NAAN, NAME, null);
    verify(resolutionService).resolveArtifactsForArk(arkIdNoVersion);
  }

  @Test
  public void testResolveArtifactsForArkReturnsListOfUrisFromService_withVersion() {
    ResponseEntity<List<URI>> listResponseEntity =
        resolutionController.resolveArtifactsForArk(NAAN, NAME, VERSION_1);
    assertEquals(version1ArtifactUris, listResponseEntity.getBody());
  }

  @Test
  public void testResolveArtifactsForArkReturnsListOfUrisFromService_noVersion() {
    ResponseEntity<List<URI>> response =
        resolutionController.resolveArtifactsForArk(NAAN, NAME, null);
    assertEquals(noVersionArtifactUris, response.getBody());
  }
}
