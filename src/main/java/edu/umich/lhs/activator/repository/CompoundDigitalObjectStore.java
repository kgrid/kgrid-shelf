package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface CompoundDigitalObjectStore {

  List<String> getChildren(Path filePath);

  Path getAbsolutePath(Path filePath);

  ObjectNode getMetadata(Path filePath);

  byte[] getBinary(Path filePath);

  void saveMetadata(Path destination, JsonNode metadata);

  void saveBinary(Path destination, byte[] data);

  ObjectNode addCompoundObjectToShelf(MultipartFile zip);

}
