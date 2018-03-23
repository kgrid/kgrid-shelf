package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CompoundDigitalObjectStore {

  List<String> getChildren(URI relativeLocation);

  URI getAbsoluteLocation(URI relativeLocation);

  ObjectNode getMetadata(URI relativeLocation);

  byte[] getBinary(URI relativeLocation);

  void saveMetadata(URI relativeDestination, JsonNode metadata);

  void saveBinary(URI relativeDestination, byte[] data);

  ObjectNode addCompoundObjectToShelf(MultipartFile zip);

  void removeFile(URI relativeLocation) throws IOException;

}
