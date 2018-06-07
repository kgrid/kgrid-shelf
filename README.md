# KGrid Shelf
[![CircleCI](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master.svg?style=shield)](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master)

KGrid Shelf - (access, acquisition) view Knowledge Objects  (and sets of Knowledge Objects ) & their components; deposit & remove published versions of Knowledge Objects; copy versions of Knowledge Objects
s between Libraries & Activators (Knowledge Objects are immutable).

KGrid Shelf has two modules, the shelf api and gateway component.  The api is used but other KGrid 
components to provide access to the KOs while the gateway wraps the api in a covenant spring boot for easy RESTFul access to the shelf api.  

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites
For building and running the application you need:

- [Git](https://git-scm.com/downloads)
- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)

### Clone
To get started you can simply clone this repository using git:
```
git clone https://github.com/kgrid/kgrid-shelf.git
cd kgrid-activator
```

### Quick start
This quick start will run the activator and load two example knowledge objects for testing.  
This example can loads a sample KO shelf (_where to look for the KOs_) via the _kgrid.shelf.cdostore.filesystem.location_ property. By default application will start up and PORT 8080. 
```
mvn clean package
java -jar target/kgrid-shelf*.jar --kgrid.shelf.cdostore.filesystem.location=etc/shelf
```

Once Running access the [Activators Health Endpoint](http://localhost:8080/health).  All _statuses_ reported should be **UP**

```$xslt
{
  "status": "UP",
  "shelf": {
    "status": "UP",
    "kgrid.shelf.cdostore.*.location": "file:///Users/developer/kgrid-shelf/etc/shelf/"
  },
  "diskSpace": {
    "status": "UP",
    "total": 499963170816,
    "free": 421147205632,
    "threshold": 10485760
  }
}
   
```

## Running the tests

#### Automated tests 
Unit and Integration tests can be executed via
```
mvn clean test
mvn clean verify
```

#### End to End Testing

Sample shelf in place the following tests can be executed against the running activator


View All Knowlege Objects on the shelf

```
curl http://localhost:8080
```

View a Knowledge Object

```
curl http://localhost:8080/hello/world
```

View a Knowledge Object Version

```
curl http://localhost:8080/hello/world/v0.0.1
```

## Additional Information

The port of the shelf gateway can be altered via the _server.port_ property 
```
java -jar shelf-gateway/target/shelf-gateway-*-boot.jar --kgrid.shelf.cdostore.filesystem.location=etc/shelf --server.port=8090

```