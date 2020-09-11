package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.kgrid.shelf.TestHelper.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResolutionServiceTest {
  public static final String EP_1_NAME = "ep1";
  public static final String EP_1_ARTIFACT = "src/" + EP_1_NAME + "artifact.js";
  public static final String EP_2_NAME = "ep2";
  public static final String EP_2_ARTIFACT_1 = "src/" + EP_2_NAME + "artifact1.js";
  public static final String EP_2_ARTIFACT_2 = "src/" + EP_2_NAME + "artifact2.js";

  @Mock KnowledgeObjectRepository koRepo;
  @InjectMocks ResolutionService resolutionService;
  ObjectNode deploymentSpec = new ObjectMapper().createObjectNode();

  private final ArkId arkId = new ArkId(NAAN, NAME, VERSION);
  private List<URI> artifactUris;

  @Before
  public void setup() {
    artifactUris =
        Arrays.asList(
            URI.create(EP_1_ARTIFACT), URI.create(EP_2_ARTIFACT_1), URI.create(EP_2_ARTIFACT_2));
    ObjectNode endpoints = deploymentSpec.putObject("endpoints");
    endpoints.putObject(EP_1_NAME).putArray("artifact").add(EP_1_ARTIFACT);
    endpoints.putObject(EP_2_NAME).putArray("artifact").add(EP_2_ARTIFACT_1).add(EP_2_ARTIFACT_2);
    when(koRepo.findDeploymentSpecification(arkId)).thenReturn(deploymentSpec);
  }

  @Test
  public void resolveArtifactsForArkCallsKoRepo() {
    resolutionService.resolveArtifactsForArk(arkId);
    verify(koRepo).findDeploymentSpecification(arkId);
  }

  @Test
  public void resolveArtifactsForArkReturnsListOfUrisForArtifactsOfEndpoints() {
    List<URI> uris = resolutionService.resolveArtifactsForArk(arkId);
    assertEquals(artifactUris, uris);
  }

  @Test
  public void resolveArtifactsThrowsErrorFromKoRepo_WhenNoDeploymentSpec() {
    when(koRepo.findDeploymentSpecification(arkId))
        .thenThrow(new ShelfResourceNotFound("not found"));
    assertThrows(
        ShelfResourceNotFound.class,
        () -> {
          resolutionService.resolveArtifactsForArk(arkId);
        });
  }

  @Test
  public void resolveArtifactsThrowsError_DeploymentHasNoEndpointsNode() {
    deploymentSpec.remove("endpoints");
    assertThrows(ShelfException.class, () -> resolutionService.resolveArtifactsForArk(arkId));
  }

  @Test
  public void resolveArtifactsThrowsError_DeploymentHasNoArtifactsInEndpointNode() {
    ObjectNode badDeploymentSpec = new ObjectMapper().createObjectNode().putObject("endpoints");
    when(koRepo.findDeploymentSpecification(arkId)).thenReturn(badDeploymentSpec);

    assertThrows(ShelfException.class, () -> resolutionService.resolveArtifactsForArk(arkId));
  }
}
