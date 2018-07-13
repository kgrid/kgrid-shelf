package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

public interface CompoundDigitalObjectStore {

  List<Path> getChildren(Path relativeLocation);

  String getAbsoluteLocation(Path relativeLocation);

  ObjectNode getMetadata(Path relativeLocation);

  byte[] getBinary(Path relativeLocation);

  void saveMetadata(Path relativeDestination, JsonNode metadata);

  void saveBinary(Path relativeDestination, byte[] data);

  ArkId addCompoundObjectToShelf(MultipartFile zip);

  void getCompoundObjectFromShelf(Path relativeDestination, boolean isVersion,
      OutputStream outputStream) throws IOException;

  void removeFile(Path relativeLocation) throws IOException;

}
