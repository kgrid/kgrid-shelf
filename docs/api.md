# Shelf API
## Import API
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
- POST a list of absolute paths or URLs to any number of KOs for ingestion by the shelf
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
  - 400 / 500: (Will just return an empty list if no KOs could be loaded, activator will display errors)
    ```json
    []
    ```
### `POST /kos/manifest-list`
- POST a list of absolute paths or URLs to any number of manifest.json files for ingestion by the shelf
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
## Export API
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
  - 500: will currently cause a server error if the KO is not found.

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
## KO Resolution API
For finding the metadata of KOs

### `GET /kos/ark:/{naan}/{name}`
- Find the metadata for a KO
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

## KO Resource API

## KO Binary API
