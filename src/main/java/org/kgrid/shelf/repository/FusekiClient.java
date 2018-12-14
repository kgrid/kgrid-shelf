package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.arq.querybuilder.AbstractQueryBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.handlers.HandlerBlock;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class FusekiClient {
  private URI fusekiServerURI;

  private static final Logger log = LoggerFactory.getLogger(FusekiClient.class);

  private static final String RDF_PREFIX = "rdf";
  private static final String RDF_URL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String KOIO_PREFIX = "koio";
  private static final String KOIO_URL = "http://kgrid.org/koio#";
  private static final String FEDORA_PREFIX = "fedora";
  private static final String FEDORA_URL = "http://fedora.info/definitions/v4/repository#";
  private static final String DC_PREFIX = "dc";
  private static final String DC_URL = "http://purl.org/dc/elements/1.1/";
  private static final String LDP_PREFIX = "ldp";
  private static final String LDP_URL = "http://www.w3.org/ns/ldp#";
  private static final String SUBJ = "?x";
  private static final String KNOWLEDGE_OBJECT_TYPE = "koio:KnowledgeObject";
  private static final String IMPLEMENTATION_TYPE = "koio:Implementation";



  public FusekiClient(
      @Value("${kgrid.shelf.fuseki.url:http://localhost:8080/fuseki/test/query}") String fusekiURI) {
    this.fusekiServerURI = URI.create(fusekiURI);

  }

  public JsonNode getAllKnowledgeObjectIDs() {
    Query allKOIDsQuery = getFullGraphOfType(KNOWLEDGE_OBJECT_TYPE).build();

    return getObjectGraph(allKOIDsQuery);
  }

  public JsonNode getAllKnowledgeObjectImpls() {
    Query allImplsQuery = getFullGraphOfType(IMPLEMENTATION_TYPE).build();

    return getObjectGraph(allImplsQuery);
  }

  public JsonNode getImplsOfKO(ArkId arkId) {
    try {
      Query koImplsQuery = getFullGraphOfType(IMPLEMENTATION_TYPE)
          .addFilter("strends(str(?parent), \"" + arkId.getDashArk() + "\")").build();
       return getObjectGraph(koImplsQuery);
    } catch (ParseException e) {
      log.warn(e.getMessage());
      throw new ShelfException(e);
    }
  }

  public List<String> getImplListOfKO(ArkId arkId) {
    try {
      Query koImplsQuery = getListOfType(IMPLEMENTATION_TYPE)
          .addFilter("strends(str(?parent), \"" + arkId.getDashArk() + "\")").build();
      return getList(koImplsQuery);
    } catch (ParseException e) {
      log.warn(e.getMessage());
      throw new ShelfException(e);
    }
  }

  private JsonNode getObjectGraph(Query query) {
    QueryExecution execution = QueryExecutionFactory.sparqlService(fusekiServerURI.toString(), query);

    Model model;
    try {
      model = execution.execConstruct();
    } catch (QueryExceptionHTTP e) {
      throw new ShelfException("Cannot fetch object list from fuseki. " +  e);
    }

    StringWriter modelString = new StringWriter();
    model.write(modelString, RDFLanguages.strLangJSONLD);
    try {
      JsonNode node = new ObjectMapper().readTree(modelString.toString());
      return node;
    } catch (IOException e) {
      log.warn("Cannot read in model from fuseki " + e);
      throw new ShelfException(e);
    }
  }

  private List<String> getList(Query query) {
    QueryExecution execution = QueryExecutionFactory.sparqlService(fusekiServerURI.toString(), query);

    ResultSet set;
    List<String> list = new ArrayList<>();
    try {
      set = execution.execSelect();
    } catch (QueryExceptionHTTP e) {
      throw new ShelfException("Cannot fetch object list from fuseki. " +  e);
    }
    while (set.hasNext()) {
     list.add(set.next().get(SUBJ).toString());
    }
    return list;
  }

  private ConstructBuilder prefixedConstructQuery() {

    return new ConstructBuilder()
        .addPrefix(RDF_PREFIX, RDF_URL)
        .addPrefix(KOIO_PREFIX, KOIO_URL)
        .addPrefix(FEDORA_PREFIX, FEDORA_URL)
        .addPrefix(DC_PREFIX, DC_URL)
        .addPrefix(LDP_PREFIX, LDP_URL);
  }

  private ConstructBuilder getFullGraphOfType(String type) {
    return prefixedConstructQuery()
        .addConstruct(SUBJ, "rdf:type", "?type")
        .addConstruct(SUBJ, "fedora:hasParent", "?parent")
        .addConstruct(SUBJ, "dc:identifier", "?identifier")
        .addConstruct(SUBJ, "dc:title", "?title")
        .addConstruct(SUBJ, "koio:hasDeploymentSpecification", "?depSpec")
        .addConstruct(SUBJ, "koio:hasPayload", "?payload")
        .addConstruct(SUBJ, "koio:hasServiceSpecification", "?serviceSpec")
        .addWhere(SUBJ, "rdf:type", type)
        .addWhere(SUBJ, "fedora:hasParent", "?parent")
        .addOptional(SUBJ, "dc:identifier", "?identifier")
        .addOptional(SUBJ, "dc:title", "?title")
        .addOptional(SUBJ, "koio:hasDeploymentSpecification", "?depSpec")
        .addOptional(SUBJ, "koio:hasPayload", "?payload")
        .addOptional(SUBJ, "koio:hasServiceSpecification", "?serviceSpec");
  }

  private SelectBuilder prefixedSelectQuery() {

    return new SelectBuilder()
        .addPrefix(RDF_PREFIX, RDF_URL)
        .addPrefix(KOIO_PREFIX, KOIO_URL)
        .addPrefix(FEDORA_PREFIX, FEDORA_URL)
        .addPrefix(DC_PREFIX, DC_URL)
        .addPrefix(LDP_PREFIX, LDP_URL);
  }

  private SelectBuilder getListOfType(String type) {

    return prefixedSelectQuery()
        .addVar("*")
        .addWhere(SUBJ, "rdf:type", type)
        .addWhere(SUBJ, "fedora:hasParent", "?parent")
        .addOptional(SUBJ, "dc:identifier", "?identifier")
        .addOptional(SUBJ, "dc:title", "?title")
        .addOptional(SUBJ, "koio:hasDeploymentSpecification", "?depSpec")
        .addOptional(SUBJ, "koio:hasPayload", "?payload")
        .addOptional(SUBJ, "koio:hasServiceSpecification", "?serviceSpec");
  }

}
