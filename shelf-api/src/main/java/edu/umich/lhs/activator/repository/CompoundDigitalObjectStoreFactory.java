package edu.umich.lhs.activator.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CompoundDigitalObjectStoreFactory {

//  @Value("${shelf.path}")
//  private String localStoragePath;

  private CompoundDigitalObjectStore cdoStore;

  @Autowired
  private CompoundDigitalObjectStoreFactory(FilesystemCDOStore filesystemCDOStore){
    if(filesystemCDOStore != null) {
      this.cdoStore = filesystemCDOStore;

    } else {
//      this.cdoStore = fedoraCDOStore;
    }
  }

  public CompoundDigitalObjectStore create(String arkFilename) {
    return cdoStore;
  }

  public CompoundDigitalObjectStore create() {
    return cdoStore;
  }

}
