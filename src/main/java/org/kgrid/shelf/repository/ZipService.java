package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.kgrid.shelf.domain.KnowledgeObject;

public abstract class ZipService {

  /**
   * Get a list of implementation paths, service, deployment and payload
   *
   * @param implementationNode ko implementation jsonnode
   * @return list of paths to implementation binaries
   */
  protected List<String> listBinaryNodes(JsonNode implementationNode) {

    List<String> binaryNodes = new ArrayList<>();
    if (implementationNode.has(KnowledgeObject.DEPLOYMENT_SPEC_TERM)) {
      binaryNodes.add(implementationNode.findValue(KnowledgeObject.DEPLOYMENT_SPEC_TERM).asText());
    }
    if (implementationNode.has(KnowledgeObject.PAYLOAD_TERM)) {
      binaryNodes.add(implementationNode.findValue(KnowledgeObject.PAYLOAD_TERM).asText());
    }
    if (implementationNode.has(KnowledgeObject.SERVICE_SPEC_TERM)) {
      binaryNodes.add(implementationNode.findValue(KnowledgeObject.SERVICE_SPEC_TERM).asText());
    }

    return binaryNodes;
  }
}
