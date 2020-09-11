package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResolutionService {
  @Autowired KnowledgeObjectRepository koRepo;

  public List<URI> resolveArtifactsForArk(ArkId arkId) {
    List<URI> artifactUris = new ArrayList<>();
    JsonNode deploymentSpecification = koRepo.findDeploymentSpecification(arkId);
    try {
      deploymentSpecification
          .get("endpoints")
          .forEach(
              (endpointNode) -> {
                JsonNode artifactNode = endpointNode.get("artifact");
                if (artifactNode.isArray()) {
                  artifactNode.forEach(
                      (artifact) -> {
                        artifactUris.add(URI.create(artifact.asText()));
                      });
                } else {
                  artifactUris.add(URI.create(artifactNode.asText()));
                }
              });
    } catch (NullPointerException e) {
      throw new ShelfException(
          "Malformed Deployment spec for: " + arkId.getFullDashArk() + "Cannot resolve artifacts.");
    }

    return artifactUris;
  }
}
