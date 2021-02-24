package org.kgrid.shelf;

import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.CompoundDigitalObjectStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
public class ShelfGateway {

  @Autowired CompoundDigitalObjectStoreFactory factory;

  Logger log = LoggerFactory.getLogger(ShelfGateway.class);

  public static void main(String[] args) {
    SpringApplication.run(ShelfGateway.class, args);
  }

  @Configuration
  @Profile(
      "Gateway") // `--spring.profiles.active=Gateway` must be set in Run Config or command line
  class Config {
    @Bean
    public CompoundDigitalObjectStore getCDOStore(
        @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String cdoStoreURI) {
      final CompoundDigitalObjectStore cdoStore = factory.create(cdoStoreURI);
      log.info("kgrid.shelf.cdostore.url: {}", cdoStore.getAbsoluteLocation(null));
      return cdoStore;
    }
  }
}
