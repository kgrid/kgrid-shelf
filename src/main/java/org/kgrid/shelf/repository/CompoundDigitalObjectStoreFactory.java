package org.kgrid.shelf.repository;

import org.springframework.stereotype.Component;

@Component
public class CompoundDigitalObjectStoreFactory {

  public static CompoundDigitalObjectStore create(String cdoStoreURI) {
    String shelfClass = cdoStoreURI.substring(0, cdoStoreURI.indexOf(':'));
    if ("filesystem".equals(shelfClass)) {
      return new FilesystemCDOStore(cdoStoreURI);
    } else {
      throw new IllegalArgumentException("Cannot find specified CDO store type " + shelfClass);
    }
  }
}
