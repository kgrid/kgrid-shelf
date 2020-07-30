package org.kgrid.shelf.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.KnowledgeObjectFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

@Qualifier("filesystem")
public class FilesystemCDOStore implements CompoundDigitalObjectStore {

  private URI localStorageURI;

  private final Logger log = LoggerFactory.getLogger(FilesystemCDOStore.class);

  public FilesystemCDOStore(
      @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String connectionURI) {
    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    if (uri.getHost() == null) {
      this.localStorageURI = uri; // Relative path
    } else {
      final Path path = Paths.get(uri.getHost(), uri.getPath());
      this.localStorageURI = path.toUri();
    }
    try {
      final Path location = Paths.get(localStorageURI);
      Files.createDirectories(location);
    } catch (IOException e) {
      log.error("Unable to find or create shelf at %s", localStorageURI);
    }
  }

  @Override
  public List<String> getChildren(String... filePathParts) {

    return getChildren(1, filePathParts);
  }

  public List<String> getChildren(int maxDepth, String... filePathParts) {
    Path path = Paths.get(Paths.get(localStorageURI).toString(), filePathParts);
    List<String> children = new ArrayList<>();
    try {
      children =
          Files.walk(path, maxDepth, FOLLOW_LINKS)
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
    if (relativePathParts == null || relativePathParts.length == 0) {
      return shelf.toString();
    }
    return Paths.get(shelf.toString(), relativePathParts).toString();
  }

  @Override
  public ObjectNode getMetadata(String... relativePathParts) {
    Path metadataPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    File metadataFile = metadataPath.toFile();
    if (metadataFile.isDirectory()
        || !metadataFile.getPath().endsWith(KnowledgeObjectFields.METADATA_FILENAME.asStr())) {
      metadataFile = metadataPath.resolve(KnowledgeObjectFields.METADATA_FILENAME.asStr()).toFile();
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
      throw new ShelfResourceNotFound("Metadata resource not found " + metadataPath, ioEx);
    }
  }

  @Override
  public byte[] getBinary(String... relativePathParts) {
    Path binaryPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    byte[] bytes = null;
    try {
      bytes = Files.readAllBytes(binaryPath);
    } catch (IOException ioEx) {
      throw new ShelfResourceNotFound("Binary resource not found " + binaryPath, ioEx);
    }
    return bytes;
  }

  @Override
  public void saveMetadata(JsonNode metadata, String... relativePathParts) {
    Path metadataPath = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    File metadataFile = metadataPath.toFile();
    if (metadataFile.isDirectory()) {
      metadataFile = metadataPath.resolve(KnowledgeObjectFields.METADATA_FILENAME.asStr()).toFile();
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
    try (FileOutputStream fos = FileUtils.openOutputStream(dataFile)) {
      fos.write(output);
    } catch (IOException ioEx) {
      log.error("Could not write to file at {}", dataPath);
      throw new ShelfException("Could not write to file at " + dataPath);
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
  public void delete(String... relativePathParts) throws ShelfException {
    Path path = Paths.get(Paths.get(localStorageURI).toString(), relativePathParts);
    try {
      FileUtils.deleteDirectory(new File(path.toString()));
    } catch (IOException e) {
      throw new ShelfException("Could not delete cdo " + relativePathParts, e);
    }
  }

  // Simple transaction support:
  @Override
  public String createTransaction() {
    String trxID = "trx-" + UUID.randomUUID().toString();
    createContainer(trxID);
    return trxID;
  }

  @Override
  public void commitTransaction(String transactionID) {
    File tempFolder = new File(localStorageURI.getPath(), transactionID);
    try {
      FileUtils.copyDirectory(tempFolder, new File(localStorageURI));
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      log.warn("Cannot copy files from temp directory to shelf " + e);
    }
  }

  @Override
  public void rollbackTransaction(String transactionID) {
    File tempFolder = new File(localStorageURI.getPath(), transactionID);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      log.warn("Cannot rollback failed transaction " + e);
    }
  }
}
