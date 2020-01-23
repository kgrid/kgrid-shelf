package org.kgrid.shelf;

import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.CompoundDigitalObjectStoreFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
public class ShelfGateway {

  @Autowired
  CompoundDigitalObjectStoreFactory factory;

  public static void main(String[] args) {
    SpringApplication.run(ShelfGateway.class, args);

  }

  public CompoundDigitalObjectStore getCDOStore( @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String cdoStoreURI) {
    return factory.create(cdoStoreURI);
  }

}
