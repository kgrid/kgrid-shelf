package org.kgrid.shelf;

import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class JsonLDCharacterizationTests {

  Object jsonObject;

  @Before
  public void setupJsonObject() throws IOException {

    //This is hello world from fedora
    jsonObject = JsonUtils.fromString("{\n"
        + "  \"@id\" : \"http://localhost:8080/fcrepo/rest/hello-world\",\n"
        + "  \"@type\" : \"koio:KnowledgeObject\",\n"
        + "  \"contributors\" : \"Kgrid Team\",\n"
        + "  \"hasImplementation\" : [ \"http://localhost:8080/fcrepo/rest/hello-world/v0.0.1\", \"http://localhost:8080/fcrepo/rest/hello-world/v0.0.2\" ],\n"
        + "  \"keywords\" : \"test hello world\",\n"
        + "  \"description\" : \"Test Hello World \",\n"
        + "  \"identifier\" : \"ark:/hello/world\",\n"
        + "  \"title\" : \"Hello  World Title\",\n"
        + "  \"junknotinkoio\" : \"Junk Not in KOIO\",\n"
        + "  \"@context\" : {\n"
        + "    \"hasImplementation\" : {\n"
        + "      \"@id\" : \"http://kgrid.org/koio#hasImplementation\",\n"
        + "      \"@type\" : \"@id\"\n"
        + "    },\n"
        + "    \"description\" : {\n"
        + "      \"@id\" : \"http://purl.org/dc/elements/1.1/description\"\n"
        + "    },\n"
        + "    \"contributors\" : {\n"
        + "      \"@id\" : \"http://kgrid.org/koio#contributors\"\n"
        + "    },\n"
        + "    \"title\" : {\n"
        + "      \"@id\" : \"http://purl.org/dc/elements/1.1/title\"\n"
        + "    },\n"
        + "    \"keywords\" : {\n"
        + "      \"@id\" : \"http://kgrid.org/koio#keywords\"\n"
        + "    },\n"
        + "    \"identifer\" : {\n"
        + "      \"@id\" : \"http://purl.org/dc/elements/1.1/identifer\"\n"
        + "    },\n"
        + "    \"premis\" : \"http://www.loc.gov/premis/rdf/v1#\",\n"
        + "    \"test\" : \"info:fedora/test/\",\n"
        + "    \"rdfs\" : \"http://www.w3.org/2000/01/rdf-schema#\",\n"
        + "    \"xsi\" : \"http://www.w3.org/2001/XMLSchema-instance\",\n"
        + "    \"xmlns\" : \"http://www.w3.org/2000/xmlns/\",\n"
        + "    \"rdf\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\",\n"
        + "    \"fedora\" : \"http://fedora.info/definitions/v4/repository#\",\n"
        + "    \"xml\" : \"http://www.w3.org/XML/1998/namespace\",\n"
        + "    \"koio\" : \"http://kgrid.org/koio#\",\n"
        + "    \"ebucore\" : \"http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#\",\n"
        + "    \"ldp\" : \"http://www.w3.org/ns/ldp#\",\n"
        + "    \"xs\" : \"http://www.w3.org/2001/XMLSchema\",\n"
        + "    \"fedoraconfig\" : \"http://fedora.info/definitions/v4/config#\",\n"
        + "    \"foaf\" : \"http://xmlns.com/foaf/0.1/\",\n"
        + "    \"dc\" : \"http://purl.org/dc/elements/1.1/\"\n"
        + "  }\n"
        + "}");


  }

  @Test
  public void compact() throws IOException {

    DocumentLoader documentLoader = new DocumentLoader();
    Object context = JsonUtils.fromURL(new URL("http://kgrid.org/koio/contexts/knowledgeobject.jsonld"), documentLoader.getHttpClient());

    JsonLdOptions options = new JsonLdOptions();
    options.setBase("http://localhost:8080/fcrepo/rest/");

    Object compact = JsonLdProcessor.compact(jsonObject, context, options);
    System.out.println("**************** COMPACT *******************");
    System.out.println(JsonUtils.toPrettyString(compact));

  }

  @Test
  public void expand() throws IOException {

    DocumentLoader documentLoader = new DocumentLoader();
    Object context = JsonUtils.fromURL(new URL("http://kgrid.org/koio/contexts/knowledgeobject.jsonld"), documentLoader.getHttpClient());

    JsonLdOptions options = new JsonLdOptions();
    options.setBase("http://localhost:8080/fcrepo/rest/");

    Object compact = JsonLdProcessor.expand(jsonObject, options);
    System.out.println("**************** EXPAND *******************");
    System.out.println(JsonUtils.toPrettyString(compact));
  }

}
