package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umich.lhs.activator.domain.ArkId;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Component
public interface CompoundDigitalObjectStore {

  List<String> getChildren(Path relativeLocation);

  String getAbsoluteLocation(Path relativeLocation);

  ObjectNode getMetadata(Path relativeLocation);

  byte[] getBinary(Path relativeLocation);

  void saveMetadata(Path relativeDestination, JsonNode metadata);

  void saveBinary(Path relativeDestination, byte[] data);

  ObjectNode addCompoundObjectToShelf(MultipartFile zip);

  void getCompoundObjectFromShelf(ArkId arkId, String version, OutputStream outputStream);

  void removeFile(Path relativeLocation) throws IOException;

}
