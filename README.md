# KGrid Shelf
[![CircleCI](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master.svg?style=shield)](https://circleci.com/gh/kgrid/kgrid-shelf/tree/master)

Shelf - (access, acquisition) view Knowledge Objects  (and sets of Knowledge Objects ) & their components; deposit & remove published versions of Knowledge Objects; copy versions of Knowledge Objects
s between Libraries & Activators (Knowledge Objects are immutable)
## Getting Started


### Prerequisites

* Maven
* Java JDK 1.8


### Installing

A step by step series of examples that tell you have to get a development env running


## Running the tests

Explain how to run the automated tests for this system

### Usage example

Spring Boot shelf gateway application can be used to access the shelf behavior

```
java -jar shelf-gateway/target/shelf-gateway-0.5.8-SNAPSHOT-boot.jar --shelf.endpoint=myshelf
```

The shelf allows for the following customizatoins

* shelf.endpoint - shelf endpoint base
* shelf.location - file or url location of shelf (file system, fedora)
* cors.url - 
