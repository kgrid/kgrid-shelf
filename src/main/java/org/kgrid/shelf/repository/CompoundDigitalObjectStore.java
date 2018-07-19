package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.web.multipart.MultipartFile;

public interface CompoundDigitalObjectStore {

  List<String> getChildren(String relativeLocation);

  boolean isMetadata(String relativeLocation);

  String getAbsoluteLocation(String relativeLocation);

  ObjectNode getMetadata(String relativeLocation);

  byte[] getBinary(String relativeLocation);

  void saveMetadata(String relativeDestination, JsonNode metadata);

  void saveBinary(String relativeDestination, byte[] data);

  ArkId addCompoundObjectToShelf(ArkId arkId, MultipartFile zip);

  void getCompoundObjectFromShelf(String relativeDestination, boolean isVersion,
      OutputStream outputStream) throws IOException;

  void removeFile(String relativeLocation) throws IOException;

}
