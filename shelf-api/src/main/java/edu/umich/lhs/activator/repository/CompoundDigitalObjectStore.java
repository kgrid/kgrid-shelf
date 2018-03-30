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

  List<String> getChildren(Path relativeLocation);

  String getAbsoluteLocation(Path relativeLocation);

  ObjectNode getMetadata(Path relativeLocation);

  byte[] getBinary(Path relativeLocation);

  void saveMetadata(Path relativeDestination, JsonNode metadata);

  void saveBinary(Path relativeDestination, byte[] data);

  ObjectNode addCompoundObjectToShelf(MultipartFile zip);

  void removeFile(Path relativeLocation) throws IOException;

}
