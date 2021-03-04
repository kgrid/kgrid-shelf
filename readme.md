# KGrid Shelf
[![CircleCI](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master.svg?style=shield)](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master)
[![GitHub release](https://img.shields.io/github/release/kgrid/kgrid-shelf.svg)](https://github.com/kgrid/kgrid-shelf/releases/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

KGrid Shelf - (access, acquisition) view Knowledge Objects  (and sets of Knowledge Objects ) & their components; deposit & remove published versions of Knowledge Objects; copy versions of Knowledge Objects
s between Libraries & Activators (Knowledge Objects are immutable).

KGrid Shelf has two modules, the shelf api and gateway component.  The api is used by other KGrid 
components to provide access to the KOs while the gateway wraps the api in a simple spring boot application for easy RESTFul access to the shelf api for testing and development.  

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
For building and running the application you need:

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)

### Clone
To get started you can simply clone this repository using git:
```
git clone https://github.com/kgrid/kgrid-shelf.git
cd kgrid-shelf
```

### Quick start
This quick start will run the activator and load two example knowledge objects for testing.  
This example will load a sample KO shelf (_where to look for the KOs_) via the _kgrid.shelf.cdostore.url_ property. 

Note: this property must be a [valid URI](https://tools.ietf.org/html/rfc3986).

By default, application will start up on PORT 8080. 
```
mvn clean package
java -jar target/kgrid-shelf*.jar --kgrid.shelf.cdostore.url=filesystem:file://shelf
```
Once Running access the [Activators Health Endpoint](http://localhost:8080/actuator/health).  All _statuses_ reported should be **UP**

```$xslt
{
  "status": "UP",
  "shelf": {
    "status": "UP",
    "kgrid.shelf.cdostore.url": "filesystem:file://shelf/"
  },
  "diskSpace": {
    "status": "UP",
    "total": 499963170816,
    "free": 421147205632,
    "threshold": 10485760
  }
}
   
```

## Unit tests

- Unit tests can be executed via maven:

    ```
    mvn clean test
    ```
- There is also 
## Importing Knowledge Objects with manifests
There are two methods by which knowledge objects can be imported to the shelf:
#### 1. Define one or more manifest.json files which point to each Knowledge Object's location. 
Currently, knowledge objects can be loaded from a url, a local file, or a classpath reference.
##### Example manifest.json file:
```json
    {
        "manifest":
          [
            "http://path-to-web-ko",
            "file:/path-to-local-ko",
            "classpath:/path-to-classpath-ko"
          ]
    }
```

When running the shelf, an array of manifest files can be automatically imported by setting the property: `kgrid.shelf.manifest`.

Example: 
  ```bash
    kgrid.shelf.manifest=file:/path/to/manifest,http://path-to-manifest,classpath:/path-to-manifest
  ```

#### 2. Pass a single manifest to the create manifest endpoint

The Shelf-API has an endpoint for importing a single manifest.
To use it, simple send a `POST` request to `/kos/manifest/` endpoint of your shelf with the contents of the manifest.json 
file as the request body. See the [API documentation](/docs/api.md) for more information.
