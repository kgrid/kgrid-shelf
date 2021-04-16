# Shelf API
## Importing KOs (Import API)
The Kgrid's mechanism for importing Knowledge Objects at runtime
  
### `POST /kos`
- POST a single Zipped KO File for ingestion by the shelf
    - Body is multipart/form-data with the value being an absolute path to the zipped KO
- Headers
  ```
  Accept: */*
  Content-type: multipart/form-data
  ```
- Request Body
  ```text
  'ko=@"ABSOLUTE_PATH_TO_ZIP/js-simple-v1.0.zip"'
  ```
- Curl Command
  ```bash
  curl --location --request POST 'http://localhost:8080/kos' \
  --header 'Content-Type: multipart/form-data' \
  --form 'ko=@"ABSOLUTE_PATH_TO_ZIP/js-simple-v1.0.zip"'
  ```
- Responses
  200:
  ```json
  {
      "Added": "js/simple/v1.0/"
  }
  ```
- Errors
  - 400:
    ```json
    {
      "Status" : "400 BAD_REQUEST",
      "Request" : "uri=/kos",
      "Error" : "Error importing: MultipartFile resource [ko]",
      "Time" : "Fri Feb 19 15:58:51 EST 2021"
    }
    ```
### `POST /kos/manifest`
- POST a mixed list of absolute paths and/or URLs to any number of KOs for ingestion by the shelf
- Headers
  ```
  Accept: */*
  Content-type: application/json
  ```
- Request Body
  ```json
  {
    "manifest": [
      "file://ABSOLUTE_PATH_TO_KO/js-simple-v1.0.zip",
      "https://github.com/kgrid-objects/example-collection/releases/download/4.1.1/js-bundled-v1.0.zip"
    ]
  }
  ```
- Curl Command
  ```bash
  curl --location --request POST 'http://localhost:8080/kos/manifest' \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "manifest": [
      "file://ABSOLUTE_PATH_TO_KO/js-simple-v1.0.zip",
      "https://github.com/kgrid-objects/example-collection/releases/download/4.1.1/js-bundled-v1.0.zip"
    ]
  }'
  ```
- Responses
  - 200:
    ```json
    [
      "js/simple/v1.0/",
      "js/bundled/v1.0/"
    ]
    ```
- Errors
  - 400 / 500: (Will just return an empty list if no KOs could be loaded, activator will log errors)
    ```json
    []
    ```
### `POST /kos/manifest-list`
- POST a mixed list of absolute paths and/or URLs to any number of manifest.json files for ingestion by the shelf
- Headers
    ```
    Accept: */*
    Content-type: application/json
    ```
- Request Body
    ```json
    [
        "file://ABSOLUTE_PATH_TO_MANIFEST/manifest.json",
        "https://github.com/kgrid-objects/example-collection/releases/download/4.1.1/manifest.json"
    ]
    ```
- Curl Command
    ```bash
    curl --location --request POST 'http://localhost:8080/kos/manifest-list' \
    --header 'Content-Type: application/json' \
    --data-raw '[
    "file://ABSOLUTE_PATH_TO_MANIFEST/manifest.json",
    "https://github.com/kgrid-objects/example-collection/releases/download/4.1.1/manifest.json"
    ]'
    ```
- Responses
  - 200: Will display a list of every successfully imported KO
    ```json
    [
      "js/simple/v1.0/",
      "node/simple/v1.0/",
      "python/simple/v1.0/",
      "resource/simple/v1.0/"
    ]
    ```
- Errors
  - 400 / 500: (Will just return an empty list if no KOs could be loaded, activator will display errors)
    ```json
    []
    ```
## Exporting KOs (Export API)
The Kgrid's mechanism for exporting Knowledge Objects at runtime as zip files

### `GET /kos/{naan}/{name}/{version}`
- Get a particular version of a KO as a zip file
- Headers
    ```
    Accept: application/zip
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/js/simple/v1.0' \
    --header 'Accept: application/zip'
    ```
- Responses
  - 200: Will start a download of the zipped KO
- Errors
  - 404: if the KO is not found.

### `GET /kos/{naan}/{name}?v={version}`
- Get a particular version of a KO as a zip file
  - (Proposed) If no version is supplied, will find default version (first available)
- Headers
    ```
    Accept: application/zip
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/js/simple?v=v1.0' \
    --header 'Accept: application/zip'
    ```
- Responses
  - 200: Will start a download of the zipped KO
- Errors
  - 500: will currently cause a server error if the KO is not found.
  
## Resolving KO Locations (KO Resolution API)
For finding the metadata of KOs using the ARK universal identifier.

### `GET /kos/ark:/{naan}/{name}/{version}`
- Find the metadata for a particular version of a KO
- Headers
    ```
    Accept: application/zip
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/ark:/js/simple/v1.0'
    ```
- Responses
  - 200: Will redirect to the metadata for the found KO
  ```json
  {
      "@id": "js/simple/v1.0",
      "@type": "koio:KnowledgeObject",
      "identifier": "ark:/js/simple/v1.0",
      "version": "v1.0",
      "title": "Hello world",
      "description": "An example of simple Knowledge Object",
      "keywords": [
          "Hello",
          "example"
      ],
      "hasServiceSpecification": "service.yaml",
      "hasDeploymentSpecification": "deployment.yaml",
      "hasPayload": "src/index.js",
      "@context": [
          "http://kgrid.org/koio/contexts/knowledgeobject.jsonld"
      ]
  }
  ```
- Errors
  - 404:
  ```json
  {
      "Status": "404 NOT_FOUND",
      "Request": "uri=/kos/missing/ko/1.0",
      "Error": "Object location not found for ark id ark:/missing/ko/1.0",
      "Time": "Fri Feb 19 16:51:53 EST 2021"
  }
  ```

### `GET /kos/ark:/{naan}/{name}`
- Find the metadata for all versions of a KO
- Headers
    ```
    Accept: application/zip
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/ark:/js/simple'
    ```
- Responses
  - 200: Will return an array of all metadata found for each version of the KO
  ```json
  [
	  {
		      "@id": "js/simple/v1.0",
		      "@type": "koio:KnowledgeObject",
		      "identifier": "ark:/js/simple/v1.0",
		      "version": "v1.0",
		      "title": "Hello world"
      },
      {
		      "@id": "js/simple/v2.0",
		      "@type": "koio:KnowledgeObject",
		      "identifier": "ark:/js/simple/v2.0",
		      "version": "v1.0",
		      "title": "Hello world"
      }
  ]
  ```
- Errors
  - 404:
  ```json
  {
      "Status": "404 NOT_FOUND",
      "Request": "uri=/kos/missing/ko/1.0",
      "Error": "Object location not found for ark id ark:/missing/ko/1.0",
      "Time": "Fri Feb 19 16:51:53 EST 2021"
  }
  ```

## Finding Info About KOs (KO Resource API)
Mainly used for retrieving metadata for particular KOs, but also for editing metadata, and deleting KOs from the shelf.

### `GET /kos`
- Will return an array of metadata files for all KOs on the shelf
- Headers
  ```
  Accept: application/json
  ```
- Curl Command
  ```bash
  curl --location --request GET 'http://localhost:8080/kos/'
  ```
- Responses
  200:
  ```json
  [
	  {
          "@id": "js/simple/v1.0",
          "@type": "koio:KnowledgeObject",
          "title": "the best hello world ever",
          "identifier": "ark:/js/simple/v1.0",
          "version": "v1.0",
          "description": "An example of simple Knowledge Object"
          "contributors": "Kgrid Team",
          "keywords": [
              "Hello",
              "example"
          ],
          "hasServiceSpecification": "service.yaml",
          "hasDeploymentSpecification": "deployment.yaml",
          "hasPayload": "src/welcome.js",
          "@context": [
              "http://kgrid.org/koio/contexts/knowledgeobject.jsonld"
          ]
	  }
  ]
  ```
  
### `GET /kos/{naan}/{name}?v={version}`
- Find the metadata for a KO (of a particular version, or the default version if none supplied)
- Headers
    ```
    Accept: application/json
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/js/simple?v=v1.0'
    ```
- Responses
  - 200: Will return the metadata for the found KO
  ```json
  {
      "@id": "js/simple/v1.0",
      "@type": "koio:KnowledgeObject",
      "identifier": "ark:/js/simple/v1.0",
      "version": "v1.0",
      "title": "Hello world",
      "description": "An example of simple Knowledge Object",
      "keywords": [
          "Hello",
          "example"
      ],
      "hasServiceSpecification": "service.yaml",
      "hasDeploymentSpecification": "deployment.yaml",
      "hasPayload": "src/index.js",
      "@context": [
          "http://kgrid.org/koio/contexts/knowledgeobject.jsonld"
      ]
  }
  ```
- Errors
  - 404:
  ```json
  {
      "Status": "404 NOT_FOUND",
      "Request": "uri=/kos/missing/ko/1.0",
      "Error": "Object location not found for ark id ark:/missing/ko/1.0",
      "Time": "Fri Feb 19 16:51:53 EST 2021"
  }
  ```

### `GET /kos/{naan}/{name}/{version}`
- Find the metadata for a KO 
- Headers
    ```
    Accept: application/json
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/js/simple/v1.0'
    ```
- Responses
  - 200: Will return the metadata for the found KO
  ```json
  {
      "@id": "js/simple/v1.0",
      "@type": "koio:KnowledgeObject",
      "identifier": "ark:/js/simple/v1.0",
      "version": "v1.0",
      "title": "Hello world",
      "description": "An example of simple Knowledge Object",
      "keywords": [
          "Hello",
          "example"
      ],
      "hasServiceSpecification": "service.yaml",
      "hasDeploymentSpecification": "deployment.yaml",
      "hasPayload": "src/index.js",
      "@context": [
          "http://kgrid.org/koio/contexts/knowledgeobject.jsonld"
      ]
  }
  ```
- Errors
  - 404:
  ```json
  {
      "Status": "404 NOT_FOUND",
      "Request": "uri=/kos/missing/ko/1.0",
      "Error": "Object location not found for ark id ark:/missing/ko/1.0",
      "Time": "Fri Feb 19 16:51:53 EST 2021"
  }
  ```

### `PUT /kos/{naan}/{name}/{version}`
- Replace the metadata for a particular version of a KO with the given json node.
- Headers
    ```
    Accept: application/json
    Content-Type: application/json
    ```
- Curl Command
    ```bash
    curl --location --request PUT 'http://localhost:8080/kos/js/simple/v1.0' \
      --header 'Content-Type: application/json' \
      --data-raw '{"@id":"new metadata as json"}'
    ```
- Responses
  - 200: Will return the new metadata for the found KO
  ```json
  {
      "@id":"new metadata as json"
  }
  ```
- Errors
  - 404:
  ```json
  {
      "Status": "404 NOT_FOUND",
      "Request": "uri=/kos/missing/ko/1.0",
      "Error": "Object location not found for ark id ark:/missing/ko/1.0",
      "Time": "Fri Feb 19 16:51:53 EST 2021"
  }
  ```
  - 400 (given bad json):
  ```json
  {
	    "Status": "400 BAD_REQUEST",
	    "Request": "uri=/kos/js/simple/v1.0",
	    "Error": "Cannot parse new metadata",
	    "Time": "Mon Feb 22 15:56:56 EST 2021"
  }
  ```

### `DELETE /kos/{naan}/{name}/{version}`
- Delete a KO from the shelf.
- Headers
    ```
    Accept: application/json
    ```
- Curl Command
    ```bash
    curl --location --request DELETE 'http://localhost:8080/kos/js/simple/v1.0'
    ```
- Responses
  - 204: No Content
- Errors
  - 404:
  ```json
  {
	  "Status": "404 NOT_FOUND",
	  "Request": "uri=/kos/js/simple/v1.0",
	  "Error": "Object location not found for ark id ark:/js/simple/v1.0",
	  "Time": "Tue Feb 23 11:37:32 EST 2021"
  }
  ```
  
## Retrieving Parts Of KOs (KO Binary API)
For retrieving artifacts out of particular KOs

### `GET /kos/{naan}/{name}/{version}/{path to file}`
- Retrieve a binary file that is part of a KO 
  - Note: can be a filename, or a path to a file within the KO.
- Headers
    ```
    Accept: */*
    ```
- Curl Command
    ```bash
    curl --location --request GET 'http://localhost:8080/kos/js/simple/v1.0/src/index.js'
    ```
- Responses
  - 200: Will return the file for the found KO, in this case, index.js.
  ```js
  function welcome(inputs){
  var name = inputs.name;
  return "Welcome to Knowledge Grid, " + name;
  }
  ```
- Errors
  - 404:
  ```json
  {
	  "Status": "404 NOT_FOUND",
	  "Request": "uri=/kos/js/simple/v1.0/notThere.txt",
	  "Error": "Binary resource not found shelf/js/simple/v1.0/notThere.txt",
	  "Time": "Tue Feb 23 11:29:20 EST 2021"
  }
  ```
