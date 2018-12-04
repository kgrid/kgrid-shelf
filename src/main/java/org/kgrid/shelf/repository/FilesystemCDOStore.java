package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

@Qualifier("filesystem")
public class FilesystemCDOStore implements CompoundDigitalObjectStore {

  private URI localStorageURI;

  private final Logger log = LoggerFactory.getLogger(FilesystemCDOStore.class);

  public FilesystemCDOStore(

      @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String connectionURI) {
    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    if (uri.getHost() == null) {
      this.localStorageURI = uri;
    } else {
      this.localStorageURI = Paths.get(uri.getHost(), uri.getPath()).toUri();
    }
  }

  @Override
  public List<String> getChildren(String... filePathParts) {
    Path path = Paths.get(Paths.get(localStorageURI).toString(), filePathParts);
    List<String> children = new ArrayList<>();
    try {
      children = Files.walk(path, 1, FOLLOW_LINKS)
          .filter(Files::isDirectory)
          .map(Object::toString)
          .collect(Collectors.toList());
      children.remove(0); // Remove the parent directory
    } catch (IOException ioEx) {
      log.error("Cannot read children at location " + path + " " + ioEx);
    }
    return children;
  }

  @Override
  public String getAbsoluteLocation(String... relativePathParts) {
    Path shelf = Paths.get(localStorageURI);
    if (!shelf.toFile().exists()) {
      throw new ShelfException(
          "Filesystem shelf location '" + shelf.toAbsolutePath() + "' is not a valid directory."
              + " Make sure the property kgrid.shelf.cdostore.url is set correctly.");
    }
    if (relativePathParts == null || relativePathParts.length == 1) {
      return shelf.toString();
    }
    return Paths.get(localStorageURI.getPath(), relativePathParts).toString();
  }

  @Override
  public boolean isMetadata(String... relativePathParts) {
    return relativePathParts[relativePathParts.length - 1]
        .endsWith(KnowledgeObject.METADATA_FILENAME);
  }

  @Override
  public ObjectNode getMetadata(String... relativePathParts) {
    Path metadataPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    File metadataFile = metadataPath.toFile();
    if (metadataFile.isDirectory()) {
      metadataFile = metadataPath.resolve(KnowledgeObject.METADATA_FILENAME)
          .toFile();
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    JsonNode koMetadata;
    try {
      koMetadata = mapper.readTree(metadataFile);
      if (koMetadata.isArray()) {
        // Parent object in json-ld is array, get first element.
        koMetadata = koMetadata.get(0);
      }

      return ((ObjectNode) koMetadata);
    } catch (Exception ioEx) {
      log.error("Cannot read file at " + metadataPath + " " + ioEx);
      throw new ShelfException(
          "Cannot read metadata file at path " + metadataPath, ioEx);
    }
  }

  @Override
  public byte[] getBinary(String... relativePathParts) {
    Path binaryPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    byte[] bytes = null;
    try {
      bytes = Files.readAllBytes(binaryPath);
    } catch (IOException ioEx) {
      log.error("Cannot read file at " + binaryPath + " " + ioEx);
      throw new ShelfException("Could not find " + binaryPath,ioEx);
    }
    return bytes;
  }

  @Override
  public void saveMetadata(JsonNode metadata, String... relativePathParts) {
    Path metadataPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    File metadataFile = metadataPath.toFile();
    if(metadataFile.isDirectory()){
      metadataFile = metadataPath.resolve(KnowledgeObject.METADATA_FILENAME).toFile();
    }
    try {
      ObjectWriter writer = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
      writer.writeValue(metadataFile, metadata);
    } catch (IOException ioEx) {
      log.error("Could not write to file at " + metadataPath + " " + ioEx);
    }
  }

  @Override
  public void saveBinary(byte[] output, String... relativePathParts) {
    Path dataPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    File dataFile = dataPath.toFile();
    try (FileOutputStream fos = new FileOutputStream(dataFile)) {
      fos.write(output);
    } catch (IOException ioEx) {
      log.error("Could not write to file at " + dataPath + " " + ioEx);
    }
  }

  // rm -rf repository/arkId ** dangerous! **
  @Override
  public void removeFile(String... relativePathParts) throws IOException {
    Path ko = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);

    Files.walk(ko)
        .sorted(Comparator
            .reverseOrder()) // Need to reverse the order to delete files before the directory they're in
        .forEach(file -> {
          try {
            Files.delete(file);
          } catch (IOException e) {
            log.error("Could not delete file " + file + " " + e);
          }
        });
  }

  private void createKOFolderStructure(Path resourceLocation, Path serviceLocation) {
    Path shelf = Paths.get(localStorageURI);

    try {
      Path resourceDir = shelf.resolve(resourceLocation);
      Files.createDirectories(resourceDir);
      Path serviceDir = shelf.resolve(serviceLocation);
      Files.createDirectory(serviceDir);
    } catch (IOException ioEx) {
      log.error("Unable to create directories for ko " + ioEx);
    }
  }

  @Override
  public void createContainer(String... relativePathParts) {
    Path containerPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    if (!containerPath.toFile().exists()) {
      containerPath.toFile().mkdirs();
    }

  }

  @Override
  public void delete(String cdoIdentifier) throws ShelfException {
    Path path = Paths.get(localStorageURI.toString(), cdoIdentifier);
    try {
      FileUtils.deleteDirectory(new File(path.toString()));
    } catch (IOException e) {
      throw new ShelfException("Could not delete cdo " + cdoIdentifier, e);
    }

  }
}
