package edu.umich.lhs.activator.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CompoundDigitalObjectStoreFactory {

  @Value("${activator.shelf.path}")
  private String localStoragePath;

  private FilesystemCDOStore filesystemCDOStore;

  @Autowired
  private CompoundDigitalObjectStoreFactory(FilesystemCDOStore filesystemCDOStore){
    this.filesystemCDOStore = filesystemCDOStore;
  }

  public CompoundDigitalObjectStore create(String arkFilename) {

    if(filesystemCDOStore.getChildren(null).contains(arkFilename)) {
      return filesystemCDOStore;
    } else {
      throw new IllegalArgumentException("Cannot locate object with id " + arkFilename);
    }
  }

  public CompoundDigitalObjectStore create() {
    return filesystemCDOStore;
  }

}
