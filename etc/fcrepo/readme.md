## Fedora Repository Characterization Tests

Characterization tests are an attempt to understand  existing behavior of a system.  
These are used often in legacy systems that are untested or undocumented.  

In this case Fedora Repository is documented and tested but the needed a set of tests that will
 help the KGrid team understand the behavior of the [Fedora Repository](https://wiki.duraspace.org/display/FF) in 
terms of [JSON LD](https://json-ld.org/) and our [KOIO](http://kgrid.org/koio) ontology.  


### Postman Testing with Fedora Repository
We use lite [Fedora image](https://hub.docker.com/r/kgrid/fcrepo/) based on [Fedora Docker](https://hub.docker.com/r/yinlinchen/fcrepo4-docker/) 
which is part of [Fedora Labs](https://github.com/fcrepo4-labs). The test runs [Postman](https://www.getpostman.com/) 
collection defined in [fcrepo create](tests/fcrepo-create.postman_collection.json) using
 [Newman](https://www.npmjs.com/package/newman)

```
npm install

npm run test-it
```

The _test-it_ starts up the fcreop docker container and executes the postman tests.  Once completed the 
fcrepo container is destroyed.  

#### Starting a Fedora Repository
You can run the fcrepo and keep it running via the ```npm start```  command.  
This starts up the fcrepo at http://localhost:8080/fcrepo/rest/.  

#### Running Postman Collection 
Postman app or via newman ```npm run postman```
