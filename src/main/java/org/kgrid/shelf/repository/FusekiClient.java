package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
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
import org.springframework.stereotype.Component;

@Component
public class FusekiClient {

  private URI fusekiServerURI;

  private static final Logger log = LoggerFactory.getLogger(FusekiClient.class);

  private static final String SUBJ = "?x";
  private static final String KNOWLEDGE_OBJECT_TYPE = "<http://kgrid.org/koio#KnowledgeObject>";
  private static final String IMPLEMENTATION_TYPE = "<http://kgrid.org/koio#Implementation>";
  private static final URI KO_CONTEXT = URI.create("http://kgrid.org/koio/contexts/knowledgeobject.jsonld");
  private static final URI IMPL_CONTEXT = URI.create("http://kgrid.org/koio/contexts/implementation.jsonld");
  private static final String CONTEXT = "@context";
  private static final String ID = "@id";

  public FusekiClient(
      @Value("${kgrid.shelf.fuseki.url:http://localhost:8080/fuseki/test/query}") String fusekiURI) {
    this.fusekiServerURI = URI.create(fusekiURI);

  }

  public JsonNode getAllKnowledgeObjects() {
    Query allKOIDsQuery = getGraphUsingContext(KNOWLEDGE_OBJECT_TYPE, KO_CONTEXT).build();

    return getObjectGraph(allKOIDsQuery);
  }

  public JsonNode getAllKnowledgeObjectImpls() {
    Query allImplsQuery = getGraphUsingContext(IMPLEMENTATION_TYPE, IMPL_CONTEXT).build();

    return getObjectGraph(allImplsQuery);
  }

  public JsonNode getImplGraphOfKO(ArkId arkId) {
    try {
      Query koImplsQuery = getGraphUsingContext(IMPLEMENTATION_TYPE, IMPL_CONTEXT)
          .addWhere(SUBJ, "<http://fedora.info/definitions/v4/repository#hasParent>", "?parent")
          .addFilter("strends(str(?parent), \"" + arkId.getDashArk() + "\")").build();
      return getObjectGraph(koImplsQuery);
    } catch (ParseException e) {
      log.warn(e.getMessage());
      throw new ShelfException(e);
    }
  }

  public List<String> getImplListOfKO(ArkId arkId) {
    try {
      Query koImplsQuery = getListUsingContext(IMPLEMENTATION_TYPE, IMPL_CONTEXT)
          .addWhere(SUBJ, "<http://fedora.info/definitions/v4/repository#hasParent>", "?parent")
          .addFilter("strends(str(?parent), \"" + arkId.getDashArk() + "\")").build();
      return getList(koImplsQuery);
    } catch (ParseException e) {
      log.warn(e.getMessage());
      throw new ShelfException(e);
    }
  }

  private JsonNode getObjectGraph(Query query) {
    QueryExecution execution = QueryExecutionFactory
        .sparqlService(fusekiServerURI.toString(), query);

    Model model;
    try {
      model = execution.execConstruct();
    } catch (QueryExceptionHTTP e) {
      throw new ShelfException("Cannot fetch object list from fuseki. " + e);
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
    QueryExecution execution = QueryExecutionFactory
        .sparqlService(fusekiServerURI.toString(), query);

    ResultSet set;
    List<String> list = new ArrayList<>();
    try {
      set = execution.execSelect();
    } catch (QueryExceptionHTTP e) {
      throw new ShelfException("Cannot fetch object list from fuseki. " + e);
    }
    while (set.hasNext()) {
      list.add(set.next().get(SUBJ).toString());
    }
    return list;
  }

  private JsonNode getContextForURI(URI contextURI) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readTree(contextURI.toURL());

    } catch (IOException e) {
      throw new ShelfException(e);
    }
  }

  private ConstructBuilder getGraphUsingContext(String type, URI contextURI) {

    ConstructBuilder constructBuilder = new ConstructBuilder();
    JsonNode ctx = getContextForURI(contextURI);
    JsonNode fields = ctx.get(CONTEXT);
    Iterator<Entry<String, JsonNode>> iter = fields.fields();
    while (iter.hasNext()) {
      Entry<String, JsonNode> entry = iter.next();
      if (entry.getValue().has(ID)) {
        constructBuilder.addConstruct(SUBJ, "<" + entry.getValue().get(ID).asText() + ">",
            "?" + entry.getKey())
        .addOptional(SUBJ, "<" + entry.getValue().get(ID).asText() + ">",
            "?" + entry.getKey());
      }
    }
    // a works instead of <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, not quite sure why
    constructBuilder.addWhere(SUBJ, "a", type);
    return constructBuilder;
  }

  private SelectBuilder getListUsingContext(String type, URI contextURI) {

    SelectBuilder selectBuilder = new SelectBuilder();
    JsonNode ctx = getContextForURI(contextURI);
    JsonNode fields = ctx.get(CONTEXT);
    Iterator<Entry<String, JsonNode>> iter = fields.fields();
    while (iter.hasNext()) {
      Entry<String, JsonNode> entry = iter.next();
      if (entry.getValue().has(ID)) {
            selectBuilder.addOptional(SUBJ, "<" + entry.getValue().get(ID).asText() + ">",
                "?" + entry.getKey());
      }
    }
    // a works instead of <http://www.w3.org/1999/02/22-rdf-syntax-ns#type>, not quite sure why
    selectBuilder.addWhere(SUBJ, "a", type);
    return selectBuilder;
  }

}
