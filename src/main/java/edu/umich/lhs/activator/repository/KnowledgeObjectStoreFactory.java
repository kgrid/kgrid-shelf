package edu.umich.lhs.activator.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeObjectStoreFactory {

  @Value("${activator.shelf.path}")
  private String localStoragePath;

  private FilesystemKOStore filesystemKOStore;

  @Autowired
  private KnowledgeObjectStoreFactory(FilesystemKOStore filesystemKOStore){
    this.filesystemKOStore = filesystemKOStore;
  }

  public KnowledgeObjectStore create(String arkFilename) {

    if(filesystemKOStore.getChildren(null).contains(arkFilename)) {
      return filesystemKOStore;
    } else {
      throw new IllegalArgumentException("Cannot locate object with id " + arkFilename);
    }
  }

  public KnowledgeObjectStore create() {
    return filesystemKOStore;
  }

}
